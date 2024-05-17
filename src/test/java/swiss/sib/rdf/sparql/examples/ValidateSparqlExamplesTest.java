package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

public class ValidateSparqlExamplesTest {
	private static final String FIND_PREFIXES = """
			PREFIX sh:<http://www.w3.org/ns/shacl#>
			SELECT ?s
			WHERE
			{
				?pn sh:prefix ?prefix ;
					sh:namespace ?namespaceI .
				BIND(CONCAT('PREFIX ',?prefix, ':<',(STR(?namespaceI)),'>') AS ?s)
			}
			""";

	@TestFactory
	public Stream<DynamicTest> testAllWithJena() throws URISyntaxException, IOException {
		BiFunction<Path, String, Executable> tester = (p,
				projectPrefixes) -> () -> CreateTestWithJenaMethods.testQueryValid(p, projectPrefixes);
		return testAll(tester);
	}

	@TestFactory
	public Stream<DynamicTest> testAllWithRDF4j() throws URISyntaxException, IOException {
		BiFunction<Path, String, Executable> tester = (p,
				projectPrefixes) -> () -> CreateTestWithRDF4jMethods.testQueryValid(p, projectPrefixes);
		return testAll(tester);
	}

	private Stream<DynamicTest> testAll(BiFunction<Path, String, Executable> tester)
			throws URISyntaxException, IOException {
		URL baseDir = getClass().getResource("/");
		Path basePath = Paths.get(baseDir.toURI());
		String commonPrefixes = extractPrefixes(Paths.get(getClass().getResource("/prefixes.ttl").toURI()));
		return Files.list(basePath).flatMap(path -> {
			try {
				Stream<DynamicTest> testForEachExample = Files.walk(path, 5).filter(this::isTurtleButNotPrefixFile)
						.map(p -> createTest(tester, path, extractProjectPrefixes(commonPrefixes, path), p));
				return Stream.concat(testPrefixes(path), testForEachExample);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private DynamicTest createTest(BiFunction<Path, String, Executable> tester, Path path, String projectPrefixes,
			Path p) {
		String testName = path.getFileName().toString() + ':' + p.getFileName().toString();
		Executable apply = tester.apply(p, projectPrefixes);
		return DynamicTest.dynamicTest(testName, apply);
	}

	String extractProjectPrefixes(String commonPrefixes, Path path) {
		String projectPrefixes = commonPrefixes;
		Path projectPrefixesPath = Paths.get(path.toString(), "prefixes.ttl");
		if (Files.exists(projectPrefixesPath)) {
			projectPrefixes = commonPrefixes + extractPrefixes(projectPrefixesPath);
		}
		return projectPrefixes;
	}

	private Stream<DynamicTest> testPrefixes(Path path) {
		Path prefixes = Paths.get(path.toString(), "prefixes.ttl");
		if (Files.exists(prefixes)) {
			return Stream
					.of(DynamicTest.dynamicTest(prefixes.getParent().getFileName().toString() + ":prefixes.ttl", () -> {
						try {
							Model model = RDFDataMgr.loadModel(prefixes.toUri().toString());
							assertFalse(model.isEmpty());
						} catch (RiotException e) {
							fail(e);
						}
					}));
		} else {
			return Stream.empty();
		}
	}

	private String extractPrefixes(Path prefixes) {
		Model model = RDFDataMgr.loadModel(prefixes.toUri().toString());
		StringBuilder prefixesSB = new StringBuilder();
		Query qry = QueryFactory.create(FIND_PREFIXES);
		QueryExecution qe = QueryExecutionFactory.create(qry, model);
		ResultSet rs = qe.execSelect();
		while (rs.hasNext()) {
			QuerySolution next = rs.next();
			prefixesSB.append(next.getLiteral("s").getValue());
		}
		rs.close();
		qe.close();
		return prefixesSB.toString();
	}

	private boolean isTurtleButNotPrefixFile(Path p) {
		return Files.exists(p) && p.toUri().getPath().endsWith(".ttl") && !p.toUri().getPath().endsWith("prefixes.ttl")
				&& Files.isRegularFile(p);
	}
}
