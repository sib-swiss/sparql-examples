package swiss.sib.rdf.sparql.examples;

import static swiss.sib.rdf.sparql.examples.SparqlInRdfToRq.streamOf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

public class SparqlInRdfToMd {

	public static List<String> asMD(Model ex) {
		List<String> rq = new ArrayList<>();

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct().forEach(queryId -> {
			rq.add("# " + queryId.stringValue() + "\n");
			streamOf(ex, queryId, SchemaDotOrg.KEYWORD, null).map(Statement::getObject).map(Value::stringValue)
					.map(k -> " * " + k).forEach(rq::add);
			streamOf(ex, queryId, RDFS.COMMENT, null).map(Statement::getObject).map(Value::stringValue).forEach(rq::add);
			
			rq.add("");
			rq.add("## Use at ");
			streamOf(ex, queryId, SchemaDotOrg.TARGET, null).map(Statement::getObject).map(Value::stringValue).map(v -> " * " +v).forEach(rq::add);
			
			rq.add("");
			rq.add("```sparql");
			Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE).flatMap(qt -> streamOf(ex, queryId, qt, null))
					.map(Statement::getObject).map(o -> o.stringValue())
					.forEach(q -> SparqlInRdfToRq.addPrefixes(q, ex, rq));
			rq.add("```");

		});
		rq.add("");
		rq.add("```mermaid");
		rq.add(SparqlInRdfToMermaid.asMermaid(ex));
		rq.add("```");
		return rq;
	}
	
	public static List<String> asIndexMD(Model ex) {
		List<String> rq = new ArrayList<>();
		streamOf(ex, null, SIB.PROJECT, null).map(Statement::getObject).distinct()
				.forEach(o -> rq.add("# " + o.stringValue()));
		rq.add("");
		streamOf(ex, null, SIB.FILE_NAME, null).forEach(s -> {

			String fileName = s.getObject().stringValue();
			streamOf(ex, s.getSubject(), RDFS.COMMENT, null).map(Statement::getObject)
					.map(v -> " - [" + v.stringValue() + "](./" + fileName + ".md)").forEach(rq::add);
		});
		
		return rq;
	}
}
