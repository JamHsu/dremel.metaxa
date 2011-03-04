package dremel.compiler.expression;

import dremel.compiler.ExpNode;
import dremel.compiler.Query;
import dremel.parser.AstNode;
import dremel.parser.impl.BqlParser;

/**
 * @author nhsan
 *
 */
public class Constant extends AbstractNode{
	Object value;
	ReturnType type;

	public Constant(AstNode node, Query query) {
		super(query);
		assert (node.getChildCount() == 1);
		assert (node.getType() == BqlParser.N_INT
				|| node.getType() == BqlParser.N_FLOAT || node.getType() == BqlParser.N_STRING);
		switch (node.getType()) {
		case BqlParser.N_INT:
			value = new Integer(node.getChild(0).getText());
			break;
		case BqlParser.N_FLOAT:
			value = new Double(node.getChild(0).getText());
			break;
		case BqlParser.N_STRING:
			value = new String(node.getChild(0).getText());
			break;
		}
	}

	@Override
	public int getChildCount() {
		return 0;
	}

	@Override
	public ExpNode getChild(int index) {
		return null;
	}

	@Override
	public String generateCode() {
		return value.toString();
	}

	@Override
	public ReturnType getReturnType() {
		return type;
	}

}
