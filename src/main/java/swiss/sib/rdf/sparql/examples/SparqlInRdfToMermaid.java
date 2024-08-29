package swiss.sib.rdf.sparql.examples;

import static swiss.sib.rdf.sparql.examples.SparqlInRdfToRq.queryContainsPrefix;
import static swiss.sib.rdf.sparql.examples.SparqlInRdfToRq.streamOf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;

import swiss.sib.rdf.sparql.examples.mermaid.FindWhichConstantsAreNotOnlyUsedAsPredicates;
import swiss.sib.rdf.sparql.examples.mermaid.NameVariablesAndConstants;
import swiss.sib.rdf.sparql.examples.mermaid.Render;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

public class SparqlInRdfToMermaid {


	/**
	 * {@link https://mermaid.js.org}
	 *
	 * @param ex the model containing all prefixes and query
	 * @return a a mermaid string
	 */
	public static String asMermaid(Model ex) {
		List<String> rq = new ArrayList<>();
		rq.add("graph TD");

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct().forEach(s -> {
			Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE).flatMap(qt -> streamOf(ex, s, qt, null))
					.forEach(q -> draw(q, ex, rq));
		});
		return rq.stream().collect(Collectors.joining("\n"));
	}

	/**
	 * Draw mermaid graph
	 **/
	public static void draw(Statement queryId, Model ex, List<String> rq) {
		String query = queryId.getObject().stringValue();

		String base = streamOf(ex, queryId.getSubject(), SchemaDotOrg.TARGET, null).map(Statement::getObject)
				.map(Value::stringValue).findFirst().orElse("https://example.org/");

		Iterator<Statement> iterator = streamOf(ex, null, SHACL.PREFIX_PROP, null).iterator();
		StringBuilder prefixes = new StringBuilder();
		Map<String, String> iriPrefixes = new TreeMap<>(
				Comparator.comparing(String::length).thenComparing(String::compareTo));
		gatherCompleteQuery(ex, query, iterator, prefixes, iriPrefixes);

		QueryParser parser = new SPARQLParserFactory().getParser();
		try {
			ParsedQuery pq = parser.parseQuery(query, base);
			TupleExpr tq = pq.getTupleExpr();

			Map<Value, String> constantKeys = new HashMap<>();
			Map<String, String> variableKeys = new HashMap<>();
			Map<String, String> anonymousKeys = new HashMap<>();
			Set<Value> usedAsNode = new HashSet<>();

			tq.visit(new NameVariablesAndConstants(constantKeys, variableKeys, anonymousKeys));
			tq.visit(new FindWhichConstantsAreNotOnlyUsedAsPredicates(usedAsNode));


			Render visitor = new Render(variableKeys, iriPrefixes, constantKeys, usedAsNode, anonymousKeys, rq, tq);
			visitor.addStyles();
			visitor.renderVariables();
			tq.visit(
					visitor);

		} catch (MalformedQueryException e) {
			String queryS = queryId.getSubject().stringValue();
			Failure.CANT_PARSE_EXAMPLE.exit(queryS, e);
		}
	}



	private static void gatherCompleteQuery(Model ex, String query, Iterator<Statement> iterator,
			StringBuilder prefixes, Map<String, String> iriPrefixes) {
		while (iterator.hasNext()) {
			Statement n = iterator.next();
			Resource ns = n.getSubject();
			String nos = n.getObject().stringValue() + ':';

			if (queryContainsPrefix(query, nos)) {
				streamOf(ex, ns, SHACL.NAMESPACE_PROP, null).map(Statement::getObject).map(Value::stringValue)
						.peek(s -> iriPrefixes.put(s, nos)).map(s -> "PREFIX " + nos + '<' + s + ">\n")
						.forEach(prefixes::append);
			}
		}
		prefixes.append(query);
	}

}
