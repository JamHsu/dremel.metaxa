package dremel.compiler.expression;

import dremel.compiler.ExpNode;
import dremel.compiler.Query;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.impl.BqlParser;

/**
 * @author nhsan
 *
 */
public class UnaryOp extends AbstractNode{
	ExpNode exp;
	String op;
	int type;

	public UnaryOp(AstNode node, Query query) {
		super(query);
		op = ExpNodeFactory.UnaryOperator.get(node.getType());
		assert (op != null);
		assert (node.getChildCount() == 1);
		AstNode node1 = (AstNode) node.getChild(0);
		assert (node1.getType() == BqlParser.N_EXPRESSION);
		type = node.getType();
		exp = ExpNodeFactory.buildNode(node1, getQuery());

	}

	@Override
	public int getChildCount() {
		return 1;
	}

	@Override
	public ExpNode getChild(int index) {
		if (index == 0)
			return exp;
		return null;
	}

	@Override
	public String generateCode() {
		return "(" + op + exp.generateCode() + ")";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.INVALID;
	}

}
