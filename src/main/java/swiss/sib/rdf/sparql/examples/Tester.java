package swiss.sib.rdf.sparql.examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.junit.platform.console.ConsoleLauncher;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.options.PicocliCommandLineOptionsParser;
import org.junit.platform.console.tasks.ConsoleTestExecutor;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class Tester {

	public static enum Failure {
		CANT_READ_INPUT_DIRECTORY(1), CANT_PARSE_EXAMPLE(2), CANT_READ_EXAMPLE(3), CANT_WRITE_EXAMPLE_RQ(4), JUNIT(5);

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
			System.err.println(queryS + "." + e.getMessage());
			e.printStackTrace();
			System.exit(exitCode);

		}

	}

	@Option(names = { "-i",
			"--input-directory" }, paramLabel = "directory containing example files to convert", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path inputDirectory;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@Option(names = { "-p", "--project" }, paramLabel = "projects to convert", defaultValue = "all")
	private String projects;

	@Option(names = { "--also-run-slow-tests" })
	private boolean alsoRunSlowTests;

	public static void main(String[] args) {
		Tester converter = new Tester();
		CommandLine commandLine = new CommandLine(converter);
		commandLine.parseArgs(args);
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
			return;
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
			return;
		} else {
			converter.test();
		}
	}

	private static final Pattern COMMA = Pattern.compile(",", Pattern.LITERAL);

	private void test() {

		if ("all".equals(projects)) {
			try (Stream<Path> list = Files.list(inputDirectory)) {
				test(list);
			} catch (IOException e) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(e);
			} catch (Exception e) {
				Failure.JUNIT.exit(e);
			}
		} else {
			try (Stream<Path> list = COMMA.splitAsStream(projects).map(inputDirectory::resolve)) {
				test(list);
			} catch (Exception e) {
				Failure.JUNIT.exit(e);
			}
		}
	}

	private void test(Stream<Path> list) throws Exception {
		ConsoleLauncher.execute(System.out, System.err, "--fail-if-no-tests", "--include-engine",
				"junit-jupiter", "--select-class", ValidateSparqlExamplesTest.class.getName());
	}

}
