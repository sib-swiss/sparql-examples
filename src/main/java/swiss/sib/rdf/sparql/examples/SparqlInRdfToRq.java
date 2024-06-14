package swiss.sib.rdf.sparql.examples;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class SparqlInRdfToRq {
	private static final IRI KEYWORD = SimpleValueFactory.getInstance().createIRI("https://schema.org/keyword");
	

	public static List<String> asRq(Model ex) {
		List<String> rq = new ArrayList<>();

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct()
				.peek(s -> rq.add("#+ id:" + s.stringValue())).forEach(s -> {
					streamOf(ex, s, RDFS.COMMENT, null).map(Statement::getObject).map(Value::stringValue)
							.map(o -> "#+ summary:" + o.replaceAll("\n", " ").replaceAll("\r", "")).forEach(rq::add);
					rq.add("\n");
					String keywords = streamOf(ex, s, KEYWORD, null).map(Statement::getObject).map(Value::stringValue)
							.collect(Collectors.joining("\n#+  -"));
					if (!keywords.isEmpty()) {
						rq.add("#+ tags:");
						rq.add("#+   -");
						rq.add(keywords);
					}
					Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE)
							.flatMap(qt -> streamOf(ex, s, qt, null)).map(Statement::getObject)
							.map(o -> o.stringValue()).forEach(q -> addPrefixes(q, ex, rq));
				});
		return rq;
	}

	/**
     * Add prefixes to the raw SPARQL query string
	 * @param rq 
     **/
	public static void addPrefixes(String query, Model ex, List<String> rq) {
		Iterator<Statement> iterator = streamOf(ex, null, SHACL.PREFIX_PROP, null).iterator();
		List<String> prefixes = new ArrayList<>();
		
		while (iterator.hasNext()) {
			Statement n = iterator.next();
			Resource ns = n.getSubject();
			String nos = n.getObject().stringValue() + ':';
			
			if (queryContainsPrefix(query, nos)) {
				prefixes.add(streamOf(ex, ns, SHACL.NAMESPACE_PROP, null).map(Statement::getObject)
						.map(Value::stringValue).map(s -> "PREFIX "+nos+'<'+s+'>').collect(Collectors.joining()));
			}
		}
		prefixes.sort(String::compareTo);
		rq.addAll(prefixes);
		rq.add(query);
	}

	static boolean queryContainsPrefix(String query, String prefix) {
		int indexOf = query.indexOf(prefix);
		if (indexOf == 0) {
			return true;
		} else if (indexOf >= 0) {
			//Make sure that the prefix is complete to avoid matching p: when prefix is actually up:
			//so the character should be a tab, space, forward slash, closing bracket or pipe
			char cb = query.charAt(indexOf - 1);
			return cb == '\t' || cb == ' ' || cb == '/' || cb == ')' || cb == '|';
		}
		return false;
	}

	private static Stream<Statement> streamOf(Model ex, Resource s, IRI p, Value o) {
		return StreamSupport.stream(ex.getStatements(s, p, o).spliterator(), false);
	}
}
