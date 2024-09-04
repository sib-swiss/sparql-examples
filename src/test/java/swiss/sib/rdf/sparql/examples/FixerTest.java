package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.junit.jupiter.api.Test;


public class FixerTest {
	private final String blazeGraphIncludeExample = """
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			SELECT ?item ?itemLabel
			WITH
			{
			  	SELECT DISTINCT ?itemLabel
			  	WHERE
			  	{
			    	?item rdfs:label ?itemLabel .
			    }
			} AS %get_labels
			WHERE
			{
			  INCLUDE %get_labels
			  ?item a rdfs:Class .
			}""";

	@Test
	public void simpleIncludeWith() {
		try {
			String fix = Fixer.fix(blazeGraphIncludeExample);
			assertFalse(fix.contains("WITH"));
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix, "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		} catch (org.openrdf.query.MalformedQueryException e) {
			fail(e);
		}
	}
}
