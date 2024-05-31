package swiss.sib.rdf.sparql.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class FindFiles {

	public static boolean isTurtleButNotPrefixFile(Path p) {
		return Files.exists(p) && p.toUri().getPath().endsWith(".ttl") && !p.toUri().getPath().endsWith("prefixes.ttl")
				&& Files.isRegularFile(p);
	}
	
	private static boolean isTurtleAndPrefixFile(Path p) {
		return Files.exists(p) && p.toUri().getPath().endsWith("prefixes.ttl")
				&& Files.isRegularFile(p);
	}

	public static Stream<Path> sparqlExamples() throws URISyntaxException, IOException {
		return sparqlExamples(getBasePath());
	}
	
	public static Stream<Path> sparqlExamples(Path path) throws IOException {
		return Files.walk(path).filter(FindFiles::isTurtleButNotPrefixFile);
	}

	public static Path getBasePath() throws URISyntaxException {
		URL baseDir = FindFiles.class.getResource("/");
		return Paths.get(baseDir.toURI());
	}

	public static Stream<Path> allPrefixFiles() throws IOException, URISyntaxException{
		return Files.walk(getBasePath()).filter(FindFiles::isTurtleAndPrefixFile);
	}

	public static Path commonPrefixes() throws URISyntaxException {
		return Paths.get(FindFiles.class.getResource("/prefixes.ttl").toURI());
	}
}
