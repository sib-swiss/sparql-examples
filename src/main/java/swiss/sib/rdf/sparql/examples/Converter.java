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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;

public class Converter {

	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

	public static enum Failure {
		CANT_READ_INPUT_DIRECTORY(1), CANT_PARSE_EXAMPLE(2), CANT_READ_EXAMPLE(3), CANT_WRITE_EXAMPLE_RQ(4);

		private final int exitCode;

		Failure(int i) {
			this.exitCode = i;
		}

		void exit(Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(exitCode);
		}

		void exit(String queryS, MalformedQueryException e) {
			System.err.println(queryS+"." + e.getMessage());
			e.printStackTrace();
			System.exit(exitCode);
			
		}

	}

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

	public static void main(String[] args) {
		Converter converter = new Converter();
		CommandLine commandLine = new CommandLine(converter);
		commandLine.parseArgs(args);
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
			return;
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
			return;
		} else {
            if (converter.outputMd) {
				converter.convertPerSingle("md", SparqlInRdfToMd::asMD, SparqlInRdfToMd::asIndexMD);
            } else if (converter.outputRq) {
				converter.convertPerSingle("rq", SparqlInRdfToRq::asRq, null);
			} else {
				converter.convertToRdf();
			}
		}
	}

	private static final Pattern COMMA = Pattern.compile(",", Pattern.LITERAL);

	private void convertToRdf() {
		Model model = new LinkedHashModel();
		if ("all".equals(projects)) {
			try (Stream<Path> list = Files.list(inputDirectory)) {
				parseAll(list, model);
			} catch (IOException e) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(e);
			}
		} else {
			try (Stream<Path> list = COMMA.splitAsStream(projects).map(inputDirectory::resolve)) {
				parseAll(list, model);
			}
		}
		print(model);
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

	private void parseAll(Stream<Path> paths, Model model) {
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

	private void parseTurtleFileIntoModel(Model model, Path p) {
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);

		rdfParser.setRDFHandler(new StatementCollector(model));
		try (InputStream is = Files.newInputStream(p)) {
			rdfParser.parse(is);
		} catch (RDFParseException | RDFHandlerException e) {
			Failure.CANT_PARSE_EXAMPLE.exit(e);
		} catch (IOException e) {
			Failure.CANT_READ_EXAMPLE.exit(e);
		}
	}

	private void print(Model model) {
		Rio.write(model, System.out,
				RDFFormat.matchFileName("a." + outputFormat, outputFormats).orElse(RDFFormat.TURTLE));
	}

}
