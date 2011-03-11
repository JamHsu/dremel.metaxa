package dremel.compiler.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import java.util.List;

import dremel.compiler.Query;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.impl.BqlParser;
import dremel.dataset.SchemaTree;

/**
 * @author nhsan
 * 
 */
public class Expression implements dremel.compiler.Expression {

	// Map of binary operators to java operators
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

	// Map of unary operators to java operators
	public static final Map<Integer, String> UnaryOperator = new HashMap<Integer, String>() {
		{
			put(BqlParser.N_LOGICAL_NOT, "!");
			put(BqlParser.N_BITWISE_NOT, "~");
		}
	};

	static public Function buildFunction(AstNode node, Query query) {

		assert (node.getType() == BqlParser.N_CALL_PARAMS);

		int count = node.getChildCount();
		assert (count >= 1);
		AstNode id = (AstNode) node.getChild(count - 1);
		assert (id.getType() == BqlParser.N_ID);
		String name = CompilerImpl.getID(id);

		if (name.equalsIgnoreCase("length")) {
			return new StringLengthFunc(node, query);
		} else
			return new Function(node, query);
	}

	/**
	 * - node factory
	 * 
	 * @param node
	 * @param query
	 * @return
	 */
	static public Node buildNode(AstNode node, Query query) {
		AstNode node2 = node;
		while (node2.getType() == BqlParser.N_EXPRESSION) {
			assert (node2.getChildCount() == 1);
			node2 = (AstNode) node2.getChild(0);
		}

		if (node2.getType() == BqlParser.N_CALL_PARAMS) {
			return (Node) (buildFunction(node2, query));
		} else if (node2.getType() == BqlParser.N_ID) {
			Map<String, dremel.compiler.Expression.Symbol> symbolTable = query.getSymbolTable();
			String id = CompilerImpl.getID(node2);
			if (symbolTable.containsKey(id)) {
				Object o = symbolTable.get(id);
				assert (o instanceof Node);
				return (Node) o;
			}
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

	static public abstract class AbstractNode implements Node {
		Query query;

		public AbstractNode(Query query) {
			this.query = query;
		}

		@Override
		public Query getQuery() {
			return query;
		}
	}

	static public class BinaryOp extends AbstractNode implements dremel.compiler.Expression.BinaryOp {
		Node left;
		Node right;
		String op;
		int type;

		public BinaryOp(AstNode node, Query query) {
			super(query);
			op = BinaryOperator.get(node.getType());
			assert (op != null);
			assert (node.getChildCount() == 2);
			AstNode node1 = (AstNode) node.getChild(0);
			AstNode node2 = (AstNode) node.getChild(1);
			type = node.getType();
			left = buildNode(node1, getQuery());
			right = buildNode(node2, getQuery());
		}

		@Override
		public int getChildCount() {
			return 2;
		}

		@Override
		public Node getChild(int index) {
			if (index == 0)
				return left;
			else if (index == 1)
				return right;
			return null;
		}

		@Override
		public String generateCode() {
			return "(" + left.generateCode() + " " + op + " " + right.generateCode() + ")";
		}

		@Override
		public ReturnType getReturnType() {
			return ReturnType.INVALID;
		}

		@Override
		public Node getLeftNode() {
			return left;
		}

		@Override
		public Node getRightNode() {
			return right;
		}

		@Override
		public String getOperator() {
			return op;
		}
	}

	static public class UnaryOp extends AbstractNode implements dremel.compiler.Expression.UnaryOp {
		Node exp;
		String op;
		int type;

		public UnaryOp(AstNode node, Query query) {
			super(query);
			op = UnaryOperator.get(node.getType());
			assert (op != null);
			assert (node.getChildCount() == 1);
			AstNode node1 = (AstNode) node.getChild(0);
			assert (node1.getType() == BqlParser.N_EXPRESSION);
			type = node.getType();
			exp = buildNode(node1, getQuery());

		}

		@Override
		public int getChildCount() {
			return 1;
		}

		@Override
		public Node getChild(int index) {
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

		@Override
		public Node getNode() {
			return exp;
		}

		@Override
		public String getOperator() {
			return op;
		}
	}

	static public class Constant extends AbstractNode implements dremel.compiler.Expression.Constant {
		Object value;
		ReturnType type;

		public Constant(AstNode node, Query query) {
			super(query);
			assert (node.getChildCount() == 1);
			assert (node.getType() == BqlParser.N_INT || node.getType() == BqlParser.N_FLOAT || node.getType() == BqlParser.N_STRING);
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
		public Node getChild(int index) {
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

		@Override
		public Object getValue() {
			return value;
		}
	}

	static public class Function extends AbstractNode implements dremel.compiler.Expression.Function {

		List<Node> nodes;
		String functionName;

		public Function(AstNode node, Query query) {
			super(query);
			assert (node.getType() == BqlParser.N_CALL_PARAMS);
			nodes = new LinkedList<Node>();

			int count = node.getChildCount();
			assert (count >= 1);
			AstNode id = (AstNode) node.getChild(count - 1);
			assert (id.getType() == BqlParser.N_ID);
			functionName = CompilerImpl.getID(id);

			for (int i = 0; i < count - 1; i++) {
				nodes.add(buildNode((AstNode) node.getChild(i), getQuery()));
			}
		}

		@Override
		public int getChildCount() {
			return nodes.size();
		}

		@Override
		public Node getChild(int index) {
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

		@Override
		public String getName() {
			return functionName;
		}

		@Override
		public int getArgumentCount() {
			return nodes.size();
		}

		@Override
		public Node getArgument(int index) {
			return nodes.get(index);
		}
	}

	static public class StringLengthFunc extends Function {
		public StringLengthFunc(AstNode node, Query query) {
			super(node, query);
			assert (nodes.size() == 1);
		}

		@Override
		public String generateCode() {
			return nodes.get(0).generateCode() + ".toString().length()";
		}
	}

	static public class Symbol extends AbstractNode implements dremel.compiler.Expression.Symbol {
		String symbol;
		Object reference;
		int sliceMappingIndex;

		public Symbol(String symbol, Query query) {
			super(query);
			this.symbol = symbol.toLowerCase();
			sliceMappingIndex = -1;
		}

		@Override
		public int getChildCount() {
			return 0;
		}

		@Override
		public Node getChild(int index) {
			return null;
		}

		@Override
		public String generateCode() {
			if (reference instanceof Expression) {
				Expression exp = (Expression) reference;
				return exp.getRoot().generateCode();
			} else if ((reference instanceof SchemaTree) && (sliceMappingIndex != -1)) {
				SchemaTree d = (SchemaTree) reference;
				if (d.isTypeInt64()) {
					return "inSlice.intValue(" + sliceMappingIndex + ")";
				} else if (d.isTypeString()) {
					return "inSlice.stringValue(" + sliceMappingIndex + ")";
				} else if (d.isTypeFloat()) {
					return "inSlice.floatValue(" + sliceMappingIndex + ")";
				} else if (d.isTypeBool()) {
					return "inSlice.boolValue(" + sliceMappingIndex + ")";
				}

			}
			return symbol;
		}

		@Override
		public ReturnType getReturnType() {
			return null;
		}

		public String getSymbol() {
			return symbol;
		}

		@Override
		public Object getReference() {
			return reference;
		}

		@Override
		public void setReference(Object reference) {
			this.reference = reference;
		}

		@Override
		public int getSliceMappingIndex() {
			return sliceMappingIndex;
		}

		@Override
		public void setSliceMappingIndex(int sliceMappingIndex) {
			this.sliceMappingIndex = sliceMappingIndex;
		}
	}

	Node root;
	String scope;
	String alias;
	int rLevel;
	int withinLevel;
	List<SchemaTree> fields;

	public Expression() {
		fields = new LinkedList<SchemaTree>();
	}

	@Override
	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}

	@Override
	public String getWithin() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	public int getWithinLevel() {
		return withinLevel;
	}

	public void setWithinLevel(int withinLevel) {
		this.withinLevel = withinLevel;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public int getRepetitionLevel() {
		return rLevel;
	}

	public void setRLevel(int rLevel) {
		this.rLevel = rLevel;
	}

	@Override
	public List<SchemaTree> getRelatedFields() {
		return fields;
	}
}
