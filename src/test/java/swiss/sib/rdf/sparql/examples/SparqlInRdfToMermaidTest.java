package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;

public class SparqlInRdfToMermaidTest {

	String rhea9 = """
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
	String simple = """
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

	@Test
	public void simple() {
		Model model = parse(simple);
		assertTrue(SparqlInRdfToMermaid.asMermaid(model).contains("SWISSLIPID:"));
	}

	@Test
	public void rhea9() {
		Model model = parse(rhea9);
		assertTrue(SparqlInRdfToMermaid.asMermaid(model).contains("rh:ec"));
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
