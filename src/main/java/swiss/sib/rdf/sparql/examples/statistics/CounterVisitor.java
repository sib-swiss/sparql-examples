package swiss.sib.rdf.sparql.examples.statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.AggregateFunctionCall;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.CompareAll;
import org.eclipse.rdf4j.query.algebra.CompareAny;
import org.eclipse.rdf4j.query.algebra.Copy;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Label;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Like;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Move;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Namespace;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

public class CounterVisitor extends AbstractQueryModelVisitor<RuntimeException> {
	private Map<Class<? extends QueryModelNode>, Integer> counters = new HashMap<>();
	private Set<Var> vars = new HashSet<>();

	@Override
	public void meet(Add add) throws RuntimeException {
		add(Add.class);
		super.meet(add);
	}

	private void add(Class<? extends QueryModelNode> clazz) {
		Integer count = counters.get(clazz);
		if (count == null) {
			count = 0;
		}
		counters.put(clazz, ++count);
	}

	@Override
	public void meet(And node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Avg node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);
	}

	@Override
	public void meet(BindingSetAssignment node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(BNodeGenerator node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Bound node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Clear node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Coalesce node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Compare node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(CompareAll node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(CompareAny node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(DescribeOperator node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Copy node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Count node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);
	}

	@Override
	public void meet(Create node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);

	}

	@Override
	public void meet(Datatype node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);

	}

	@Override
	public void meet(DeleteData node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);

	}

	@Override
	public void meet(Difference node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Distinct node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);

	}

	@Override
	public void meet(EmptySet node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Exists node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Extension node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(ExtensionElem node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Filter node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(FunctionCall node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(AggregateFunctionCall node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);
	}

	@Override
	public void meet(Group node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(GroupConcat node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);
	}

	@Override
	public void meet(GroupElem node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(If node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(In node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(InsertData node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Intersection node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(IRIFunction node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(IsBNode node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(IsLiteral node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(IsNumeric node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(IsResource node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(IsURI node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Join node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Label node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Lang node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(LangMatches node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(LeftJoin node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Like node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Load node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(LocalName node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(MathExpr node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Max node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);
	}

	@Override
	public void meet(Min node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);
	}

	@Override
	public void meet(Modify node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Move node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(MultiProjection node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Namespace node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Not node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Or node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Order node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(OrderElem node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Projection node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(ProjectionElem node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(ProjectionElemList node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Reduced node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Regex node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(SameTerm node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Sample node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);
	}

	@Override
	public void meet(Service node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(SingletonSet node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Slice node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(Str node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Sum node) throws RuntimeException{
		add(node.getClass());
		add(AggregateOperator.class);
		super.meet(node);


	}

	@Override
	public void meet(Union node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(ValueConstant node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);


	}

	@Override
	public void meet(ListMemberOperator node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(Var node) throws RuntimeException{
		vars.add(node);
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meet(ZeroLengthPath node) throws RuntimeException{
		add(node.getClass());
		super.meet(node);
	}

	@Override
	public void meetOther(QueryModelNode node) throws RuntimeException{
		add(node.getClass());
		super.meetOther(node);
	}

	public int getCount(Class<? extends QueryModelNode> class1) {
		return counters.getOrDefault(class1, 0);
	}
	
	public Map<Class<? extends QueryModelNode>, Integer> getCounts() {
		return Map.copyOf(counters);
	}

	public int getConstantVariables() {
		return (int) vars.stream().filter(Var::isConstant).count();
	}
	
	public int getVariables() {
		return vars.size();
	}
	
}
