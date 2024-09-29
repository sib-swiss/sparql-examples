package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.junit.jupiter.api.Test;

import swiss.sib.rdf.sparql.examples.Fixer.Fixed;
import swiss.sib.rdf.sparql.examples.fixes.Blazegraph;
import swiss.sib.rdf.sparql.examples.fixes.Prefixes;

public class FixerTest {

	private static final Map<String, String> PREFIXES = Map.of("skos", SKOS.NAMESPACE,
			"wikibase", "http://wikiba.se/ontology#",
			"wdt", "http://www.wikidata.org/prop/direct/",
			"wd", "http://www.wikidata.org/entity/",
			"bd", "http://www.bigdata.com/rdf#>");

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

	private final String blazeGraphIncludeExample2 = """
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			SELECT ?item ?itemLabel WITH
			{
			  	SELECT DISTINCT ?itemLabel
			  	WHERE
			  	{
			    	?item rdfs:label ?itemLabel .
			    }
			} AS %get_labels
			WITH
			{
			  	SELECT DISTINCT ?itemType
			  	WHERE
			  	{
			    	?item rdfs:type ?itemType .
			    }
			} AS %get_labels2

			WHERE
			{
			  INCLUDE %get_labels
			  INCLUDE %get_labels2
			  ?item a rdfs:Class .
			}""";
	
	
	private final String includingInclude = """
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			SELECT ?item ?itemLabel WITH
			{
			  	SELECT DISTINCT ?itemLabel
			  	WHERE
			  	{
			    	?item rdfs:label ?itemLabel .
			    }
			} AS %get_labels
			WITH
			{
			  	SELECT DISTINCT ?itemType
			  	WHERE
			  	{
			  		INCLUDE %get_labels
			    	?item rdfs:type ?itemType .
			    }
			} AS %get_labels2

			WHERE
			{
			  INCLUDE %get_labels2
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

	private final String blazeGraphWithHintsAndInclude = """
			PREFIX wikibase: <http://wikiba.se/ontology#>
			PREFIX p: <http://www.wikidata.org/prop/>
			PREFIX bd: <http://www.bigdata.com/rdf#>
			SELECT ?property ?propertyLabel ?count WITH {
			SELECT ?property (COUNT(?ppp) AS ?count)  # use a named subquery first to do the count
			WHERE
			{
			  hint:Query hint:optimizer \"Runtime\" .
			  hint:Query hint:maxParallel 50 .       # use a big hammer
			  ?item p:P580 [?ppp [] ].               # P580 statement with a qualifier or reference
			  ?property wikibase:qualifier ?ppp.     # it's a qualifier & we get the property name
			        } GROUP BY ?property } AS %i     # group this subquery to get a count
			WHERE
			{
			  INCLUDE %i                             # get the label for the property here once we have a list of properties and counts
			  SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],es,en\". }
			} ORDER BY DESC(?count)""";

	private String failing = """
						PREFIX wikibase: <http://wikiba.se/ontology#>
			PREFIX wdt: <http://www.wikidata.org/prop/direct/>
			PREFIX wd: <http://www.wikidata.org/entity/>
			PREFIX bd: <http://www.bigdata.com/rdf#>
			SELECT DISTINCT ?itemLabel ?hasc
			WITH
			{
			  # Subquery to get all values of hasc in Germany
			  SELECT ?region ?hasc
			  WHERE
			  {
			    ?region wdt:P8119 ?hasc .
			    FILTER(REGEX(STR(?hasc), "^DE.[A-Z]{2}.[A-Z]{2}$","i"))
			    ?region wdt:P17 wd:Q183 . # country is Germany
			  }
			  #ORDER BY ?hasc
			  #OFFSET 0
			  #LIMIT 50
			} AS %hasc
			WHERE
			{
			  INCLUDE %hasc
			  ?item wdt:P131 * ?region .
			  #?item wdt:P31 / wdt:P279 * wd:Q486972 . # ?item is a subclass of human settlement
			  VALUES ?instance_of {
			    wd:Q253019      # Ortsteil
			    wd:Q486972      # Siedlung
			    wd:Q262166      # Gemeinde in Deutschland
			    wd:Q123705      # Stadtviertel
			  }
			  ?item wdt:P31 ?instance_of .
			  #hint:Prior hint:gearing "forward" .
			  SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE],en,de". }
			}
			""";

	private String failing2 = """
						PREFIX wdt: <http://www.wikidata.org/prop/direct/>
			PREFIX wd: <http://www.wikidata.org/entity/>
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			select distinct ?country (group_concat(?NatureLabelFr;separator=",") as ?NatureLabelFr) ?countryLabel  ?creationDate ?dissolutionDate
			with {
			select ?country (coalesce(?countryLabelFr, ?countryLabelEn,  ?country) as ?countryLabel) ?creationDate ?dissolutionDate{
			VALUES ?what {
			               wd:Q3624078 # sovereign states, I don’t know if it’s the right item
			               wd:Q3024240 # états historiques
			             }
			?country wdt:P31/wdt:P279* ?what .
			MINUS { ?country (wdt:P31/wdt:P279*) wd:Q1790360. } #empires coloniaux
			MINUS { ?country (wdt:P31/wdt:P279*) wd:Q1371288. } #états vassals
			MINUS { ?country (wdt:P31/wdt:P279*) wd:Q21512251. } #états autoproclamés
			MINUS { ?country (wdt:P31/wdt:P279*) wd:Q1642488. } #chefferies
			optional { ?country rdfs:label ?countryLabelFr filter(lang(?countryLabelFr)= "fr")} .
			optional { ?country rdfs:label ?countryLabelEn filter(lang(?countryLabelEn)= "en")}
			optional { ?country wdt:P571 ?creationDate }
			optional { ?country wdt:P576 ?dissolutionDate }
			} order by ?countryLabel
			} as %datas
			where {
			include %datas .
			optional{
			?country wdt:P31/rdfs:label ?NatureLabelFr filter(lang(?NatureLabelFr)= "fr") .
			}
			} group by ?country ?countryLabel  ?creationDate ?dissolutionDate
						""";
	
	private String failing3 = """
				PREFIX wikibase: <http://wikiba.se/ontology#>
				PREFIX wdt: <http://www.wikidata.org/prop/direct/>
				PREFIX wd: <http://www.wikidata.org/entity/>
				PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
				PREFIX bd: <http://www.bigdata.com/rdf#>
				SELECT ?item ?itemLabel ?aliases with {
				  select ?item (group_concat(distinct ?alias;separator=\", \") as ?aliases)
				  WHERE 
				  {
				    values ?occ {wd:Q482980 wd:Q36180}
				    ?item wdt:P106 ?occ.
				    ?item skos:altLabel ?alias. filter(lang(?alias)=\"en\")
				  } group by ?item } as %i
				where
				{
				  include %i
				  SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],en\". }
				}
			""";

	private String failing4 = """
				PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
				PREFIX wikibase: <http://wikiba.se/ontology#>
				PREFIX wdt: <http://www.wikidata.org/prop/direct/>
				PREFIX wd: <http://www.wikidata.org/entity/>
				PREFIX ps: <http://www.wikidata.org/prop/statement/>
				PREFIX prov: <http://www.w3.org/ns/prov#>
				PREFIX pr: <http://www.wikidata.org/prop/reference/>
				PREFIX p: <http://www.wikidata.org/prop/>
				PREFIX bd: <http://www.bigdata.com/rdf#>
				SELECT ?property ?propertyLabel ?cnt_s ?cnt_refs ?ratio WITH {
				  SELECT DISTINCT ?property ?s WHERE {
				    [] p:P106/ps:P106/wdt:P279* wd:Q26270618; ?p ?s .
				    ?property wikibase:claim ?p; wikibase:propertyType ?type .
				    FILTER(?type != wikibase:ExternalId) .
				  }
				} AS %subquery1 WHERE {
				   {
				  SELECT ?property (COUNT(DISTINCT ?s) AS ?cnt_s) (COUNT(DISTINCT ?dummy) as ?cnt_refs) WHERE {
				    INCLUDE %subquery1 .
				    OPTIONAL {
				      ?s prov:wasDerivedFrom ?refHandle .
				      ?refHandle pr:P143 ?val . # P143, P887, P4656, P3452, P5852
				    }
				    BIND(CONCAT(STR(?s), STR(?refHandle)) AS ?dummy) .
				  } GROUP BY ?property HAVING(?cnt_refs > 0)
				}.
				  BIND(CONCAT(STR(ROUND((?cnt_refs / ?cnt_s)*1000)/10), '%') AS ?ratio) .
				  SERVICE wikibase:label { bd:serviceParam wikibase:language 'en' }
				} ORDER BY DESC(?cnt_refs) DESC(xsd:integer(STRBEFORE(?ratio, '%')))
			""";
	
	@Test
	public void simpleIncludeWith() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraphIncludeWith(new Fixed(false, null, blazeGraphIncludeExample), "", null);
			assertTrue(fix.changed());
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}

	@Test
	public void doubleIncludeWith() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraphIncludeWith(new Fixed(false, null, blazeGraphIncludeExample2), "", null);
			assertTrue(fix.changed());
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}
	
	@Test
	public void innerIncludeWith() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraphIncludeWith(new Fixed(false, null, includingInclude), "", null);
			assertTrue(fix.changed());
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}

	@Test
	public void failingExample() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraphIncludeWith(new Fixed(false, null, failing), "", null);
			assertTrue(fix.changed());
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}
	

	@Test
	public void failingExample2() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraphIncludeWith(new Fixed(false, null, failing2), "", null);
			assertTrue(fix.changed());
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}
	
	@Test
	public void failingExample3() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraph(new Fixed(false, null, failing3), "", null);
			assertNotNull(fix);
			assertTrue(fix.changed());
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}
	
	@Test
	public void failingExample4() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraph(new Fixed(false, null, failing4), "", null);
			assertNotNull(fix);
			assertTrue(fix.changed());
			assertFalse(fix.fixed().contains("WITH"));
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}

	@Test
	public void real() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraph(new Fixed(false, null, blazeGraphWithoutIncludeExample), "", null);
			assertFalse(fix.changed());
			assertNull(fix.fixed());

			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(blazeGraphWithoutIncludeExample, "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}

	@Test
	public void hintsAndInclude() {
		try {
			Fixed fix = Blazegraph.fixBlazeGraphIncludeWith(new Fixed(false, null, blazeGraphWithHintsAndInclude), "", null);
			assertNotNull(fix);
			assertTrue(fix.changed());
			fix = Blazegraph.fixBlazeGraphHints(new Fixed(false, null, fix.fixed()), "", null);
			assertNotNull(fix);

			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(blazeGraphWithoutIncludeExample, "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}

	@Test
	public void simpleMissingPrefix() {
		try {
			Fixed fix = Prefixes.fixMissingPrefixes(missingPrefix, PREFIXES);
			assertTrue(fix.changed());
			QueryParser parser = new SPARQLParserFactory().getParser();
			parser.parseQuery(fix.fixed(), "http://example.org/");
		} catch (MalformedQueryException e) {
			fail(e);
		}
	}
}
