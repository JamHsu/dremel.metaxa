package dremel.compiler.expression;


import dremel.compiler.ExpNode;
import dremel.compiler.Query;
import dremel.compiler.parser.AstNode;

/**
 * @author nhsan
 *
 */
public class BinaryOp extends AbstractNode{
	ExpNode left;
	ExpNode right;
	String op;
	int type;

	public BinaryOp(AstNode node, Query query) {
		super(query);
		op = ExpNodeFactory.BinaryOperator.get(node.getType());
		assert (op != null);
		assert (node.getChildCount() == 2);
		AstNode node1 = (AstNode) node.getChild(0);
		AstNode node2 = (AstNode) node.getChild(1);
		type = node.getType();
		left = ExpNodeFactory.buildNode(node1, getQuery());
		right = ExpNodeFactory.buildNode(node2, getQuery());
	}

	@Override
	public int getChildCount() {
		return 2;
	}

	@Override
	public ExpNode getChild(int index) {
		if (index == 0)
			return left;
		else if (index == 1)
			return right;
		return null;
	}

	@Override
	public String generateCode() {
		return "(" + left.generateCode() + " " + op + " "
				+ right.generateCode() + ")";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.INVALID;
	}
	
	public String getOperation() {
		return op;
	}
}
