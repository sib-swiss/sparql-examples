package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	
	private final String blazeGraphWithoutIncludeExample = """
			PREFIX wikibase: <http://wikiba.se/ontology#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
PREFIX wd: <http://www.wikidata.org/entity/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX bd: <http://www.bigdata.com/rdf#>
PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
SELECT
  ?place
  ?placeLabel
  ?dist
  (GROUP_CONCAT(DISTINCT ?instanceLabel; SEPARATOR = \", \") AS ?instanceLabels)
  (GROUP_CONCAT(DISTINCT ?adminLabel; SEPARATOR = \", \") AS ?adminLabels)
WHERE
{
  wd:Q116716361 wdt:P625 ?loc .
  SERVICE wikibase:around {
      ?place wdt:P625 ?location .
      bd:serviceParam wikibase:center ?loc .
      bd:serviceParam wikibase:radius \"6\" .
  }
  OPTIONAL { ?place wdt:P31 ?instance }
  OPTIONAL { ?place wdt:P131 ?admin }
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language \"en\" .
    ?instance rdfs:label ?instanceLabel .
    ?place rdfs:label ?placeLabel .
    ?admin rdfs:label ?adminLabel .
  }
  BIND(geof:distance(?loc, ?location) as ?dist)
}
GROUP BY ?place ?placeLabel ?dist
ORDER BY ?dist""";


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
	public void real() {
		try {
			String fix = Fixer.fixBlazeGraphIncludeWith(blazeGraphWithoutIncludeExample, "", null);
			assertNull(fix);

			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(blazeGraphWithoutIncludeExample, "http://example.org/");
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
