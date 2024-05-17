package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public class CreateTestWithRDF4jMethods {

	static void testQueryValid(Path p, String projectPrefixes) {
		assertTrue(Files.exists(p));
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));
		try (InputStream newInputStream = Files.newInputStream(p)) {
			rdfParser.parse(newInputStream);
		} catch (RDFParseException | RDFHandlerException | IOException e) {
			fail(e);
		}
		assertFalse(model.isEmpty());
		QueryParser parser = new SPARQLParserFactory().getParser();
		Stream.of("ask", "select", "concat", "describe")
				.map(s -> model.getStatements(null,
						SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, s), null))
				.map(Iterable::iterator).forEach(i -> {
					while (i.hasNext()) {
						Value obj = i.next().getObject();
						assertNotNull(obj);
						assertTrue(obj.isLiteral());
						String queryStr = projectPrefixes + obj.stringValue();

						try {
							parser.parseQuery(queryStr, "https://example.org/");
						} catch (MalformedQueryException qe) {
							fail(qe.getMessage() + "\n" + queryStr, qe);
						}
					}
				});

	}

}
