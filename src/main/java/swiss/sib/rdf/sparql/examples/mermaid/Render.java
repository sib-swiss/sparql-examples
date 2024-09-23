package swiss.sib.rdf.sparql.examples.mermaid;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

public final class Render extends AbstractQueryModelVisitor<RuntimeException> {
	private int existCount = 0;

	private final class FindNodesUsedInFilterOrBind extends AbstractQueryModelVisitor<RuntimeException> {
		private final String parentId;
		private final String prefix;

		private FindNodesUsedInFilterOrBind(String parentId, String prefix) {
			this.parentId = parentId;
			this.prefix = prefix;
		}

		@Override
		public void meet(Exists node) throws RuntimeException {
			int id = existCount++;
			String existId = prefix + "e" + id;
			rq.add(indent() + "subgraph " + parentId + existId + "[\"Exists Clause\"]");
			indent += 2;
			Map<Value, String> constantKeys = new HashMap<>();
			Map<String, String> variableKeys = new HashMap<>();
			Map<String, String> anonymousKeys = new HashMap<>();
			Set<Value> usedAsNode = new HashSet<>();

			node.visit(new NameVariablesAndConstants(constantKeys, variableKeys, anonymousKeys, existId));
			node.visit(new FindWhichConstantsAreNotOnlyUsedAsPredicates(usedAsNode));
			Render visitor = new Render(variableKeys, iriPrefixes, constantKeys, usedAsNode, anonymousKeys, rq,
					node.getSubQuery(), existId);
			visitor.indent = indent;
			node.visit(visitor);
			visitor.renderVariables();
			indent -= 2;
			rq.add(indent() + "end");
			rq.add(indent() + parentId + "--EXISTS--> " + parentId + existId);
		}
	}

	private final Map<String, String> variableKeys;
	private final Map<String, String> iriPrefixes;
	private final Map<Value, String> constantKeys;
	private final Set<Value> usedAsNode;
	private final Map<Value, String> serviceKeys = new HashMap<>();
	private final Map<String, String> anonymousKeys;
	private final List<String> rq;
	private final String prefix;
	private int indent = 2;
	private int optionalCount = 0;
	private int unionCount = 0;
	private boolean optional;
	private int filterCount;
	private final TupleExpr te;

	public Render(Map<String, String> variableKeys, Map<String, String> iriPrefixes, Map<Value, String> constantKeys,
			Set<Value> usedAsNode, Map<String, String> anonymousKeys, List<String> rq, TupleExpr te) {
		this(variableKeys, iriPrefixes, constantKeys, usedAsNode, anonymousKeys, rq, te, "");
	}

	public Render(Map<String, String> variableKeys, Map<String, String> iriPrefixes, Map<Value, String> constantKeys,
			Set<Value> usedAsNode, Map<String, String> anonymousKeys, List<String> rq, TupleExpr te, String prefix) {
		this.variableKeys = variableKeys;
		this.iriPrefixes = iriPrefixes;
		this.constantKeys = constantKeys;
		this.usedAsNode = usedAsNode;
		this.anonymousKeys = anonymousKeys;
		this.rq = rq;
		this.te = te;
		this.prefix = prefix;
	}

	public void renderVariables() {
		FindProjectedVariables findProjectedVariables = new FindProjectedVariables();
		te.visit(findProjectedVariables);
		Set<Value> pv = findProjectedVariables.variables;
		Set<String> pnv = findProjectedVariables.namedVariables;
		addWithLeadingWhiteSpace(variableKeys.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
				.map(en -> en.getValue() + "(\"?" + en.getKey() + "\")" + addProjectedClass(en.getKey(), pnv)), rq);
		addWithLeadingWhiteSpace(anonymousKeys.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
				.map(en -> en.getValue() + "((\" \"))" + addProjectedClass(en.getKey(), pnv)), rq);
		addWithLeadingWhiteSpace(
				constantKeys.entrySet().stream().filter(en -> usedAsNode.contains(en.getKey())).map(en -> en.getValue()
						+ "([" + prefix(en.getKey(), iriPrefixes, '"') + "])" + addProjectedClass(en.getKey(), pv)),
				rq);
	}

	public void addStyles() {
		rq.add("classDef projected fill:lightgreen;");
	}

	private void addWithLeadingWhiteSpace(Stream<String> map, List<String> rq) {
		map.map(s -> indent() + s).forEach(rq::add);
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		String subj = asString(node.getSubjectVar());
		String obj = asString(node.getObjectVar());
		if (node.getPredicateVar().isConstant() && !usedAsNode.contains(node.getPredicateVar().getValue())) {
			String pred = prefix(node.getPredicateVar().getValue(), iriPrefixes, '"');
			rq.add(print(subj, obj, pred));
		} else {
			String pred = asString(node.getPredicateVar());
			rq.add(indent() + subj + " -->" + pred + "--> " + obj);
		}
		super.meet(node);
	}

	private String print(String subj, String obj, String pred) {
		String arrB = " --";
		String arrE = "--> ";
		if (optional) {
			arrB = " -.";
			arrE = ".-> ";
			optional = false; // Afterwards they should be solid again
		}
		return indent() + subj + arrB + pred + arrE + " " + obj;
	}

	@Override
	public void meet(Filter f) throws RuntimeException {
		String filterId = prefix + "f" + filterCount++;
		ValueExprAsString visitor2 = new ValueExprAsString(filterId);
		f.getCondition().visit(visitor2);

		rq.add(indent() + filterId + "[[\"" + visitor2.sb.toString() + "\"]]");
		f.getCondition().visit(new FindNodesUsedInFilterOrBind(filterId, prefix));
		f.getCondition().visit(new FindValues(filterId, "-->", false));
		super.meet(f);
	}

	@Override
	public void meet(BindingSetAssignment b) throws RuntimeException {
		String bindId = "bind" + filterCount++;
		int values=0;
		rq.add(indent() + bindId + "[/VALUES "
				+ b.getBindingNames().stream().sorted().map(s -> "?" + s).collect(Collectors.joining(" ")) + "/]");
		for (String bn : b.getBindingNames())
		{
			rq.add(indent() + bindId + "-->" + variableKeys.get(bn));
		}
		for (BindingSet bs : b.getBindingSets()) {
			for (String bn : b.getBindingNames()) {
				int valueId=values++;
				rq.add(indent() + bindId + valueId + "([" + prefix(bs.getValue(bn), iriPrefixes, '"') + "])");
				rq.add(indent() + bindId + valueId + " --> " + bindId);
			}
		}
		
//		b.getBindingNames();
//		rq.add(indent() + bindId + "-->" + );

		super.meet(b);
	}

	@Override
	public void meet(ExtensionElem node) throws RuntimeException {
		super.meet(node);
		// TODO Auto-generated method stub
		String bindId = prefix + "bind" + filterCount++;
		{
			ValueExprAsString visitor = new ValueExprAsString(bindId);
			node.visitChildren(visitor);
			rq.add(indent() + bindId + "[/\"" + visitor.sb.toString() + "\"/]");
		}
		node.visitChildren(new FindNodesUsedInFilterOrBind(bindId, prefix));
		node.visitChildren(new FindValues(bindId, "--o", true));
//		for (Var v : visitor.variables) {
//			rq.add(indent() + asString(v) + " --o " + bindId);
//		}
		if (node.getName() != null && variableKeys.containsKey(node.getName())) {
			rq.add(indent() + bindId + " --as--o " + variableKeys.get(node.getName()));
		}
	}

	@Override
	public void meet(Union u) throws RuntimeException {
//		rq.add(indent() + "%%or");
		String thisUnionId = prefix + unionCount++;
		rq.add(indent() + "subgraph union" + thisUnionId + "[\" Union \"]");
		rq.add(indent() + "subgraph union" + thisUnionId + "l[\" \"]");
		indent += 2;
		rq.add(indent() + "style union" + thisUnionId + "l fill:#abf,stroke-dasharray: 3 3;");
		u.getRightArg().visit(this);
		indent -= 2;
		rq.add(indent() + "end");

		rq.add(indent() + "subgraph union" + thisUnionId + "r[\" \"]");
		indent += 2;
		rq.add(indent() + "style union" + thisUnionId + "r fill:#abf,stroke-dasharray: 3 3;");
		u.getLeftArg().visit(this);
		indent -= 2;
		rq.add(indent() + "end");
		rq.add(indent() + "union" + thisUnionId + "r <== or ==> union" + thisUnionId + "l");
		rq.add(indent() + "end");

	}

	@Override
	public void meet(LeftJoin s) throws RuntimeException {
		s.getLeftArg().visit(this);
		optional = true;
		String optionalId = prefix + optionalCount++;
//		rq.add(indent() + "%%optional");
		rq.add(indent() + "subgraph optional" + optionalId + "[\"(optional)\"]");
		rq.add(indent() + "style optional" + optionalId + " fill:#bbf,stroke-dasharray: 5 5;");
		indent += 2;
		s.getRightArg().visit(this);
		indent -= 2;
		rq.add(indent() + "end");
	}

	@Override
	public void meet(Service s) throws RuntimeException {
		Value sv = s.getServiceRef().getValue();
		String serviceIri;
		if (sv !=null) {
			serviceIri = sv.stringValue();
		} else {
			serviceIri = "ERROR: Bad value";;
		}
		String key = serviceKeys.computeIfAbsent(sv, (t) -> prefix + "s" + (serviceKeys.size() + 1));
		rq.add(indent() + "subgraph " + key + "[\"" + serviceIri + "\"]");
		indent += 2;
		rq.add(indent() + "style " + key + " stroke-width:4px;");
		super.meet(s);
		indent -= 2;
		rq.add(indent() + "end");
	}

	@Override
	public void meet(Difference s) throws RuntimeException {

		s.getLeftArg().visit(this);
		String key = "minus" + filterCount++;
		rq.add(indent() + "subgraph " + key + "[\"MINUS\"]");
		indent += 2;
		rq.add(indent() + "style " + key + " stroke-width:6px,fill:pink,stroke:red;");
		s.getRightArg().visit(this);
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

	private static String prefix(Value v, Map<String, String> iriPrefixes, char s) {
		if (v instanceof IRI i)
			return prefix(i, iriPrefixes, s);
		else if (v instanceof Literal l)
			return prefix(l, iriPrefixes, s);
		else if (v != null)
			return v.stringValue();
		else
			return "ERROR: Bad value";
	}

	private static String prefix(String stringValue, Map<String, String> iriPrefixes, char s) {
		for (Map.Entry<String, String> en : iriPrefixes.entrySet()) {
			if (stringValue.startsWith(en.getKey())) {
				return s + en.getValue() + stringValue.substring(en.getKey().length()) + s;
			}
		}
		return stringValue;
	}

	private static String prefix(IRI stringValue, Map<String, String> iriPrefixes, char s) {
		if (RDF.TYPE.equals(stringValue)) {
			return "\"a\"";
		} else {
			return prefix(stringValue.stringValue(), iriPrefixes, s);
		}
	}

	private static final Pattern LEFT_BRACKET = Pattern.compile("[", Pattern.LITERAL);
	private static final Pattern RIGHT_BRACKET = Pattern.compile("]", Pattern.LITERAL);

	private static String escape(String in) {
		String nol = LEFT_BRACKET.matcher(in).replaceAll("#91;");
		String norl = RIGHT_BRACKET.matcher(nol).replaceAll("#93;");
		return norl;
	}

	@Override
	public void meet(GroupElem node) throws RuntimeException {
		String groupId = prefix + "g" + filterCount++;

		node.getOperator().visit(this);

	}

	private static String prefix(Literal l, Map<String, String> iriPrefixes, char s) {
		CoreDatatype cd = l.getCoreDatatype();
		if (cd.isXSDDatatype()) {
			if (CoreDatatype.XSD.STRING.equals(cd))
				return s + escape(l.stringValue()) + s;
			else if (cd.isXSDDatatype())
				return s + l.stringValue() + "^^xsd:" + cd.getIri().getLocalName() + s;
			else if (cd.isRDFDatatype())
				return s + l.stringValue() + "^^rdf:" + cd.getIri().getLocalName() + s;
			else if (cd.isGEODatatype())
				return s + l.stringValue() + "^^geo:" + cd.getIri().getLocalName() + s;
		}
		return 's' + escape(l.stringValue()) + "^^<" + l.getDatatype() + ">" + s;
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

	private class FindValues extends AbstractQueryModelVisitor<RuntimeException> {

		private final Set<Var> variables = new HashSet<>();
		private final String parentId;
		private final boolean forward;
		private final String arrow;

		public FindValues(String parentId, String arrow, boolean forward) {
			super();
			this.parentId = parentId;
			this.arrow = arrow;
			this.forward = forward;
		}

		@Override
		public void meet(Var node) throws RuntimeException {
			super.meet(node);
			if (variables.add(node)) {
				if (forward) {
					rq.add(indent() + asString(node) + " " + arrow + " " + parentId);
				} else {
					rq.add(indent() + parentId + " " + arrow + " " + asString(node));
				}
			}
		}
	}

	private class ValueExprAsString extends AbstractQueryModelVisitor<RuntimeException> {
		
		private final String parentId;

		public ValueExprAsString(String bindId) {
			this.parentId = bindId;
		}

		@Override
		public void meet(IsBNode node) throws RuntimeException {
			sb.append("isBlank(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(IsLiteral node) throws RuntimeException {
			sb.append("isLiteral(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(IsNumeric node) throws RuntimeException {
			sb.append("isNumeric(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(IsResource node) throws RuntimeException {
			sb.append("isIRI(");
			node.getArg().visit(this);
			sb.append(") || isBlank(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(IsURI node) throws RuntimeException {
			sb.append("isIRI(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(LangMatches node) throws RuntimeException {
			sb.append("langmatch(");
			node.getLeftArg().visit(this);
			sb.append(",");
			node.getRightArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(LocalName node) throws RuntimeException {
			// TODO Auto-generated method stub
			super.meet(node);
		}

		StringBuilder sb = new StringBuilder();

		@Override
		public void meet(Avg node) throws RuntimeException {
			sb.append("average(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Sample node) throws RuntimeException {
			sb.append("sample(");
			node.getArg().visit(this);
			sb.append(")");
		}
		
		@Override
		public void meet(Max node) throws RuntimeException {
			sb.append("max(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Min node) throws RuntimeException {
			sb.append("min(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Sum node) throws RuntimeException {
			sb.append("sum(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Var node) throws RuntimeException {
			if (!node.isAnonymous() && !node.isConstant()) {
				sb.append("?" + node.getName());
			} else if (node.isAnonymous() && node.isConstant()) {
				sb.append(" [] ");
			} else if (node.isConstant()) {
				prefix(node.getValue(), iriPrefixes, '\'');
			}
		}

		@Override
		public void meet(SameTerm node) throws RuntimeException {
			sb.append("sameterm(");
			node.getLeftArg().visit(this);
			sb.append(",");
			node.getRightArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Not node) throws RuntimeException {
			sb.append("not ");
			node.getArg().visit(this);
		}

		@Override
		public void meet(Bound node) throws RuntimeException {
			sb.append("bound(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Exists node) throws RuntimeException {
			sb.append(" ");
		}

		@Override
		public void meet(Compare node) throws RuntimeException {

			node.getLeftArg().visit(this);
			switch (node.getOperator()) {
			case EQ -> sb.append(" = ");
			case NE -> sb.append(" != ");
			case LT -> sb.append(" < ");
			case GT -> sb.append(" > ");
			case LE -> sb.append(" <= ");
			case GE -> sb.append(" >= ");
			}
			node.getRightArg().visit(this);
		}

		@Override
		public void meet(Str node) throws RuntimeException {
			sb.append("str(");
			node.getArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(If node) throws RuntimeException {
			sb.append("if(");
			node.getCondition().visit(this);
			sb.append(',');
			node.getResult().visit(this);
			sb.append(',');
			node.getAlternative().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Count node) throws RuntimeException {
			sb.append("count(");
			if (node.getArg() != null) {
				node.getArg().visit(this);
			} else {
				sb.append("*");
			}
			sb.append(")");
		}

		@Override
		public void meet(Or node) throws RuntimeException {
			sb.append("(");
			node.getLeftArg().visit(this);
			sb.append(" || ");
			node.getRightArg().visit(this);
			sb.append(")");
		}

		@Override
		public void meet(Regex node) throws RuntimeException {
			sb.append("regex(");
			node.getLeftArg().visit(this);
			sb.append(",");
			// TODO escape a regex properly so that it does not kill mermaid or jekyll
			node.getRightArg().visit(this);
			if (node.getFlagsArg() != null) {
				sb.append(",");
				node.getFlagsArg().visit(this);
			}
			sb.append(")");
		}

		@Override
		public void meet(MathExpr node) throws RuntimeException {
			node.getLeftArg().visit(this);
			sb.append(" ");
			sb.append(node.getOperator().getSymbol());
			sb.append(" ");
			node.getRightArg().visit(this);
		}

		@Override
		public void meet(FunctionCall node) throws RuntimeException {
			if (node.getURI().startsWith("http://www.w3.org/2005/xpath-functions#")) {
				sb.append(node.getURI().substring("http://www.w3.org/2005/xpath-functions#".length()));
			} else {
				sb.append(node.getURI());
			}
			sb.append("(");
			for (Iterator<ValueExpr> iterator = node.getArgs().iterator(); iterator.hasNext();) {
				var arg = iterator.next();
				arg.visit(this);
				if (iterator.hasNext()) {
					sb.append(",");
				}
			}
			sb.append(")");
		}

		@Override
		public void meet(ValueConstant node) throws RuntimeException {
			sb.append(prefix(node.getValue(), iriPrefixes, '\''));
		}

		@Override
		public void meet(ListMemberOperator node) throws RuntimeException {
			sb.append(" in ");
		
			int id = existCount++;
			String existId = prefix + "list" + id;
			Map<Value, String> constantKeys = new HashMap<>();
			Map<String, String> variableKeys = new HashMap<>();
			Map<String, String> anonymousKeys = new HashMap<>();

			node.visit(new NameVariablesAndConstants(constantKeys, variableKeys, anonymousKeys, existId));
			addWithLeadingWhiteSpace(
					constantKeys.entrySet().stream().map(en -> en.getValue()
							+ "([" + prefix(en.getKey(), iriPrefixes, '"') + "])" + addProjectedClass(en.getKey(), Set.of())),
					rq);			
			List<ValueExpr> arguments = node.getArguments();
			//The first value is skipped because that is the value being filtered against
			for (int i = 1; i < arguments.size(); i++) {
				ValueExpr ve = arguments.get(i);
				if (ve instanceof ValueConstant vc) {
					String source = constantKeys.get((vc).getValue());
					rq.add(indent() + source + " --o "+ parentId);
				} else if (ve instanceof Var va){
					rq.add(indent() + asString(va) + " --o "+ parentId);
				}
			}
		}
	}
}