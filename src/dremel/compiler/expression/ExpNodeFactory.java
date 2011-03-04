package dremel.compiler.expression;

import java.util.HashMap;
import java.util.Map;

import dremel.compiler.ExpNode;
import dremel.compiler.ParserTest;
import dremel.compiler.Query;
import dremel.compiler.impl.MetaxaQuery;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;
import dremel.parser.impl.BqlParser;

/**
 * 
 * ExpNodeFactory contains static list of operators and function for creating a
 * expression node from an AstNode
 * 
 * @author nhsan
 * 
 */
public class ExpNodeFactory {

	//Map of binary operators to java operators
	public static final Map<Integer, String> BinaryOperator = new HashMap<Integer, String>() {
		{
			put(BqlParser.N_ADD, "+");
			put(BqlParser.N_SUBSTRUCT, "-");
			put(BqlParser.N_MULTIPLY, "*");
			put(BqlParser.N_DIVIDE, "/");
			put(BqlParser.N_GREATER_THAN, ">");
			put(BqlParser.N_LESS_THAN, "<");
			put(BqlParser.N_BITWISE_AND, "&");
			put(BqlParser.N_BITWISE_LEFT_SHIFT, "<<");
			put(BqlParser.N_BITWISE_OR, "|");
			put(BqlParser.N_BITWISE_RIGHT_SHIFT, ">>");
			put(BqlParser.N_BITWISE_XOR, "^");
			put(BqlParser.N_EQUAL, "==");
			put(BqlParser.N_GREATER_THAN_OR_EQUAL, ">=");
			put(BqlParser.N_LESS_THAN_OR_EQUAL, "<=");
			put(BqlParser.N_LOGICAL_AND, "&&");
			put(BqlParser.N_LOGICAL_OR, "||");
			put(BqlParser.N_NOT_EQUAL, "!=");
		}
	};

	//Map of unary operators to java operators
	public static final Map<Integer, String> UnaryOperator = new HashMap<Integer, String>() {
		{
			put(BqlParser.N_LOGICAL_NOT, "!");
			put(BqlParser.N_BITWISE_NOT, "~");
		}
	};

	static public ExpNode buildNode(AstNode node, Query query) {
		AstNode node2 = node;
		while (node2.getType() == BqlParser.N_EXPRESSION) {
			assert (node2.getChildCount() == 1);
			node2 = (AstNode) node2.getChild(0);
		}

		if (node2.getType() == BqlParser.N_CALL_PARAMS) {
			return new Function(node2, query);
		} else if (node2.getType() == BqlParser.N_ID) {
			Map<String, Symbol> symbolTable = query.getSymbolTable();
			String id = MetaxaQuery.getID(node2);
			if (symbolTable.containsKey(id))
				return symbolTable.get(id);
			Symbol s = new Symbol(id, query);
			symbolTable.put(id, s);
			return s;
		} else if (node2.getType() == BqlParser.N_INT || node2.getType() == BqlParser.N_FLOAT || node2.getType() == BqlParser.N_STRING) {
			return new Constant(node2, query);
		} else {
			String op = BinaryOperator.get(node2.getType());
			if (op != null) {
				return new BinaryOp(node2, query);
			}

			op = UnaryOperator.get(node2.getType());
			if (op != null) {
				return new UnaryOp(node2, query);
			}
			assert (false);
			return null;
		}
	}
}
