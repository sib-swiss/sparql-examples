package swiss.sib.rdf.sparql.examples.statistics;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;

/**
 * Count basic algebra statistics of a queries.
 */
public class Counter {
	private final Map<Class<? extends QueryModelNode>, Integer> counts = new HashMap<>();
	private int queries;

	public void count(TupleExpr te) {
		queries++;
		CounterVisitor visitor = new CounterVisitor();
		te.visit(visitor);
		add(visitor.getCounts());

	}

	private void add(Map<Class<? extends QueryModelNode>, Integer> newCounts) {
		newCounts.forEach((k, v) -> counts.put(k, counts.getOrDefault(k, 0) + v));
	}

	public int getQueries() {
		return queries;
	}

	public int getBasicPatterns() {
		return counts.getOrDefault(StatementPattern.class, 0);
	}

	public int getPropertyPaths() {
		return counts.getOrDefault(ArbitraryLengthPath.class, 0) + counts.getOrDefault(ZeroLengthPath.class, 0);
	}

	public int getUnions() {
		return counts.getOrDefault(Union.class, 0);
	}

	public int getServiceClauses() {
		return counts.getOrDefault(Service.class, 0);
	}

	public int getOptionals() {
		return counts.getOrDefault(LeftJoin.class, 0);
	}

	public int getFilters() {
		return counts.getOrDefault(Filter.class, 0);
	}

	public int getExists() {
		return counts.getOrDefault(Exists.class, 0);
	}

	public int getMinus() {
		return counts.getOrDefault(Difference.class, 0);
	}
	
	public int getGroups() {
		return counts.getOrDefault(Group.class, 0);
	}
	
	public int getOrder() {
		return counts.getOrDefault(Order.class, 0);
	}

	public int getAggregates() {
		return counts.getOrDefault(AggregateOperator.class, 0);
	}
	
	public int getSums() {
		return counts.getOrDefault(Sum.class, 0);
	}
	
	public int getAverages() {
		return counts.getOrDefault(Avg.class, 0);
	}
	
	public int getMaxs() {
		return counts.getOrDefault(Max.class, 0);
	}
	
	public int getMins() {
		return counts.getOrDefault(Min.class, 0);
	}
	
	public int getCount() {
		return counts.getOrDefault(Count.class, 0);
	}
	
	public int getGroupConcat() {
		return counts.getOrDefault(GroupConcat.class, 0);
	}

	public int getSample() {
		return counts.getOrDefault(Sample.class, 0);
	}
	
}
