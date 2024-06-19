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

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct().forEach(s -> {
			rq.add("# " + s.stringValue() + "\n");
			streamOf(ex, s, SchemaDotOrg.KEYWORD, null).map(Statement::getObject).map(Value::stringValue)
					.map(k -> " * " + k).forEach(rq::add);
			streamOf(ex, s, RDFS.COMMENT, null).map(Statement::getObject).map(Value::stringValue).forEach(rq::add);
			rq.add("\n");
			rq.add("```sparql");
			Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE).flatMap(qt -> streamOf(ex, s, qt, null))
					.map(Statement::getObject).map(o -> o.stringValue())
					.forEach(q -> SparqlInRdfToRq.addPrefixes(q, ex, rq));
			rq.add("```");

		});

		rq.add("```mermaid");
		rq.add(SparqlInRdfToMermaid.asMermaid(ex));
		rq.add("```");
		return rq;
	}
}
