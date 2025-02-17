package swiss.sib.rdf.sparql.examples.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.junit.jupiter.api.Test;

class CounterVisitorTest {

	@Test
	void aggregates() {

		CounterVisitor visitor = new CounterVisitor();
		visitor.meet(new Sum(new ValueConstant(SimpleValueFactory.getInstance().createLiteral(1))));;
		assertEquals(1, visitor.getCount(Sum.class), "We should have one add operator.");
		int count = visitor.getCount(AggregateOperator.class);
		assertEquals(1, count, "We should have one aggregate operator.");
	}
}
