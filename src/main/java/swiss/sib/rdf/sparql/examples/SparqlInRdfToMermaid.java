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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;



import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

public class SparqlInRdfToMermaid {
	private static final class Render extends AbstractQueryModelVisitor<RuntimeException> {
		private final Map<String, String> variableKeys;
		private final Map<String, String> iriPrefixes;
		private final Map<Value, String> constantKeys;
		private final Set<Value> usedAsNode;
		private final Map<String, String> anonymousKeys;
		private final List<String> rq;
		private int indent = 2;
		private boolean optional;
		
		private Render(Map<String, String> variableKeys, Map<String, String> iriPrefixes,
				Map<Value, String> constantKeys, Set<Value> usedAsNode, Map<String, String> anonymousKeys,
				List<String> rq) {
			this.variableKeys = variableKeys;
			this.iriPrefixes = iriPrefixes;
			this.constantKeys = constantKeys;
			this.usedAsNode = usedAsNode;
			this.anonymousKeys = anonymousKeys;
			this.rq = rq;
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			String subj = asString(node.getSubjectVar());
			String obj = asString(node.getObjectVar());
			if (node.getPredicateVar().isConstant() && !usedAsNode.contains(node.getPredicateVar().getValue())) {
				String pred = prefix(node.getPredicateVar().getValue(), iriPrefixes);
				rq.add(print(subj, obj, pred));
			} else {
				String pred = asString(node.getPredicateVar());
				rq.add(indent() + subj + " -->" + pred + "--> "
						+ obj);
			}
			super.meet(node);
		}

		private String print(String subj, String obj, String pred) {
			String arrB = "--";
			String arrE = "-->";
			if (optional) {
				arrB = "-.";
				arrE = ".->";
				optional=false; // Afterwards they should be solid again
			}
			return indent() + subj + arrB + pred + arrE +" "
					+ obj;
		}
		
		@Override
		public void meet(LeftJoin s) throws RuntimeException {
			s.getLeftArg().visit(this);
			optional=true;
			rq.add(indent()+"%%optional");
			indent+=2;
			s.getRightArg().visit(this);
			indent-=2;
		}

		@Override
		public void meet(Service s) throws RuntimeException {
			rq.add(indent()+"subgraph \"" + s.getServiceRef().getValue().stringValue()+'"');
			indent+=2;
			rq.add(indent()+"direction LR");
			super.meet(s);
			indent-=2;
			rq.add(indent()+"end");
		}

		private String indent() {
			return IntStream.range(0, indent).mapToObj(i -> " ").collect(Collectors.joining());
		}

		private String asString(Var v) {
			if (v.isAnonymous() && !v.isConstant()) {
				return anonymousKeys.get(v.getName());
			} else if (v.isConstant()) {
				return constantKeys.get(v.getValue());
			} else {
				return variableKeys.get(v.getName());
			}
		}
	}

	private static final class FindWhichConstantsAreNotOnlyUsedAsPredicates
			extends AbstractQueryModelVisitor<RuntimeException> {
		private final Set<Value> usedAsNode;

		private FindWhichConstantsAreNotOnlyUsedAsPredicates(Set<Value> usedAsNode) {
			this.usedAsNode = usedAsNode;
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			if (node.getPredicateVar().isConstant()) {
				markAsUsed(node.getSubjectVar(), node.getObjectVar());
			} else {
				markAsUsed(node.getSubjectVar(), node.getPredicateVar(), node.getObjectVar());
			}
			super.meet(node);
		}

		private void markAsUsed(Var... vars) {
			for (Var v : vars) {
				usedAsNode.add(v.getValue());
			}
		}
	}

	public static final class NameVariablesAndConstants extends AbstractQueryModelVisitor<RuntimeException> {
		private final Map<Value, String> constantKeys;
		private final Map<String, String> variableKeys;
		private final Map<String, String> anonymousKeys;

		public NameVariablesAndConstants(Map<Value, String> constantKeys, Map<String, String> variableKeys,
				Map<String, String> anonymousKeys) {
			this.constantKeys = constantKeys;
			this.variableKeys = variableKeys;
			this.anonymousKeys = anonymousKeys;
		}

		@Override
		public void meet(Var node) throws RuntimeException {
			super.meet(node);
			if (node.isAnonymous() && !node.isConstant()) {
				if (!anonymousKeys.containsKey(node.getName())) {
					String nextId = "a" + (anonymousKeys.size() + 1);
					anonymousKeys.put(node.getName(), nextId);
				}
			} else if (!node.isConstant() && !variableKeys.containsKey(node.getName())) {
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
				return '"' + en.getValue() + stringValue.substring(en.getKey().length()) + '"';
			}
		}
		return stringValue;
	}

	private static String prefix(Value v, Map<String, String> iriPrefixes) {
		if (v instanceof IRI i)
			return prefix(i, iriPrefixes);
		else if (v instanceof Literal l)
			return prefix(l, iriPrefixes);
		else
			return v.stringValue();
	}

	private static String prefix(IRI stringValue, Map<String, String> iriPrefixes) {
		return prefix(stringValue.stringValue(), iriPrefixes);
	}

	private static String prefix(Literal l, Map<String, String> iriPrefixes) {
		CoreDatatype cd = l.getCoreDatatype();
		if (cd.isXSDDatatype()) {
			if (CoreDatatype.XSD.STRING.equals(cd))
				return '"' + l.stringValue() + '"';
			else if (cd.isXSDDatatype())
				return '"' + l.stringValue() + "\"^^xsd:" + cd.getIri().getLocalName();
			else if (cd.isRDFDatatype())
				return '"' + l.stringValue() + "\"^^rdf:" + cd.getIri().getLocalName();
			else if (cd.isGEODatatype())
				return '"' + l.stringValue() + "\"^^geo:" + cd.getIri().getLocalName();
		}
		return '"' + l.stringValue() + "\"^^<" + l.getDatatype() + ">";
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
			TupleExpr tq = pq.getTupleExpr();

			Map<Value, String> constantKeys = new HashMap<>();
			Map<String, String> variableKeys = new HashMap<>();
			Map<String, String> anonymousKeys = new HashMap<>();
			Set<Value> usedAsNode = new HashSet<>();

			tq.visit(new NameVariablesAndConstants(constantKeys, variableKeys, anonymousKeys));
			tq.visit(new FindWhichConstantsAreNotOnlyUsedAsPredicates(usedAsNode));

			addWithLeadingWhiteSpace(variableKeys.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
					.map(en -> en.getValue() + "(\"?" + en.getKey() + "\")"), rq);
			addWithLeadingWhiteSpace(anonymousKeys.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
					.map(en -> en.getValue() + "((\"_:" + en.getValue() + "\"))"), rq);
			addWithLeadingWhiteSpace(constantKeys.entrySet().stream().filter(en -> usedAsNode.contains(en.getKey()))
					.map(en -> en.getValue() + "([" + prefix(en.getKey(), iriPrefixes) + "])"), rq);
			tq.visit(
					new Render(variableKeys, iriPrefixes, constantKeys, usedAsNode, anonymousKeys, rq));

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
