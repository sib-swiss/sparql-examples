package swiss.sib.rdf.sparql.examples.statistics;

import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;

public class Counter {
	private int queries;
	private int basicPatterns;
	private int propertyPaths;
	private int unions;
	private int serviceClauses;
	private int optionals;
	private int filters;
	private int exists;
	private int minus;

	public void count(TupleExpr te) {
		queries++;
		CounterVisitor visitor = new CounterVisitor();
		te.visit(visitor);
		basicPatterns += visitor.getCount(StatementPattern.class);
		propertyPaths += visitor.getCount(ZeroLengthPath.class);
		propertyPaths += visitor.getCount(ArbitraryLengthPath.class);

		unions += visitor.getCount(Union.class);
		serviceClauses += visitor.getCount(Service.class);
		optionals += visitor.getCount(LeftJoin.class);

		filters += visitor.getCount(Filter.class);
		exists += visitor.getCount(Exists.class);
		minus += visitor.getCount(Difference.class);
	}

	public int getQueries() {
		return queries;
	}

	public int getBasicPatterns() {
		return basicPatterns;
	}

	public int getPropertyPaths() {
		return propertyPaths;
	}

	public int getUnions() {
		return unions;
	}

	public int getServiceClauses() {
		return serviceClauses;
	}

	public int getOptionals() {
		return optionals;
	}

	public int getFilters() {
		return filters;
	}

	public int getExists() {
		return exists;
	}

	public int getMinus() {
		return minus;
	}
}
