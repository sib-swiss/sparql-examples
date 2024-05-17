package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.RDFDataMgr;

public class CreateTestWithJenaMethods {

	static void testQueryValid(Path p, String projectPrefixes) {
		assertTrue(Files.exists(p));
		Model model = RDFDataMgr.loadModel(p.toUri().toString());
		assertFalse(model.isEmpty());
		Stream.of("ask", "select", "concat", "describe")
				.map(s -> model.listObjectsOfProperty(model.createProperty("http://www.w3.org/ns/shacl#", s)))
				.map(NodeIterator::toList).flatMap(List::stream).forEach(n -> {
					assertNotNull(n);
					Literal ql = n.asLiteral();

					try {
						Query qry = QueryFactory.create(projectPrefixes + ql.getString());
						Query q = QueryFactory.create(qry);
						assertNotNull(q);
					} catch (QueryException qe) {
						fail(qe.getMessage(), qe);
					}
				});

	}

}
