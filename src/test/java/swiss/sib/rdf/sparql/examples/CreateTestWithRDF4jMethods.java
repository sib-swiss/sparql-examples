package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public class CreateTestWithRDF4jMethods {
	private static final IRI DESCRIBE = SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, "describe");

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
		Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, DESCRIBE)
			.map(s -> model.getStatements(null, s, null))
			.map(Iterable::iterator)
			.forEach(i -> testAllQueryStringsInModel(projectPrefixes, parser, i));

	}

	static Stream<String> extractServiceEndpoints(Path p, String projectPrefixes) {
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
		
		return Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, DESCRIBE).map(
				s -> model.getStatements(null, s, null))
				.map(Iterable::iterator).map(i -> {
					return collectServiceIrisInFromOneExample(projectPrefixes, parser, i);
				}).flatMap(Set::stream);
	}

	private static Set<String> collectServiceIrisInFromOneExample(String projectPrefixes, QueryParser parser,
			Iterator<Statement> i) {
		Set<String> serviceIris = new HashSet<>();
		while (i.hasNext()) {
			Value obj = i.next().getObject();
			String queryStr = projectPrefixes + obj.stringValue();

			try {
				ParsedQuery query = parser.parseQuery(queryStr, "https://example.org/");
				query.getTupleExpr().visit(new AbstractQueryModelVisitor<RuntimeException>() {

					@Override
					public void meet(Service node) throws RuntimeException {
						serviceIris.add(node.getServiceRef().getValue().stringValue());
						super.meet(node);
					}

				});
			} catch (MalformedQueryException qe) {
				//Ignore as tested by above;
			}
		}
		return serviceIris;
	}

	private static void testAllQueryStringsInModel(String projectPrefixes, QueryParser parser, Iterator<Statement> i) {
		while (i.hasNext()) {
			Statement next = i.next();
			testQueryStringInValue(projectPrefixes, parser, next);
		}
	}

	private static void testQueryStringInValue(String projectPrefixes, QueryParser parser, Statement next) {
		Value obj = next.getObject();
		assertNotNull(obj);
		assertTrue(obj.isLiteral());
		String queryStr = projectPrefixes + obj.stringValue();

		try {
			parser.parseQuery(queryStr, "https://example.org/");
		} catch (MalformedQueryException qe) {
			fail(qe.getMessage() + "\n" + queryStr, qe);
		}
	}

}
