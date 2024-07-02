package swiss.sib.rdf.sparql.examples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SparqlInRdfToRqTest {

	@Test
	public void prefixInQuery() {
		String q ="""
				PREFIX up: <http://purl.uniprot.org/core/>
				SELECT ?taxon
				FROM <http://sparql.uniprot.org/taxonomy>
				WHERE
				{
				    ?taxon a up:Taxon .
				}""";
		assertTrue(SparqlInRdfToRq.queryContainsPrefix(q, "up:"), "up: (uniprot) is in the query");
		assertFalse(SparqlInRdfToRq.queryContainsPrefix(q, "p:"), "p: (wikidata) not in the query");
	}
}
