package swiss.sib.rdf.sparql.examples;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;

@Command(name = "convert", description = "Converts example files into RDF")
public class Converter implements Callable<Integer>{
	@Spec 
	CommandSpec spec;
	
	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

	private final Set<RDFFormat> outputFormats = Set.of(RDFFormat.TURTLE, RDFFormat.RDFXML, RDFFormat.NTRIPLES,
			RDFFormat.JSONLD, RDFFormat.NDJSONLD);

	@Option(names = { "-f",
			"--format" }, paramLabel = "output RDF format", description = "the format that the example queries with their prefixes should be concattenated into", defaultValue = "ttl")
	private String outputFormat;

	@Option(names = { "-r",
			"--rq" }, paramLabel = "output example files as rq files next to the exiting turtle", description = "output example files as rq files next to the exiting turtle", defaultValue = "false")
	private boolean outputRq;
	
    @Option(names = { "-m",
			"--markdown" }, paramLabel = "output example files as markdown files next to the exiting turtle", description = "output example files as markdown files next to the exiting turtle", defaultValue = "false")
	private boolean outputMd;


	@Option(names = { "-i",
			"--input-directory" }, paramLabel = "directory containing example files to convert", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path inputDirectory;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@Option(names = { "-p", "--project" }, paramLabel = "projects to convert", defaultValue = "all")
	private String projects;

	public Integer call() {
		CommandLine commandLine = spec.commandLine();
		System.err.println("inputDirectory: "+inputDirectory);
		
		System.setProperty(Tester.class.getName(), inputDirectory.toString());
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
			return 0;
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
			return 0;
		} else {
            if (outputMd) {
				convertPerSingle("md", SparqlInRdfToMd::asMD, SparqlInRdfToMd::asIndexMD);
            } else if (outputRq) {
				convertPerSingle("rq", SparqlInRdfToRq::asRq, null);
			} else {
				convertToRdf();
			}
		}
		return 0;
	}

	private static final Pattern COMMA = Pattern.compile(",", Pattern.LITERAL);

	private void convertToRdf() {
		Model model = parseExampleFilesIntoModel(projects, inputDirectory);
		print(model);
	}

	static Model parseExampleFilesIntoModel(String projects, Path inputDirectory) {
		Model model = new LinkedHashModel();
		if ("all".equals(projects)) {
			try (Stream<Path> list = Files.list(inputDirectory)) {
				parseAll(list, model, inputDirectory);
			} catch (IOException e) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(e);
			}
		} else {
			try (Stream<Path> list = COMMA.splitAsStream(projects).map(inputDirectory::resolve)) {
				parseAll(list, model, inputDirectory);
			}
		}
		return model;
	}

	private void convertPerSingle(String extension, Function<Model, List<String>> converter, Function<Model, List<String>> converterPerProject){
		if ("all".equals(projects)) {
			try (Stream<Path> list = Files.list(inputDirectory)) {
				convertProjectsPerSingle(list, extension, converter, converterPerProject);
			} catch (IOException e) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(e);
			}
		} else {
			try (Stream<Path> list = COMMA.splitAsStream(projects).map(inputDirectory::resolve)) {
				convertProjectsPerSingle(list, extension, converter, converterPerProject);
			}
		}
	}

	private void convertProjectsPerSingle(Stream<Path> list, String extension, Function<Model, List<String>> converter, Function<Model, List<String>> convertPerProject) {
		Optional<Path> findCommonPrefixes = FindFiles.prefixFile(inputDirectory).findFirst();
		Model commonPrefixes = prefixModel(findCommonPrefixes);
		list.filter(Files::isDirectory).forEach(pro -> {
			try {
				Optional<Path> findProjectPrefixes = FindFiles.prefixFile(pro).findFirst();
				Model projectPrefixes = prefixModel(findProjectPrefixes);
				Model allForProject = new LinkedHashModel();
				allForProject.addAll(commonPrefixes);
				allForProject.addAll(projectPrefixes);
				FindFiles.sparqlExamples(pro).forEach((p) -> parseAndRenderSingleExample(extension, converter,
						commonPrefixes, projectPrefixes, allForProject, p));
				if (convertPerProject != null) {
					renderAllExamplesInAProject(extension, convertPerProject, pro, allForProject);
				}
			} catch (IOException e) {
				Failure.CANT_PARSE_EXAMPLE.exit(e);
				throw new RuntimeException(e);
			}
		});
	}

	private void renderAllExamplesInAProject(String extension, Function<Model, List<String>> convertPerProject,
			Path pro, Model allForProject) {
		String prqfn = "index."+extension;
		Path prq = pro.resolve(prqfn);
		try {
			List<String> rq = convertPerProject.apply(allForProject);
			Files.write(prq, rq, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			Failure.CANT_WRITE_EXAMPLE_RQ.exit(e);
			throw new RuntimeException(e);
		}
	}

	private void parseAndRenderSingleExample(String extension, Function<Model, List<String>> converter,
			Model commonPrefixes, Model projectPrefixes, Model allForProject, Path p) {
		Model ex = parseSingle(p);
		ex.addAll(commonPrefixes);
		ex.addAll(projectPrefixes);
		allForProject.addAll(ex);
		Iterator<Statement> iterator = ex.getStatements(null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).iterator();
		if (iterator.hasNext()) {
			Resource subject = iterator.next().getSubject();
			allForProject.add(VF.createStatement(subject, SIB.FILE_NAME, VF.createLiteral(p.getFileName().toString())));
			allForProject.add(VF.createStatement(subject, SIB.FILE_PATH, VF.createLiteral(p.toFile().toString())));
			allForProject.add(VF.createStatement(subject, SIB.PROJECT, VF.createLiteral(p.getParent().getFileName().toString())));
		}
	
		String pfn = p.getFileName().toString();
		String prqfn = pfn.substring(0, pfn.indexOf('.')) + "."+extension;
		Path prq = p.getParent().resolve(prqfn);
		try {
			List<String> rq = converter.apply(ex);
			Files.write(prq, rq, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			Failure.CANT_WRITE_EXAMPLE_RQ.exit(e);
			throw new RuntimeException(e);
		}
	}

	private Model prefixModel(Optional<Path> findFirst) {
		Model prefixes = new LinkedHashModel();
		if (findFirst.isPresent() && Files.exists(findFirst.get())) {
			prefixes = parseSingle(findFirst.get());
		}
		return prefixes;
	}

	static void parseAll(Stream<Path> paths, Model model, Path inputDirectory) {
		Stream.concat(FindFiles.prefixFile(inputDirectory), paths.flatMap(arg0 -> {
			try {
				return Stream.concat(FindFiles.prefixFile(arg0), FindFiles.sparqlExamples(arg0));
			} catch (IOException e) {
				Failure.CANT_READ_EXAMPLE.exit(e);
				throw new RuntimeException(e);
			}
		})).filter(Files::exists).forEach(p -> {
			parseTurtleFileIntoModel(model, p);
		});
	}

	private Model parseSingle(Path path) {
		Model model = new LinkedHashModel();
		parseTurtleFileIntoModel(model, path);
		
		return model;
	}

	static void parseTurtleFileIntoModel(Model model, Path p) {
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		Model temp = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(temp));
		try (InputStream is = Files.newInputStream(p)) {
			rdfParser.parse(is);
		} catch (RDFParseException | RDFHandlerException e) {
			System.err.println("Failed to parse " + p);
			Failure.CANT_PARSE_EXAMPLE.exit(e);
		} catch (IOException e) {
			Failure.CANT_READ_EXAMPLE.exit(e);
		}
		IRI pAsIri = VF.createIRI(p.toUri().toString());
		temp.getStatements(null, RDF.TYPE, null).forEach(s -> {
			temp.add(VF.createStatement(s.getSubject(), SIB.FILE_NAME, pAsIri));
			temp.add(VF.createStatement(s.getSubject(), SIB.FILE_PATH, VF.createLiteral(p.toFile().toString())));
		});
		model.addAll(temp);
	}

	private void print(Model model) {
		Rio.write(model, System.out,
				RDFFormat.matchFileName("a." + outputFormat, outputFormats).orElse(RDFFormat.TURTLE));
	}

}
