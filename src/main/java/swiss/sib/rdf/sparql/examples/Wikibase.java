package swiss.sib.rdf.sparql.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.evaluation.function.hash.MD5;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

@Command(name = "wikibase", description = "Converts queries from wikibase wiki into sparql-example-files")
public class Wikibase implements Callable<Integer> {
	@Spec
	CommandSpec spec;

	@Option(names = { "-o",
			"--output-directory" }, paramLabel = "directory to store the extracted example files", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path outputDirectory;

	@Option(names = { "-u",
			"--wiki-url" }, paramLabel = "url of the wiki to search", description = "The wiki associated with the wikidata instance.", defaultValue = "https://www.wikidata.org/")
	private String wikidatawiki = "https://www.wikidata.org/";

	@Option(names = { "-s",
			"--sparql-endpoint-url" }, paramLabel = "url of the wikidata sparql endpoint", description = "The wikidata sparql endpoint instance.", defaultValue = "https://query.wikidata.org/sparql/")
	private String wikidatasparql = "https://query.wikidata.org/sparql/";

	@Option(names = { "-e",
			"--your-email" }, paramLabel = "your e-mail address", description = "Used as header in all requests. Per wikipedia API guidelines", required = true)
	private String yourEmail = "https://www.wikidata.org/";

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@Option(names = { "--use-cached" }, description = "use prior retrieved files, do not request an update")
	private boolean useCached;

	private File outputSearchResultDir;
	private File outputHtmlDir;
	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	private static final IRI CC_BY_4 = VF.createIRI("https://creativecommons.org/licenses/by-sa/4.0/");

	public Integer call() {
		CommandLine commandLine = spec.commandLine();
		System.err.println("outputDirectory: " + outputDirectory);

		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
			return 0;
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
			return 0;
		} else {
			outputSearchResultDir = new File(outputDirectory.toFile(), "searchResults");
			if (!outputSearchResultDir.exists() && !outputSearchResultDir.mkdirs()) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(null);
			}
			outputHtmlDir = new File(outputDirectory.toFile(), "html");
			if (!outputHtmlDir.exists() && !outputHtmlDir.mkdirs()) {
				Failure.CANT_READ_INPUT_DIRECTORY.exit(null);
			}
			fetchHtml();
		}
		return 0;
	}

	private enum NamedTemplate {
		SPARQL("SPARQL", ".mw-highlight-lang-sparql"), 
//		WDQUERY("Wdquery", ".mw-highlight-lang-sparql"),
//		SPARQL_INLINE("SPARQL_Inline/doc", ".mw-highlight-lang-sparql"),
		SPARQL2("SPARQL2", ".mw-highlight-lang-sparql"),;

		private final String name;
		private final String cssClass;

		NamedTemplate(String name, String cssClass) {
			this.name = name;
			this.cssClass = cssClass;

		}
	}

	private void fetchHtml() {

		for (NamedTemplate nt : NamedTemplate.values()) {
			String url = wikidatawiki + "w/index.php?title=Special:WhatLinksHere/Template:" + nt.name;

			try {
				while (url != null) {
					Document searchResultDocument = retrieveAndCacheResult(url, outputSearchResultDir);
					Elements ulWhatLinksHereList = searchResultDocument.select("#mw-whatlinkshere-list li > a");
					for (Element link : ulWhatLinksHereList) {
						String pageLinkingToSparqlTemplate = makeAbsoluteUrl(link);
						System.err.println(pageLinkingToSparqlTemplate);
						extractSparqlQueriesFromPage(pageLinkingToSparqlTemplate, nt);
					}
					Element nextLink = searchResultDocument.select(".mw-nextlink").first();
					url = makeAbsoluteUrl(nextLink);
				}
			} catch (IOException e) {
				Failure.CONNECTION_TO_WIKIDATA_WIKI_FAIL.exit(e);
			}
		}
	}

	private String makeAbsoluteUrl(Element link) {
		String pageLinkingToSparqlTemplate = link.attr("href");
		if (pageLinkingToSparqlTemplate.startsWith("/")) {
			pageLinkingToSparqlTemplate = wikidatawiki + pageLinkingToSparqlTemplate;
		}
		return pageLinkingToSparqlTemplate;
	}

	private static final Pattern SELECT = Pattern.compile("SELECT", Pattern.CASE_INSENSITIVE);
	private static final Pattern CONSTRUCT = Pattern.compile("CONSTRUCT", Pattern.CASE_INSENSITIVE);
	private static final Pattern ASK = Pattern.compile("ASK", Pattern.CASE_INSENSITIVE);

	private void extractSparqlQueriesFromPage(String pageLinkingToSparqlTemplate, NamedTemplate nt) throws IOException {
		Document htmlPageDocument = retrieveAndCacheResult(pageLinkingToSparqlTemplate, outputHtmlDir);
		if (htmlPageDocument == null) {
			System.err.println("Could not retrieve:" + pageLinkingToSparqlTemplate);
		} else {
			String languageInHtml = htmlPageDocument.child(0).attr("lang");

			Elements sparqlTemplates = htmlPageDocument.select(nt.cssClass);
			for (Element sparqlTemplate : sparqlTemplates) {
				String query = sparqlTemplate.select("pre").eachText().stream().collect(Collectors.joining("\n"));
				LinkedHashModel model = new LinkedHashModel();

				String urlForFileName = new MD5().evaluate(VF, VF.createLiteral(query)).stringValue();
				IRI iriForQuery = VF.createIRI(wikidatawiki + "#query-" + urlForFileName);
				addQueryStringToModel(query, model, iriForQuery);
				StringBuilder sb = makeThePreviosSiblingNodesTheLabel(sparqlTemplate);
				if (!sb.isEmpty() && languageInHtml != null && !languageInHtml.isBlank()) {
					model.add(iriForQuery, RDFS.COMMENT, VF.createLiteral(sb.toString(), languageInHtml));
				} else if (!sb.isEmpty()) {
					model.add(iriForQuery, RDFS.COMMENT, VF.createLiteral(sb.toString()));
				}
				model.add(iriForQuery, DCTERMS.IS_PART_OF, VF.createIRI(pageLinkingToSparqlTemplate));
				model.add(iriForQuery, DCTERMS.LICENSE, CC_BY_4);
				model.add(iriForQuery, SHACL.PREFIXES, VF.createBNode("wikidata_prefixes"));
				model.add(iriForQuery, SchemaDotOrg.TARGET, VF.createIRI(wikidatasparql));
				writeModelToTurtle(model, urlForFileName);
			}
		}
	}

	private StringBuilder makeThePreviosSiblingNodesTheLabel(Element sparqlTemplate) {
		Element parent = sparqlTemplate.parent();
		List<Node> childNodes = parent.childNodes();
		StringBuilder sb = new StringBuilder();
		for (Node childNode : childNodes) {
			if (childNode.equals(sparqlTemplate)) {
				return sb;
			} else {
				if (childNode instanceof TextNode tn) {
					sb.append(tn.text());
				} else if (childNode instanceof Element e) {
					sb.append(e.text());
				}
			}
		}
		return sb;
	}

	private void writeModelToTurtle(LinkedHashModel model, String urlForFileName)
			throws IOException, FileNotFoundException {
		File turtleDestination = new File(outputDirectory.toFile(),
				urlForFileName + "." + RDFFormat.TURTLE.getDefaultFileExtension());
		// If the turle file already exists we have seen this query before.
		// We might be able to add more rdfs:comment in other languages
		if (turtleDestination.exists()) {
			try (FileInputStream fileInputStream = new FileInputStream(turtleDestination)) {
				Model existingModel = Rio.parse(fileInputStream, RDFFormat.TURTLE);
				model.addAll(existingModel);
			}
		}
		try (FileOutputStream output = new FileOutputStream(turtleDestination)) {
			model.getNamespaces().add(SHACL.NS);
			model.getNamespaces().add(RDF.NS);
			model.getNamespaces().add(RDFS.NS);
			model.getNamespaces().add(DCTERMS.NS);
			model.getNamespaces().add(SchemaDotOrg.NS);
			Rio.write(model, output, RDFFormat.TURTLE);
		}
	}

	private void addQueryStringToModel(String query, LinkedHashModel model, IRI iriForQuery) {
		model.add(iriForQuery, RDF.TYPE, SHACL.SPARQL_EXECUTABLE);
		if (SELECT.matcher(query).find()) {
			model.add(iriForQuery, RDF.TYPE, SHACL.SPARQL_SELECT_EXECUTABLE);
			model.add(iriForQuery, SHACL.SELECT, VF.createLiteral(query));
		} else if (CONSTRUCT.matcher(query).find()) {
			model.add(iriForQuery, RDF.TYPE, SHACL.SPARQL_CONSTRUCT_EXECUTABLE);
			model.add(iriForQuery, SHACL.CONSTRUCT, VF.createLiteral(query));
		} else if (ASK.matcher(query).find()) {
			model.add(iriForQuery, RDF.TYPE, SHACL.SPARQL_ASK_EXECUTABLE);
			model.add(iriForQuery, SHACL.ASK, VF.createLiteral(query));
		}
	}

	private Document retrieveAndCacheResult(String url, File output) throws IOException {

		String urlForFileName = URLEncoder.encode(url, StandardCharsets.UTF_8);
		if (urlForFileName.length() > 255) {
			urlForFileName = urlForFileName.substring(0, 255);
		}
		File cacheLocationF = new File(output, urlForFileName);
		Path cacheLocation = cacheLocationF.toPath();
		if (Files.exists(cacheLocation)
				&& (useCached || Files.getLastModifiedTime(cacheLocation).toInstant().isAfter(Instant.now()))) {
			return Jsoup.parse(cacheLocationF);
		} else {
			int tries = 0;
			do {
				try {
					tries++;
					Connection connect = Jsoup.connect(url);
					connect.userAgent(yourEmail).method(Method.GET);
					Response response = connect.execute();
					if (response.statusCode() == 200) {
						return parseAndCacheSuccefullResponse(cacheLocation, response);
					}
					sleep(100);
				} catch (SocketTimeoutException | HttpStatusException ste) {
					// If we had a socket timeout we sleep longer before trying again.
					sleep(1000);
				}
			} while (tries <= 3);
			return null;
		}
	}

	private Document parseAndCacheSuccefullResponse(Path cacheLocation, Response response) throws IOException {
		Document document;
		byte[] bodyAsBytes = response.bodyAsBytes();
		document = response.parse();
		Files.write(cacheLocation, bodyAsBytes);

		// Wikipedia asks us to sleep for 100ms between requests

		if (response.hasHeader("expires")) {
			String expires = response.header("expires");
			TemporalAccessor temporal = DateTimeFormatter.RFC_1123_DATE_TIME.parse(expires);
			Instant expiresInstant = Instant.from(temporal);
			Files.setLastModifiedTime(cacheLocation, FileTime.from(expiresInstant));
		}
		return document;
	}

	private void sleep(int ms) {
		try {
			Thread.sleep(Duration.ofMillis(ms));
		} catch (InterruptedException e) {
			Thread.interrupted();
		}
	}
}
