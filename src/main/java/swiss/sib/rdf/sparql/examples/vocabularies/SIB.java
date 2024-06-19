package swiss.sib.rdf.sparql.examples.vocabularies;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class SIB {
	public static final IRI DESCRIBE = SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, "describe");

	private SIB() {
		
	}
}
