package dremel.compiler.expression;

import java.util.LinkedList;

import java.util.List;

import dremel.compiler.ExpNode;
import dremel.compiler.Query;
import dremel.compiler.impl.MetaxaQuery;
import dremel.parser.AstNode;
import dremel.parser.impl.BqlParser;

/**
 * @author nhsan
 *
 */
public class Function extends AbstractNode {

	List<ExpNode> nodes;
	String functionName;

	public Function(AstNode node, Query query) {
		super(query);
		assert (node.getType() == BqlParser.N_CALL_PARAMS);
		nodes = new LinkedList<ExpNode>();

		int count = node.getChildCount();
		assert (count >= 1);
		AstNode id = (AstNode) node.getChild(count - 1);
		assert (id.getType() == BqlParser.N_ID);
		functionName = MetaxaQuery.getID(id);

		for (int i = 0; i < count - 1; i++) {
			nodes.add(ExpNodeFactory.buildNode((AstNode) node.getChild(i), getQuery()));
		}
	}

	@Override
	public int getChildCount() {
		return nodes.size();
	}

	@Override
	public ExpNode getChild(int index) {
		return nodes.get(index);
	}

	@Override
	public String generateCode() {
		StringBuilder builder = new StringBuilder();
		builder.append(functionName);
		builder.append("(");

		if (nodes.size() > 0) {
			builder.append(nodes.get(0).generateCode());
		}

		for (int i = 1; i < nodes.size(); i++) {
			builder.append(", ");
			builder.append(nodes.get(i).generateCode());
		}

		builder.append(")");
		return builder.toString();
	}

	@Override
	public ReturnType getReturnType() {
		return null;
	}

	public String getFunctionName() {
		return functionName;
	}
}
