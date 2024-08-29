package swiss.sib.rdf.sparql.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.platform.console.ConsoleLauncher;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import swiss.sib.rdf.sparql.examples.tests.ValidateSparqlExamplesTest;

public class Tester {

	@Option(names = { "-i",
			"--input-directory" }, paramLabel = "directory containing example files to test", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path inputDirectory;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@Option(names = { "-p", "--project" }, paramLabel = "projects to test", defaultValue = "all")
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
		System.setProperty(Tester.class.getName(), inputDirectory.toString());
		List<String> standardOptions = List.of("--fail-if-no-tests", "--include-engine",
				"junit-jupiter", "--select-class", ValidateSparqlExamplesTest.class.getName());
		if (!alsoRunSlowTests) {
			standardOptions = new ArrayList<>(standardOptions);
			standardOptions.add("--exclude-tag");
			standardOptions.add("SlowTest");
		}
		ConsoleLauncher.execute(System.out, System.err, (String[]) standardOptions.toArray(new String[0]));
	}

}
