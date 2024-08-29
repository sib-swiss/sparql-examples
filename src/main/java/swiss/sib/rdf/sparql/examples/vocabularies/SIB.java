package swiss.sib.rdf.sparql.examples.vocabularies;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class SIB {
	public static final IRI DESCRIBE = SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, "describe");
	public static final IRI BIGDATA_SELECT = SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, "bigdata_select");
	public static final IRI BIGDATA_SPARQL_SELECT = SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, "BigDataSPARQLSelect");
	
	public static final IRI FILE_NAME = SimpleValueFactory.getInstance().createIRI("http://example.org/", "file_name");
	public static final IRI PROJECT = SimpleValueFactory.getInstance().createIRI("http://example.org/", "project");
	
	private SIB() {
		
	}
}
