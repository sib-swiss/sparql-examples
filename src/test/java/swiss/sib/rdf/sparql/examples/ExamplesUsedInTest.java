package swiss.sib.rdf.sparql.examples;

public class ExamplesUsedInTest {

	static String rhea9 = """
	prefix ex: <https://sparql.rhea-db.org/.well-known/sparql-examples/>
	prefix sh: <http://www.w3.org/ns/shacl#>
	prefix schema:<https://schema.org/>
	prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
	prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
	
	_:sparql_examples_prefixes sh:declare _:prefix_rhea .
	_:prefix_rhea
	  sh:prefix "rh" ;
	  sh:namespace "http://rdf.rhea-db.org/"^^xsd:anyURI .
	
	_:sparql_examples_prefixes sh:declare _:prefix_ec .
	_:prefix_ec 
	  sh:prefix "ec" ;
	  sh:namespace "http://purl.uniprot.org/enzyme/"^^xsd:anyURI .
	
	
	ex:9
	    a sh:SPARQLSelectExecutable, sh:SPARQLExecutable ;
	    sh:prefixes _:sparql_examples_prefixes ;
	    schema:target <https://sparql.rhea-db.org/sparql/> ;
	    rdfs:comment "Select all Rhea reactions mapped to enzyme classification (EC numbers)"^^rdf:html ;
	    sh:select '''# Query 9
	# Select all Rhea reactions mapped to EC numbers (enzyme classification)
	#
	# This query corresponds to the Rhea website query:
	# https://www.rhea-db.org/rhea?query=ec:*
	#
	SELECT ?ec ?ecNumber ?rhea ?accession ?equation
	WHERE {
	  ?rhea rdfs:subClassOf rh:Reaction .
	  ?rhea rh:accession ?accession .
	  ?rhea rh:ec ?ec .
	  BIND(strafter(str(?ec),str(ec:)) as ?ecNumber)
	  ?rhea rh:isTransport ?isTransport .
	  ?rhea rh:equation ?equation .
	}''' . """;
	static String rhea9Anon = """
	prefix ex: <https://sparql.rhea-db.org/.well-known/sparql-examples/>
	prefix sh: <http://www.w3.org/ns/shacl#>
	prefix schema:<https://schema.org/>
	prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
	prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
	
	_:sparql_examples_prefixes sh:declare _:prefix_rhea .
	_:prefix_rhea
	  sh:prefix "rh" ;
	  sh:namespace "http://rdf.rhea-db.org/"^^xsd:anyURI .
	
	_:sparql_examples_prefixes sh:declare _:prefix_ec .
	_:prefix_ec 
	  sh:prefix "ec" ;
	  sh:namespace "http://purl.uniprot.org/enzyme/"^^xsd:anyURI .
	
	
	ex:9
	    a sh:SPARQLSelectExecutable, sh:SPARQLExecutable ;
	    sh:prefixes _:sparql_examples_prefixes ;
	    schema:target <https://sparql.rhea-db.org/sparql/> ;
	    rdfs:comment "Select all Rhea reactions mapped to enzyme classification (EC numbers)"^^rdf:html ;
	    sh:select '''# Query 9
	# Select all Rhea reactions mapped to EC numbers (enzyme classification)
	#
	# This query corresponds to the Rhea website query:
	# https://www.rhea-db.org/rhea?query=ec:*
	#
	SELECT ?ec ?ecNumber ?rhea
	WHERE {
	  ?rhea rdfs:subClassOf rh:Reaction .
	  ?rhea rh:accession 'lala' .
	  ?rhea rh:ec ?ec .
	  BIND(strafter(str(?ec),str(ec:)) as ?ecNumber)
	  ?rhea rh:isTransport ?isTransport .
	  ?rhea rh:equation [] .
	  FILTER(?isTransport = true)
	}''' . """;
	static String simple = """
				prefix ex: <https://sparql.swisslipids.org/.well-known/sparql-examples/>
	prefix sh: <http://www.w3.org/ns/shacl#>
	prefix schema:<https://schema.org/>
	prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
	prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
	_:sparql_examples_prefixes sh:declare _:prefix_slm .
	_:prefix_slm
	sh:prefix "SWISSLIPID" ;
	sh:namespace "https://swisslipids.org/rdf/SLM_"^^xsd:anyURI .
	_:sparql_examples_prefixes sh:declare _:prefix_rdfs .
	_:prefix_rdfs
	sh:prefix "rdfs" ;
	sh:namespace "http://www.w3.org/2000/01/rdf-schema#"^^xsd:anyURI .
	
	ex:1
	a sh:SPARQLSelectExecutable, sh:SPARQLExecutable ;
	sh:prefixes _:sparql_examples_prefixes ;
	schema:target <https://sparql.swisslipids.org/sparql/> ;
	rdfs:comment "Select the SwissLipids categories and their labels."^^rdf:HTML ;
	sh:select '''# Example 1
	SELECT ?category ?label
	WHERE
	{
	?category SWISSLIPID:rank SWISSLIPID:Category .
	?category rdfs:label ?label .
	}''' . """;

}
