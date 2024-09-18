package swiss.sib.rdf.sparql.examples.vocabularies;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class SIB {
	public static final String NAMESPACE = "http://example.org/";
	public static final IRI DESCRIBE = SimpleValueFactory.getInstance().createIRI(SHACL.NAMESPACE, "describe");
	public static final IRI BIGDATA_SELECT = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "bigdata_select");
	
	public static final IRI FILE_NAME = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "file_name");
	public static final IRI FILE_PATH = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "file_path");
	public static final IRI PROJECT = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "project");
	
	private SIB() {
		
	}
}
