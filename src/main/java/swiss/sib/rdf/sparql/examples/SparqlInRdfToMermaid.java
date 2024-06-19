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
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;

import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

public class SparqlInRdfToMermaid {
	public static final class NameVariablesAndConstants extends AbstractQueryModelVisitor<RuntimeException> {
		private final Map<Value, String> constantKeys;
		private final Map<String, String> variableKeys;

		public NameVariablesAndConstants(Map<Value, String> constantKeys, Map<String, String> variableKeys) {
			this.constantKeys = constantKeys;
			this.variableKeys = variableKeys;
		}

		@Override
		public void meet(Var node) throws RuntimeException {
			super.meet(node);
			if (!node.isConstant() && !variableKeys.containsKey(node.getName())) {
				String nextId = "v" + (variableKeys.size() + 1);
				variableKeys.put(node.getName(), nextId);

			} else if (node.isConstant() && !constantKeys.containsKey(node.getValue())) {
				String nextId = "c" + (constantKeys.size() + 1);
				constantKeys.put(node.getValue(), nextId);
			}
		}

	}

	private static String prefix(String stringValue, Map<String, String> iriPrefixes) {
		for (Map.Entry<String, String> en : iriPrefixes.entrySet()) {
			if (stringValue.startsWith(en.getKey())) {
				return en.getValue() + stringValue.substring(en.getKey().length());
			}
		}
		return stringValue;
	}

	/**
	 * {@link https://grlc.io/}
	 * 
	 * @param ex the model containing all prefixes and query
	 * @return a rq formatted list of strings that should be concatenated later.
	 */
	public static String asMermaid(Model ex) {
		List<String> rq = new ArrayList<>();

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct().forEach(s -> {
			rq.add("graph TD");
			Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE).flatMap(qt -> streamOf(ex, s, qt, null))
					.forEach(q -> addPrefixes(q, ex, rq));
		});
		return rq.stream().collect(Collectors.joining("\n"));
	}

	/**
	 * Add prefixes to the raw SPARQL query string
	 * 
	 * @param rq
	 **/
	public static void addPrefixes(Statement queryId, Model ex, List<String> rq) {
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
			ParsedQuery pq = parser.parseQuery(prefixes.toString(), base);

			Map<Value, String> constantKeys = new HashMap<>();
			Map<String, String> variableKeys = new HashMap<>();
			pq.getTupleExpr().visit(new NameVariablesAndConstants(constantKeys, variableKeys));

			List<String> connected = new ArrayList<>();
			Set<Value> usedAsNode = new HashSet<>();
			pq.getTupleExpr().visit(new AbstractQueryModelVisitor<RuntimeException>() {

				@Override
				public void meet(StatementPattern node) throws RuntimeException {
					if (node.getPredicateVar().isConstant()) {
						String predicateAsString = prefix(node.getPredicateVar().getValue().stringValue(), iriPrefixes);
						connected.add(asString(node.getSubjectVar()) + " --" + predicateAsString + "--> "
								+ asString(node.getObjectVar()));
						markAsUsed(node.getSubjectVar(), node.getObjectVar());
					} else {
						rq.add(asString(node.getSubjectVar()) + " -->" + asString(node.getPredicateVar()) + "--> "
								+ asString(node.getObjectVar()));
						markAsUsed(node.getSubjectVar(), node.getPredicateVar(), node.getObjectVar());
					}
					super.meet(node);
				}

				private void markAsUsed(Var... vars) {
					for (Var v : vars) {
						usedAsNode.add(v.getValue());
					}
				}

				private String asString(Var v) {
					if (v.isConstant()) {
						return constantKeys.get(v.getValue());
					} else {
						return variableKeys.get(v.getName());
					}
				}
			});
			addWithLeadingWhiteSpace(variableKeys.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
					.map(en -> en.getValue() + "(\"?" + en.getKey() + "\")"), rq);
			addWithLeadingWhiteSpace(constantKeys.entrySet().stream().filter(en -> usedAsNode.contains(en.getKey()))
					.map(en -> en.getValue() + "([" + prefix(en.getKey().stringValue(), iriPrefixes) + "])"), rq);
			addWithLeadingWhiteSpace(connected.stream(), rq);
		} catch (MalformedQueryException e) {
			String queryS = queryId.getSubject().stringValue();
			Converter.Failure.CANT_PARSE_EXAMPLE.exit(queryS, e);
		}
	}

	private static void addWithLeadingWhiteSpace(Stream<String> map, List<String> rq) {
		map.map(s -> "  " + s).forEach(rq::add);
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
