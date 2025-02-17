package swiss.sib.rdf.sparql.examples.mermaid;

import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * A class to find which constants are not only used as predicates. 
 */
final class FindWhichConstantsAreNotOnlyUsedAsPredicates
		extends AbstractQueryModelVisitor<RuntimeException> {
	private final Set<Value> usedAsNode;

	public FindWhichConstantsAreNotOnlyUsedAsPredicates(Set<Value> usedAsNode) {
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