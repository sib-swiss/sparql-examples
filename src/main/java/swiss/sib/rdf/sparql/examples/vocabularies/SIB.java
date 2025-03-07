package swiss.sib.rdf.sparql.examples.vocabularies;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.AbstractNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;


public class SIB {
	public static final String NAMESPACE = "https://purl.expasy.org/sparql-examples/ontology#";
	public static final String PREFIX = "spex";
	public static final IRI DESCRIBE = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "describe");
	public static final IRI SPARQL_DESCRIBE_EXECUTABLE = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "SPARQLDescribeExecutable");
	public static final IRI BIGDATA_QUERY = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "bigdata_query");
	
	public static final IRI FILE_NAME = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "file_name");
	public static final IRI FILE_PATH = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "file_path");
	public static final IRI PROJECT = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "project");
	
	public static final IRI FEDERATES_WITH = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "federatesWith");
	public static final Namespace NS = new AbstractNamespace() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public String getName() {
			return NAMESPACE;
		}
		
	};
	
	private SIB() {
		
	}
}
