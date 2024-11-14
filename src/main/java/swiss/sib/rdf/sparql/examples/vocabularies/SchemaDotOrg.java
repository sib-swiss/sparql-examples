package swiss.sib.rdf.sparql.examples.vocabularies;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class SchemaDotOrg {
	public static final Namespace NS = new SimpleNamespace("schema", "https://schema.org/");
	public static final IRI KEYWORDS = SimpleValueFactory.getInstance().createIRI("https://schema.org/keywords");
	public static final IRI TARGET = SimpleValueFactory.getInstance().createIRI("https://schema.org/target");

	private SchemaDotOrg() {

	}
}
