package swiss.sib.rdf.sparql.examples.vocabularies;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class SchemaDotOrg {
	public static final IRI KEYWORD = SimpleValueFactory.getInstance().createIRI("https://schema.org/keyword");
	public static final IRI TARGET = SimpleValueFactory.getInstance().createIRI("https://schema.org/target");

	private SchemaDotOrg () {
		
	}
}
