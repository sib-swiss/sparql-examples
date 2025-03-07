package swiss.sib.rdf.sparql.examples;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

@CommandLine.Command(name = "import-rq", description = "Attempts to import *.rq files")
public class ImportFromRq implements Callable<Integer> {
	private static final Logger log = LoggerFactory.getLogger(ImportFromRq.class);
	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Spec
	CommandSpec spec;

	@Option(names = { "-i",
			"--input-directory" }, paramLabel = "directory containing example files to import", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path inputDirectory;

	@Option(names = { "-b",
			"--base" }, required = true, description = "The base URI for the examples. e.g. https://purl.example.org/.well-known/sparql-examples#")
	private String base;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	public Integer call() {
		CommandLine commandLine = spec.commandLine();
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
		} else {
			try {
				findFilesToImport();
			} catch (IOException e) {
				log.error("Can't read input directory", e);
				return Failure.CANT_READ_INPUT_DIRECTORY.exitCode();
			} catch (NeedToStopException e) {
				log.error("{}", e.getMessage());
				return e.getFailure().exitCode();
			}
		}
		return 0;
	}

	private void findFilesToImport() throws IOException {
		Map<String, String> prefixes = new TreeMap<>();
		List<Path> iter = Files.walk(inputDirectory).filter(p -> p.toUri().getPath().endsWith(".rq"))
				.collect(Collectors.toList());
		iter.sort((a, b) -> a.toUri().compareTo(b.toUri()));
		int index = 1;
		for (Path p : iter) {
			importFile(p, prefixes, index++);
		}
		log.info("Found prefixes: {}", prefixes.size());
		Model prefixModel = new TreeModel();
		prefixModel.setNamespace(SHACL.NS);
		prefixModel.setNamespace(XSD.NS);
		prefixModel.setNamespace(OWL.NS);
		prefixModel.setNamespace(RDF.NS);
		prefixes.entrySet().stream().forEach(e -> {
			BNode pb = VF.createBNode("prefix_" + e.getKey());
			prefixModel.add(VF.createStatement(PREFIXES_BNODE, SHACL.DECLARE, pb));
			prefixModel.add(VF.createStatement(pb, SHACL.PREFIX_PROP, VF.createLiteral(e.getKey())));
			prefixModel.add(VF.createStatement(pb, SHACL.NAMESPACE_PROP, VF.createLiteral(e.getValue(), XSD.ANYURI)));

		});
		prefixModel.add(VF.createStatement(PREFIXES_BNODE, RDF.TYPE, OWL.ONTOLOGY));
		prefixModel.add(VF.createStatement(PREFIXES_BNODE, OWL.IMPORTS, VF.createIRI(SHACL.NAMESPACE)));
		writeModel(inputDirectory.resolve("prefixes.ttl"), prefixModel);
	}

	private static final Pattern PREFIX_PATTERN = Pattern.compile("[pP][rR][eE][fF][iI][xX]\\s+(\\w+):\\s+<([^>]+)>");
	private static final Pattern LEADING_HASH_PLUS_PATTERN = Pattern.compile("^(#+)\\+\\s*([^:]+):(.+)?");
	private static final Pattern LEADING_HASH_PATTERN = Pattern.compile("^(#+)");
	private static final Pattern LEADING_HASH_PLUS_DASH_PATTERN = Pattern.compile("^#\\+\\s*-\\s*(.+)");
	private static final Pattern SELECT = Pattern.compile("SELECT", Pattern.CASE_INSENSITIVE);
	private static final Pattern ASK = Pattern.compile("ASK", Pattern.CASE_INSENSITIVE);
	private static final Pattern CONSTRUCT = Pattern.compile("CONSTRUCT", Pattern.CASE_INSENSITIVE);
	private static final Pattern DESCRIBE = Pattern.compile("DESCRIBE", Pattern.CASE_INSENSITIVE);
	private static final BNode PREFIXES_BNODE = VF.createBNode("sparql_examples_prefixes");

	private void importFile(Path path1, Map<String, String> prefixes2, int count) {
		log.info("Importing file: {}", path1);
		try {
			Model model = new TreeModel();
			IRI predicate = null;
			Map<String, String> prefixes = new TreeMap<>();
			List<String> lines = Files.readAllLines(path1);
			List<String> topComments = new ArrayList<>();
			List<String> query = new ArrayList<>();
			boolean queryStarted = false;
			boolean tags = false;
			String fn = path1.getFileName().toString();
			model.setNamespace("ex", this.base);
			IRI ex = VF.createIRI(this.base + count + "_" + fn.substring(0, fn.lastIndexOf(".")));
			model.add(VF.createStatement(ex, RDF.TYPE, SHACL.SPARQL_EXECUTABLE));
			model.add(VF.createStatement(ex, SHACL.PREFIXES, PREFIXES_BNODE));
			for (String line : lines) {
				String l = line.trim();
				queryStarted = extractPrefixes(prefixes, l);

				if (!queryStarted && predicate == null) {
					Matcher hmp = LEADING_HASH_PLUS_PATTERN.matcher(l);
					Matcher hmd = LEADING_HASH_PLUS_DASH_PATTERN.matcher(l);
					Matcher hm = LEADING_HASH_PATTERN.matcher(l);
					if (hmp.find()) {
						tags = garlicMain(model, tags, ex, hmp);
					} else if (hmd.find()) {
						if (tags) {
							model.add(VF.createStatement(ex, SchemaDotOrg.KEYWORDS, VF.createLiteral(hmd.group(1))));
						}
					} else if (hm.find()) {
						topComments.add(l.substring(hm.group(1).length()));
					} else {
						predicate = tryToStartQuery(predicate, query, l);
						if (predicate != null) {
							queryStarted = true;
						}
					}
				} else {
					// We want to keep indentations from this point on.
					query.add(line);
				}
			}

			if (!topComments.isEmpty()) {
				model.add(VF.createStatement(ex, RDFS.COMMENT, VF.createLiteral(String.join("\n", topComments))));
			}
			if (!query.isEmpty()) {
				String q = Stream
						.concat(prefixes.entrySet().stream().map(e -> "PREFIX " + e.getKey() + ": " + e.getValue()),
								query.stream())
						.collect(Collectors.joining("\n"));
				model.add(VF.createStatement(ex, predicate, VF.createLiteral(q)));
			}

			Path out = path1.getParent().resolve(fn.replace(".rq", ".ttl"));
			writeModel(out, model);
			prefixes2.putAll(prefixes);
		} catch (IOException e) {
			throw Failure.CANT_PARSE_EXAMPLE.tothrow(e);
		}
	}

	public boolean extractPrefixes(Map<String, String> prefixes2, String l) {
		Matcher pm = PREFIX_PATTERN.matcher(l.trim());
		boolean queryStarted = false;
		while (pm.find()) {
			queryStarted = true;
			prefixes2.put(pm.group(1), pm.group(2));
		}
		return queryStarted;
	}

	private static final Map<IRI, Pattern> QTYPES = Map.of(SHACL.SELECT, SELECT, SHACL.ASK, ASK, SHACL.CONSTRUCT,
			CONSTRUCT, SIB.DESCRIBE, DESCRIBE);

	public IRI tryToStartQuery(IRI predicate, List<String> query, String l) {
		for (Map.Entry<IRI, Pattern> e : QTYPES.entrySet()) {
			Matcher m = e.getValue().matcher(l);
			if (m.find()) {
				predicate = e.getKey();
				query.add(l.substring(m.start()));
				break;
			}
		}
		return predicate;
	}

	public boolean garlicMain(Model model, boolean tags, IRI ex, Matcher hmp) {
		String s = hmp.group(2);
		switch (s) {
		case "summary":
			tags = false;
			model.add(VF.createStatement(ex, RDFS.LABEL, VF.createLiteral(hmp.group(3))));
			break;
		case "description":
			tags = false;
			model.add(VF.createStatement(ex, RDFS.COMMENT, VF.createLiteral(hmp.group(3))));
			break;
		case "endpoint":
			tags = false;
			model.add(VF.createStatement(ex, SchemaDotOrg.TARGET, VF.createLiteral(hmp.group(3))));
			break;
		case "tags":
			tags = true;
			break;
		}

		return tags;
	}

	public String afterColon(String s) {
		return s.substring(s.indexOf(":") + 1).trim();
	}

	public static void writeModel(Path file, Model model) {
		try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
			model.getNamespaces().add(SHACL.NS);
			model.getNamespaces().add(RDF.NS);
			model.getNamespaces().add(RDFS.NS);
			model.getNamespaces().add(SchemaDotOrg.NS);
			model.getNamespaces().add(DCTERMS.NS);
			model.getNamespaces().add(SIB.NS);
			Rio.write(model, out, RDFFormat.TURTLE);

		} catch (RDFHandlerException | IOException e) {
			Failure.CANT_WRITE_FIXED_EXAMPLE.exit(e);
		}
	}
}
