package swiss.sib.rdf.sparql.examples.fixes;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;

import swiss.sib.rdf.sparql.examples.Fixer.Fixed;
import swiss.sib.rdf.sparql.examples.tests.CreateTestWithRDF4jMethods;

/**
 * A class to fix federation metadata.
 */
public class Federation {
	private static final QueryParser PARSER = new SPARQLParserFactory().getParser();

	private Federation() {

	}
	/**
	 * A method to fix federation metadata in the query.
	 * @param prior a prior Fixed object.
	 * @param queryIri the query IRI.
	 * @return a Fixed object (either original or changed).
	 */
	public static Fixed fix(Fixed prior, String queryIri) {
		String original;
		if (prior.changed()) {
			original = prior.fixed();
		} else {
			original = prior.original();
		}
		try {
			Set<String> federationPartners = new HashSet<>();
			CreateTestWithRDF4jMethods.extractedServiceIRIsFromOneQuery(PARSER, federationPartners, original);

			return new Fixed(prior.changed(), prior.fixed(), prior.original(), federationPartners);
		} catch (IllegalArgumentException e) {
			System.out.println("Could not fix federation metadata in:" + queryIri);
			return prior;
		}
	}
}
