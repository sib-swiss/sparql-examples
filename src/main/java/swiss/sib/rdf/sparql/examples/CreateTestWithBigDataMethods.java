package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.openrdf.query.MalformedQueryException;

import com.bigdata.rdf.sail.sparql.Bigdata2ASTSPARQLParser;

import swiss.sib.rdf.sparql.examples.vocabularies.SIB;

public class CreateTestWithBigDataMethods {

	static void testQueryValid(Path p) {
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


		Iterable<Statement> statements = model.getStatements(null, SIB.BIGDATA_SELECT, null);
		testAllQueryStringsInModel(new Bigdata2ASTSPARQLParser(), statements.iterator());
	}


	private static void testAllQueryStringsInModel(Bigdata2ASTSPARQLParser parser, Iterator<Statement> i) {
		while (i.hasNext()) {
			Statement next = i.next();
			testQueryStringInValue(parser, next);
		}
	}

	private static void testQueryStringInValue(Bigdata2ASTSPARQLParser parser, Statement next) {
		Value obj = next.getObject();
		assertNotNull(obj);
		assertTrue(obj.isLiteral());

		try {
			parser.parseQuery(obj.stringValue(), "https://example.org/");
		} catch (MalformedQueryException qe) {
			fail(qe.getMessage() + "\n" + obj.stringValue());
		}
	}
}
