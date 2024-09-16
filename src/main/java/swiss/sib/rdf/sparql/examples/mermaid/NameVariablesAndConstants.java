package swiss.sib.rdf.sparql.examples.mermaid;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

public final class NameVariablesAndConstants extends AbstractQueryModelVisitor<RuntimeException> {
	private final Map<Value, String> constantKeys;
	private final Map<String, String> variableKeys;
	private final Map<String, String> anonymousKeys;
	private final String prefix;

	public NameVariablesAndConstants(Map<Value, String> constantKeys, Map<String, String> variableKeys,
			Map<String, String> anonymousKeys) {
		this(constantKeys, variableKeys, anonymousKeys, "");
	}

	public NameVariablesAndConstants(Map<Value, String> constantKeys, Map<String, String> variableKeys,
			Map<String, String> anonymousKeys, String prefix) {
		this.constantKeys = constantKeys;
		this.variableKeys = variableKeys;
		this.anonymousKeys = anonymousKeys;
		this.prefix = prefix;
	}

	@Override
	public void meet(Var node) throws RuntimeException {
		super.meet(node);
		if (node.isAnonymous() && !node.isConstant()) {
			if (!anonymousKeys.containsKey(node.getName())) {
				String nextId = prefix + "a" + (anonymousKeys.size() + 1);
				anonymousKeys.put(node.getName(), nextId);
			}
		} else if (!node.isConstant() && !variableKeys.containsKey(node.getName())) {
			String nextId = prefix + "v" + (variableKeys.size() + 1);
			variableKeys.put(node.getName(), nextId);

		} else if (node.isConstant() && !constantKeys.containsKey(node.getValue())) {
			String nextId = prefix + "c" + (constantKeys.size() + 1);
			constantKeys.put(node.getValue(), nextId);
		}
	}

	@Override
	public void meet(BindingSetAssignment node) throws RuntimeException {
		super.meet(node);
		for (String name : node.getBindingNames()) {
			String nextId = prefix + "v" + (variableKeys.size() + 1);
			variableKeys.put(name, nextId);
		}
	}
	
	@Override
	public void meet(ExtensionElem node)
	{
		String nextId = prefix + "v" + (variableKeys.size() + 1);
		variableKeys.put(node.getName(), nextId);
	}

	@Override
	public void meet(ListMemberOperator node) throws RuntimeException {
		for(ValueExpr argument : node.getArguments()) {
			argument.visit(this);
		}
	}

	@Override
	public void meet(ValueConstant node) throws RuntimeException {
		if (!constantKeys.containsKey(node.getValue())) {
			String nextId = prefix + "c" + (constantKeys.size() + 1);
			constantKeys.put(node.getValue(), nextId);
		}
	}
	
	
}