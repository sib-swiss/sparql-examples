package swiss.sib.rdf.sparql.examples.statistics;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.Resource;
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
import org.slf4j.LoggerFactory;

/**
 * Count basic algebra statistics of a queries.
 */
public class Counter {
	
	public static record CountResult(int perFeature, int perQuery) {
		
	}
	
	private final Map<Class<? extends QueryModelNode>, Integer> countsOfFeatureInUse = new HashMap<>();
	private final Map<Class<? extends QueryModelNode>, Integer> countsOfQueriesUseingFeature = new HashMap<>();
	private int queries;
	private int variables;
	private int constants;
	
	public void count(TupleExpr te, Resource queryId) {
		queries++;
		
		CounterVisitor visitor = new CounterVisitor();
		te.visit(visitor);
		if (visitor.getCount(StatementPattern.class) == 0) {
			LoggerFactory.getLogger(Counter.class).debug("No statement patterns in query, {}", queryId);
		}
		add(visitor.getCounts());
		variables += visitor.getVariables();
		constants += visitor.getConstantVariables();
	}

	private void add(Map<Class<? extends QueryModelNode>, Integer> newCounts) {
		newCounts.forEach((k, v) -> countsOfFeatureInUse.put(k, countsOfFeatureInUse.getOrDefault(k, 0) + v));
		newCounts.forEach((k, v) -> countsOfQueriesUseingFeature.put(k, countsOfQueriesUseingFeature.getOrDefault(k, 0) + 1));
	}

	public int getQueries() {
		return queries;
	}

	public CountResult getBasicPatterns() {
		return count(StatementPattern.class);
	}

	public CountResult getPropertyPaths() {
		int ppf = countsOfFeatureInUse.getOrDefault(ArbitraryLengthPath.class, 0) + countsOfFeatureInUse.getOrDefault(ZeroLengthPath.class, 0);
		int ppq = countsOfQueriesUseingFeature.getOrDefault(ArbitraryLengthPath.class, 0) + countsOfQueriesUseingFeature.getOrDefault(ZeroLengthPath.class, 0);
		return new CountResult(ppf, ppq);
	}

	public CountResult getUnions() {
		return count(Union.class);
	}

	public CountResult getServiceClauses() {
		return count(Service.class);
	}

	public CountResult getOptionals() {
		return count(LeftJoin.class);
	}

	public CountResult getFilters() {
		return count(Filter.class);
	}

	public CountResult getExists() {
		return count(Exists.class);
	}

	public CountResult getMinus() {
		return count(Difference.class);
	}
	
	public CountResult getGroups() {
		return count(Group.class);
	}
	
	public CountResult getOrder() {
		return count(Order.class);
	}

	public CountResult getAggregates() {
		return count(AggregateOperator.class);
	}
	
	public CountResult getSums() {
		return count(Sum.class);
	}
	
	public CountResult getAverages() {
		return count(Avg.class);
	}
	
	public CountResult getMaxs() {
		return count(Max.class);
	}
	
	public CountResult getMins() {
		return count(Min.class);
	}
	
	public CountResult getCount() {
		return count(Count.class);
	}
	
	public CountResult getGroupConcat() {
		return count(GroupConcat.class);
	}

	public CountResult getSample() {
		return count(Sample.class);
	}
	
	private CountResult count(Class<? extends QueryModelNode> clazz) {
		return new CountResult(countsOfFeatureInUse.getOrDefault(clazz, 0),
				countsOfQueriesUseingFeature.getOrDefault(clazz, 0));
	}

	public int getVariables() {
		return variables;
	}
	
	public int getConstants() {
		return constants;
	}
}
