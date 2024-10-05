package swiss.sib.rdf.sparql.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.platform.console.ConsoleLauncher;
import org.junit.platform.console.ConsoleLauncherExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import swiss.sib.rdf.sparql.examples.tests.ValidateSparqlExamplesTest;

@Command(name = "test", description = "Tests the example files")
public class Tester implements Callable<Integer> {
	// Next cell in a markdown table
	private static final String NC = " | ";

	@Spec
	CommandSpec spec;

	@Option(names = { "-i",
			"--input-directory" }, paramLabel = "directory containing example files to test", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path inputDirectory;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@Option(names = { "-p", "--project" }, paramLabel = "projects to test", defaultValue = "all")
	private String projects;

	@Option(names = { "--also-run-slow-tests" })
	private boolean alsoRunSlowTests;

	@Option(names = { "--status-markdown" })
	private File statusMarkdown;

	@Override
	public Integer call() throws Exception {
		CommandLine commandLine = spec.commandLine();
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
		} else {
			return test();
		}
		return 0;
	}

	private static final Pattern COMMA = Pattern.compile(",", Pattern.LITERAL);

	private int test() {

		if ("all".equals(projects)) {
			try (Stream<Path> list = Files.list(inputDirectory)) {
				return test(list);
			} catch (IOException e) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(e);
			} catch (Exception e) {
				Failure.JUNIT.exit(e);
			}
		} else {
			try (Stream<Path> list = COMMA.splitAsStream(projects).map(inputDirectory::resolve)) {
				return test(list);
			} catch (Exception e) {
				Failure.JUNIT.exit(e);
			}
		}
		return Failure.DID_NOTHING.exitCode();
	}

	private int test(Stream<Path> list) throws Exception {
		System.setProperty(Tester.class.getName(), inputDirectory.toString());
		List<String> standardOptions = List.of("--fail-if-no-tests", "--include-engine", "junit-jupiter",
				"--select-package", ValidateSparqlExamplesTest.class.getPackageName());
		if (!alsoRunSlowTests) {
			standardOptions = new ArrayList<>(standardOptions);
			standardOptions.add("--exclude-tag");
			standardOptions.add("SlowTest");
		}
		ConsoleLauncherExecutionResult execute = ConsoleLauncher.execute(System.out, System.err,
				(String[]) standardOptions.toArray(new String[0]));
		if (statusMarkdown != null && execute.getTestExecutionSummary().isPresent()) {
			try (BufferedWriter w = Files.newBufferedWriter(statusMarkdown.toPath(),
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
				TestExecutionSummary tes = execute.getTestExecutionSummary().get();
				Map<String, List<TestExecutionSummary.Failure>> collect = tes.getFailures().stream()
						.collect(Collectors.groupingBy((f) -> {
							TestIdentifier ti = f.getTestIdentifier();
							Optional<UniqueId> opid = ti.getParentIdObject();
							if (opid.isPresent()) {
								return opid.get().getLastSegment().getValue();
							}
							return ti.getUniqueId();
						}));
				printMarkdownSummary(w, tes);
				w.append("# Summary regarding test failures");
				w.newLine();
				if (collect.containsKey("testAllWithBigData()")) {
					List<TestExecutionSummary.Failure> bigdata = collect.get("testAllWithBigData()");
					w.append("| test group | failed | compared to blazegraph |");
					w.newLine();
					w.append("| ---- | ---- | ---- |");
					w.newLine();
					for (Map.Entry<String, List<TestExecutionSummary.Failure>> gf : collect.entrySet()) {
						if (!gf.getKey().equals("testAllWithBigData()") && gf.getKey().startsWith("testAllWith")) {
							w.append(NC);
							w.append(gf.getKey());
							w.append(NC);
							w.append(Integer.toString(gf.getValue().size()));
							w.append(NC);
							w.append(percentage(gf.getValue().size(), bigdata.size()));
							w.append(NC);
							w.newLine();
						}
					}
				} else {
					for (Map.Entry<String, List<TestExecutionSummary.Failure>> gf : collect.entrySet()) {
						w.append("# Summary " + gf.getKey() + " (" + gf.getValue().size() + ")");
						w.newLine();
						w.append("");
						w.newLine();
					}
				}
				w.newLine();
				w.newLine();
				for (Map.Entry<String, List<TestExecutionSummary.Failure>> gf : collect.entrySet()) {
					w.append("# Failures " + gf.getKey() + " (" + gf.getValue().size() + ")");
					w.newLine();
					w.append("| test name | exception |");
					w.newLine();
					w.append("| ---- | ---- |");
					w.newLine();
					for (var failure : gf.getValue()) {
						String dn = failure.getTestIdentifier().getDisplayName();
						w.append("|[").append(dn).append("](examples/").append(dn).append(")|");
						String em = failure.getException().getMessage();
						if (em == null) {
							w.append(" ");
						} else {
							em = em.replace('\n', ' ').replace('\r', ' ');
							if (em.length() > 80) {
								w.append(em, 0, 80).append("...");
							} else {
								w.append(em);
							}
						}
						w.append("|");
						w.newLine();
					}

				}
			}
		}
		return execute.getExitCode();

	}

	public void printMarkdownSummary(BufferedWriter w, TestExecutionSummary tes) throws IOException {
		w.append("# Summary regarding test failures");
		w.newLine();
		w.newLine();
		w.append("| total | tests | percentage");
		w.newLine();
		w.append("| ---- | ---- | ---- |");
		w.newLine();
		long found = tes.getTestsFoundCount();
		long failed = tes.getTestsFailedCount();
		long aborted = tes.getTestsAbortedCount();
		long skipped = tes.getTestsSkippedCount();
		long passed = tes.getTestsSucceededCount();
		w.append("| found | ").append(Long.toString(found)).append(NC).append("100%|");
		w.newLine();
		w.append("| failed | ").append(Long.toString(failed)).append(NC).append(percentage(failed, found))
				.append(NC);
		w.newLine();
		w.append("| aborted | ").append(Long.toString(aborted)).append(NC).append(percentage(aborted, found))
				.append(NC);
		w.newLine();
		w.append("| skipped | ").append(Long.toString(skipped)).append(NC).append(percentage(skipped, found))
				.append(NC);
		w.newLine();
		w.append("| passed | ").append(Long.toString(passed)).append(NC).append(percentage(passed, found))
				.append(NC);
		w.newLine();
		w.newLine();
	}

	public String percentage(long part, long whole) {
		if (part > 0) {
			return NumberFormat.getPercentInstance().format((part / (double) whole));
		} else {
			return "-";
		}
	}
}
