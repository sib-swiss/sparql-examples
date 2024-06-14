package swiss.sib.rdf.sparql.examples;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class Converter {

	private static enum Failure {
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

	}

	private final Set<RDFFormat> outputFormats = Set.of(RDFFormat.TURTLE, RDFFormat.RDFXML, RDFFormat.NTRIPLES,
			RDFFormat.JSONLD, RDFFormat.NDJSONLD);

	@Option(names = { "-f",
			"--format" }, paramLabel = "output RDF format", description = "the format that the example queries with their prefixes should be concattenated into", defaultValue = "ttl")
	private String outputFormat;

	@Option(names = { "-r",
			"--rq" }, paramLabel = "output example files as rq files next to the exiting turtle", description = "output example files as rq files next to the exiting turtle", defaultValue = "false")
	private boolean outputRq;

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
			if (converter.outputRq) {
				converter.convertToRQs();
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

	private void convertToRQs() {

		if ("all".equals(projects)) {
			try (Stream<Path> list = Files.list(inputDirectory)) {
				convertProjectsToRq(list);
			} catch (IOException e) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(e);
			}
		} else {
			try (Stream<Path> list = COMMA.splitAsStream(projects).map(inputDirectory::resolve)) {
				convertProjectsToRq(list);
			}
		}
	}

	private void convertProjectsToRq(Stream<Path> list) {
		Optional<Path> findCommonPrefixes = FindFiles.prefixFile(inputDirectory).findFirst();
		Model commonPrefixes = prefixModel(findCommonPrefixes);
		list.forEach(pro -> {
			try {
				Optional<Path> findProjectPrefixes = FindFiles.prefixFile(pro).findFirst();
				Model projectPrefixes = prefixModel(findProjectPrefixes);
				FindFiles.sparqlExamples(pro).forEach((p) -> {
					Model ex = parseSingle(p);
					ex.addAll(commonPrefixes);
					ex.addAll(projectPrefixes);
					String pfn = p.getFileName().toString();
					String prqfn = pfn.substring(0, pfn.indexOf('.')) + ".rq";
					Path prq = p.getParent().resolve(prqfn);
					try {
						List<String> rq = SparqlInRdfToRq.asRq(ex);
						Files.write(prq, rq, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
					} catch (IOException e) {
						Failure.CANT_WRITE_EXAMPLE_RQ.exit(e);
						throw new RuntimeException(e);
					}
				});
			} catch (IOException e) {
				Failure.CANT_PARSE_EXAMPLE.exit(e);
				throw new RuntimeException(e);
			}
		});
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
