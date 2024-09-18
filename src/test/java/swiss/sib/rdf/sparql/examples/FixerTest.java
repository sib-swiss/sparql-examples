package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.junit.jupiter.api.Test;


public class FixerTest {
	private final String missingPrefix = """
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			SELECT ?item ?itemLabel
			WHERE
			{
			  ?item a rdfs:Class ;
			          skos:prefLabel ?itemLabel .
			}""";
	
	private final String blazeGraphIncludeExample = """
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			SELECT ?item ?itemLabel WITH
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
			String fix = Fixer.fixBlazeGraphIncludeWith(blazeGraphIncludeExample, "", null);
			assertFalse(fix.contains("WITH"));
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix, "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}
	
	@Test
	public void simpleMissingPrefix() {
		try {
			String fix = Fixer.fixMissingPrefixes(missingPrefix, Map.of("skos", SKOS.NAMESPACE));

			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix, "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}
}
