package swiss.sib.rdf.sparql.examples.mermaid;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

public final class Render extends AbstractQueryModelVisitor<RuntimeException> {
	private final class FindNodesUsedInFilter extends AbstractQueryModelVisitor<RuntimeException> {
		private final String filterId;

		private FindNodesUsedInFilter(String filterId) {
			this.filterId = filterId;
		}

		@Override
		public void meet(Var node) throws RuntimeException {
			rq.add(indent() + filterId + " .-> " + asString(node));
		}

		@Override
		public void meet(SameTerm node) throws RuntimeException {
			rq.add(indent() + filterId + "{{ sameTerm }}");
			super.meet(node);
		}

		@Override
		public void meet(Not node) throws RuntimeException {
			rq.add(indent() + filterId + "{{ Not }}");
			super.meet(node);
		}

		@Override
		public void meet(Exists node) throws RuntimeException {
			rq.add(indent() + "subgraph " + filterId + "{{ Exists }}");
			Map<Value, String> constantKeys = new HashMap<>();
			Map<String, String> variableKeys = new HashMap<>();
			Map<String, String> anonymousKeys = new HashMap<>();
			Set<Value> usedAsNode = new HashSet<>();

			node.visit(new NameVariablesAndConstants(constantKeys, variableKeys, anonymousKeys));
			node.visit(new FindWhichConstantsAreNotOnlyUsedAsPredicates(usedAsNode));
			node.visit(new Render(variableKeys, iriPrefixes, constantKeys, usedAsNode, anonymousKeys, rq,
					node.getSubQuery()));

		}

		@Override
		public void meet(Compare node) throws RuntimeException {
			switch (node.getOperator()) {
			case EQ -> rq.add(indent() + filterId + "{{ = }}");
			case NE -> rq.add(indent() + filterId + "{{ != }}");
			case LT -> rq.add(indent() + filterId + "{{ < }}");
			case GT -> rq.add(indent() + filterId + "{{ > }}");
			case LE -> rq.add(indent() + filterId + "{{ <= }}");
			case GE -> rq.add(indent() + filterId + "{{ >= }}");
			}
			super.meet(node);
		}
	}

	private final Map<String, String> variableKeys;
	private final Map<String, String> iriPrefixes;
	private final Map<Value, String> constantKeys;
	private final Set<Value> usedAsNode;
	private final Map<Value, String> serviceKeys = new HashMap<>();
	private final Map<String, String> anonymousKeys;
	private final List<String> rq;
	private int indent = 2;
	private int optionalCount = 0;
	private int unionCount = 0;
	private boolean optional;
	private int filterCount;

	public Render(Map<String, String> variableKeys, Map<String, String> iriPrefixes, Map<Value, String> constantKeys,
			Set<Value> usedAsNode, Map<String, String> anonymousKeys, List<String> rq, TupleExpr te) {
		this.variableKeys = variableKeys;
		this.iriPrefixes = iriPrefixes;
		this.constantKeys = constantKeys;
		this.usedAsNode = usedAsNode;
		this.anonymousKeys = anonymousKeys;
		this.rq = rq;

		FindProjectedVariables findProjectedVariables = new FindProjectedVariables();
		te.visit(findProjectedVariables);
		Set<Value> pv = findProjectedVariables.variables;
		Set<String> pnv = findProjectedVariables.namedVariables;
		addWithLeadingWhiteSpace(variableKeys.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
				.map(en -> en.getValue() + "(\"?" + en.getKey() + "\")" + addProjectedClass(en.getKey(), pnv)), rq);
		addWithLeadingWhiteSpace(
				anonymousKeys.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).map(
						en -> en.getValue() + "((\"_:" + en.getValue() + "\"))" + addProjectedClass(en.getKey(), pnv)),
				rq);
		addWithLeadingWhiteSpace(
				constantKeys.entrySet().stream().filter(en -> usedAsNode.contains(en.getKey())).map(en -> en.getValue()
						+ "([" + prefix(en.getKey(), iriPrefixes) + "])" + addProjectedClass(en.getKey(), pv)),
				rq);
		addStyles(rq);
	}

	private void addStyles(List<String> rq2) {
		rq2.add("classDef projected fill:lightgreen;");
	}

	private static void addWithLeadingWhiteSpace(Stream<String> map, List<String> rq) {
		map.map(s -> "  " + s).forEach(rq::add);
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
			rq.add(indent() + subj + " -->" + pred + "--> " + obj);
		}
		super.meet(node);
	}

	private String print(String subj, String obj, String pred) {
		String arrB = "--";
		String arrE = "-->";
		if (optional) {
			arrB = "-.";
			arrE = ".->";
			optional = false; // Afterwards they should be solid again
		}
		return indent() + subj + arrB + pred + arrE + " " + obj;
	}

	@Override
	public void meet(Filter f) throws RuntimeException {
		String filterId = "f" + filterCount++;

		FindNodesUsedInFilter visitor = new FindNodesUsedInFilter(filterId);
		f.getCondition().visit(visitor);
		super.meet(f);
	}

	@Override
	public void meet(Union u) throws RuntimeException {
		rq.add(indent() + "%%or");
		rq.add(indent() + "subgraph union" + unionCount + "l[\" \"]");
		indent += 2;
		rq.add(indent() + "style union" + unionCount + "l fill:#abf,stroke-dasharray: 3 3;");
		u.getRightArg().visit(this);
		indent -= 2;
		rq.add(indent() + "end");

		rq.add(indent() + "subgraph union" + unionCount + "r[\" \"]");
		indent += 2;
		rq.add(indent() + "style union" + unionCount + "r fill:#abf,stroke-dasharray: 3 3;");
		u.getLeftArg().visit(this);
		indent -= 2;
		rq.add(indent() + "end");
		rq.add(indent() + "union" + unionCount + "r <== or ==> union" + unionCount + "l");
		unionCount++;
	}

	@Override
	public void meet(LeftJoin s) throws RuntimeException {
		s.getLeftArg().visit(this);
		optional = true;
		rq.add(indent() + "%%optional");
		rq.add(indent() + "subgraph optional" + optionalCount + "[\"(optional)\"]");
		rq.add(indent() + "style optional" + optionalCount++ + " fill:#bbf,stroke-dasharray: 5 5;");
		indent += 2;
		s.getRightArg().visit(this);
		indent -= 2;
		rq.add(indent() + "end");
	}

	@Override
	public void meet(Service s) throws RuntimeException {
		Value sv = s.getServiceRef().getValue();
		String serviceIri = sv.stringValue();
		String key = serviceKeys.computeIfAbsent(sv, (t) -> "s" + (serviceKeys.size() + 1));
		rq.add(indent() + "subgraph " + key + "[\"" + serviceIri + "\"]");
		indent += 2;
		rq.add(indent() + "style " + key + " stroke-width:4px;");
		super.meet(s);
		indent -= 2;
		rq.add(indent() + "end");
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

	private static String prefix(Value v, Map<String, String> iriPrefixes) {
		if (v instanceof IRI i)
			return prefix(i, iriPrefixes);
		else if (v instanceof Literal l)
			return prefix(l, iriPrefixes);
		else
			return v.stringValue();
	}

	private static String prefix(String stringValue, Map<String, String> iriPrefixes) {
		for (Map.Entry<String, String> en : iriPrefixes.entrySet()) {
			if (stringValue.startsWith(en.getKey())) {
				return '"' + en.getValue() + stringValue.substring(en.getKey().length()) + '"';
			}
		}
		return stringValue;
	}

	private static String prefix(IRI stringValue, Map<String, String> iriPrefixes) {
		if (RDF.TYPE.equals(stringValue)) {
			return "\"a\"";
		} else {
			return prefix(stringValue.stringValue(), iriPrefixes);
		}
	}

	private static String prefix(Literal l, Map<String, String> iriPrefixes) {
		CoreDatatype cd = l.getCoreDatatype();
		if (cd.isXSDDatatype()) {
			if (CoreDatatype.XSD.STRING.equals(cd))
				return '"' + l.stringValue() + '"';
			else if (cd.isXSDDatatype())
				return '"' + l.stringValue() + "^^xsd:" + cd.getIri().getLocalName() + '"';
			else if (cd.isRDFDatatype())
				return '"' + l.stringValue() + "^^rdf:" + cd.getIri().getLocalName() + '"';
			else if (cd.isGEODatatype())
				return '"' + l.stringValue() + "^^geo:" + cd.getIri().getLocalName() + '"';
		}
		return '"' + l.stringValue() + "^^<" + l.getDatatype() + ">\"";
	}

	private static String addProjectedClass(Value v, Set<Value> variables) {
		if (variables.contains(v)) {
			return ":::projected ";
		}
		return "";
	}

	private static String addProjectedClass(String name, Set<String> variables) {
		if (variables.contains(name)) {
			return ":::projected ";
		}
		return "";
	}

	private class FindProjectedVariables extends AbstractQueryModelVisitor<RuntimeException> {

		private Set<String> namedVariables = new HashSet<>();
		private Set<Value> variables = new HashSet<>();

		@Override
		public void meet(Projection node) throws RuntimeException {
			ProjectionElemList projectionElemList = node.getProjectionElemList();
			for (ProjectionElem element : projectionElemList.getElements()) {
				if (element.getSourceExpression() != null) {
					ValueExpr expr = element.getSourceExpression().getExpr();
					expr.visit(this);
				} else {
					namedVariables.add(element.getName());
				}
			}
		}

		@Override
		public void meet(Var node) throws RuntimeException {
			super.meet(node);
			if (node.isConstant() && !node.isAnonymous()) {
				variables.add(node.getValue());
			} else {
				namedVariables.add(node.getName());
			}
		}
	}
}