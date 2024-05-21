package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

public class CreateTestWithRDF4jMethods {
	private static final String shaclOneIdOneQuery = """
			PREFIX sh:<http://www.w3.org/ns/shacl#>
			PREFIX schema:<https://schema.org/>

			[] sh:targetClass sh:SPARQLExecutable ;
				sh:property [
					sh:path [ sh:alternativePath ( sh:select sh:ask sh:describe sh:construct ) ] ;
					sh:maxCount 1 ;
					sh:minCount 1
				] , [
					sh:path rdfs:comment ;
					sh:maxCount 1 ;
					sh:minCount 1 ] , [
				sh:path schema:target;
					sh:minCount 1 ].
			""";

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
		Stream.of("ask", "select", "concat", "describe").map(
				s -> model.getStatements(null, SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, s), null))
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

	/**
	 * Use shacl to test all the turtle files contain an rdfs:comment and one query.
	 * Also makes a test that all example IRIs are unique.
	 * 
	 * @param paths
	 */
	static void testShaclContsraints(Stream<Path> paths) {
		MemoryStore memoryStore = new MemoryStore();
		try {
			memoryStore.init();

			ShaclSail shaclSail = new ShaclSail(memoryStore);
			Repository repo = new SailRepository(shaclSail);
			try (RepositoryConnection connection = repo.getConnection();
					StringReader sr = new StringReader(shaclOneIdOneQuery)) {
				// add shapes
				connection.begin();
				connection.add(sr, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
				connection.commit();
			} catch (RDFParseException | RepositoryException | IOException e) {
				fail(e);
			}
			try (RepositoryConnection connection = repo.getConnection()) {
				paths.forEach(p -> testValidatingAFile(connection, p));
			}
			repo.shutDown();
			shaclSail.shutDown();
		} finally {
			memoryStore.shutDown();
		}
	}

	private static void testValidatingAFile(RepositoryConnection connection, Path p) {
		assertTrue(Files.exists(p));
		connection.begin();
		try {
			IRI iri = connection.getValueFactory().createIRI(p.toUri().toString());
			connection.add(p.toFile(), iri);
			connection.commit();
		} catch (RDFParseException | RepositoryException | IOException e) {
			if (e.getCause() instanceof ValidationException ve) {
				String report = validationReportAsString(ve);
				fail(p.toUri() + " failed " + ve + '\n'+ report, e.getCause());
			}
			fail(e);
		}
		assertFalse(connection.size() == 0);
	}

	private static String validationReportAsString(ValidationException ve) {
		Model vem = ve.validationReportAsModel();
		var boas = new ByteArrayOutputStream();
		TurtleWriter tw = new TurtleWriter(boas);
		tw.startRDF();
		Iterator<Statement> iterator = vem.iterator();
		while(iterator.hasNext()) {
			tw.handleStatement(iterator.next());
		}
		tw.endRDF();
		String report = boas.toString();
		return report;
	}

}
