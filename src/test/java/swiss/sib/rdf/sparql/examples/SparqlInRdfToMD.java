package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;

public class SparqlInRdfToMD {

	@Test
	public void simple() {
		Model model = parse(ExamplesUsedInTest.simple);
		String res = SparqlInRdfToMd.asMD(model).stream().collect(Collectors.joining());
		assertTrue(res.contains("SWISSLIPID:"));
	}

	@Test
	public void rhea9() {
		Model model = parse(ExamplesUsedInTest.rhea9);
		String res = SparqlInRdfToMd.asMD(model).stream().collect(Collectors.joining());
		assertTrue(res.contains("rh:ec"));
	}
	
	@Test
	public void rhea9Anon() {
		Model model = parse(ExamplesUsedInTest.rhea9Anon);
		String res = SparqlInRdfToMd.asMD(model).stream().collect(Collectors.joining());
		assertTrue(res.contains("rh:ec"));
	}

	private Model parse(String ttl) {
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));
		try (InputStream newInputStream = new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8))) {
			rdfParser.parse(newInputStream);
		} catch (RDFParseException | RDFHandlerException | IOException e) {
			fail(e);
		}
		return model;
	}

}
