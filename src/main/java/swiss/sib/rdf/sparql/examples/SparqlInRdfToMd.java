package swiss.sib.rdf.sparql.examples;

import static swiss.sib.rdf.sparql.examples.SparqlInRdfToRq.streamOf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

public class SparqlInRdfToMd {
	private static final Logger log = LoggerFactory.getLogger(SparqlInRdfToMd.class);
	public static List<String> asMD(Model ex) {
		List<String> rq = new ArrayList<>();

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct().forEach(queryId -> {
			rq.add("# " + ((IRI) queryId).getLocalName() + "\n");
			streamOf(ex, queryId, SchemaDotOrg.KEYWORD, null).map(Statement::getObject).map(Value::stringValue)
					.map(k -> " * " + k).forEach(rq::add);
			streamOf(ex, queryId, RDFS.COMMENT, null).map(Statement::getObject).map(Value::stringValue)
					.forEach(rq::add);

			rq.add("");
			rq.add("## Use at ");
			streamOf(ex, queryId, SchemaDotOrg.TARGET, null).map(Statement::getObject).map(Value::stringValue)
					.map(v -> " * " + v).forEach(rq::add);

			rq.add("");
			rq.add("```sparql");
			Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE)
					.flatMap(qt -> streamOf(ex, queryId, qt, null)).map(Statement::getObject).map(o -> o.stringValue())
					.forEach(q -> rq.add(q));
			rq.add("```");
			Iterator<Statement> iterator = streamOf(ex, queryId, DCTERMS.IS_PART_OF, null).iterator();
			if (iterator.hasNext()) {
				rq.add("## Query found at ");
				while(iterator.hasNext()) {
					String obj = iterator.next().getObject().stringValue();
					rq.add(" * ["+obj+"]("+obj+")");
				}
			}
			

		});
		rq.add("");
		try {
			String asMermaid = SparqlInRdfToMermaid.asMermaid(ex);
			rq.add("```mermaid");
			rq.add(asMermaid);
			rq.add("```");
		} catch (MalformedQueryException | IllegalArgumentException e) {
			log.debug("Could not convert to mermaid", e);
		}
		return rq;
	}

	public static List<String> asIndexMD(Model ex) {
		List<String> rq = new ArrayList<>();
		streamOf(ex, null, SIB.PROJECT, null).map(Statement::getObject).distinct()
				.forEach(o -> rq.add("# " + o.stringValue()));
		rq.add("");
		
		streamOf(ex, null, SIB.FILE_NAME, null).flatMap(s -> streamOf(ex, s.getSubject(), RDFS.COMMENT, null)
				.map(Statement::getObject).map(asNiceLink(s.getObject().stringValue()))).sorted().forEach(rq::add);
		return rq;
	}

	private static Function<Value, String> asNiceLink(String fileName) {
		return v -> {
			String comment = Jsoup.parse(v.stringValue()).text();

			String fileNameOfMdFile = fileName.substring(0, fileName.length() - 4) + ".md)";
			return " - [" + comment + "](./" + fileNameOfMdFile;
		};
	}
}
