package dremel.compiler.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import java.util.List;

import com.google.protobuf.Descriptors.FieldDescriptor;

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
		} else if (name.equalsIgnoreCase("count")) {
			return new CountFunc(node, query);
		} else if (name.equalsIgnoreCase("sum")) {
			return new SumFunc(node, query);
		} else
			throw new RuntimeException("Not support function:" + name);
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

		@Override
		public boolean isTypeInt() {
			return (getReturnType() == ReturnType.INT);
		}

		@Override
		public boolean isTypeFloat() {
			return (getReturnType() == ReturnType.FLOAT);
		}

		@Override
		public boolean isTypeBool() {
			return (getReturnType() == ReturnType.BOOL);
		}

		@Override
		public boolean isTypeString() {
			return (getReturnType() == ReturnType.STRING);
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
			ReturnType r1 = left.getReturnType();
			ReturnType r2 = right.getReturnType();

			if (r1 == ReturnType.INVALID || r2 == ReturnType.INVALID)
				return ReturnType.INVALID;

			switch (type) {
			case BqlParser.N_LOGICAL_AND:
			case BqlParser.N_LOGICAL_OR:
				if (r1 != ReturnType.BOOL || r2 != ReturnType.BOOL)
					return ReturnType.INVALID;
				else
					return ReturnType.BOOL;
			case BqlParser.N_EQUAL:
			case BqlParser.N_LESS_THAN:
			case BqlParser.N_LESS_THAN_OR_EQUAL:
			case BqlParser.N_GREATER_THAN:
			case BqlParser.N_GREATER_THAN_OR_EQUAL:
			case BqlParser.N_NOT_EQUAL:
				if ((r1 == ReturnType.STRING || r2 == ReturnType.STRING) && (r1 != r2))
					return ReturnType.INVALID;
				return ReturnType.BOOL;
			case BqlParser.N_BITWISE_AND:
			case BqlParser.N_BITWISE_LEFT_SHIFT:
			case BqlParser.N_BITWISE_OR:
			case BqlParser.N_BITWISE_RIGHT_SHIFT:
			case BqlParser.N_BITWISE_XOR:
				if (r1 != ReturnType.INT || r2 != ReturnType.INT)
					return ReturnType.INVALID;
				else
					return ReturnType.INT;
			case BqlParser.N_ADD:
			case BqlParser.N_SUBSTRUCT:
			case BqlParser.N_MULTIPLY:
			case BqlParser.N_DIVIDE:
				if (r1 == ReturnType.STRING || r2 == ReturnType.STRING)
					return ReturnType.INVALID;
				else if (r1 == ReturnType.BOOL || r2 == ReturnType.BOOL)
					return ReturnType.INVALID;
				else if (r1 == ReturnType.FLOAT || r2 == ReturnType.FLOAT)
					return ReturnType.FLOAT;
				else
					return ReturnType.INT;
			default:
				return ReturnType.INVALID;
			}
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
			ReturnType r = exp.getReturnType();
			if (r == ReturnType.INVALID)
				return ReturnType.INVALID;
			if (type == BqlParser.N_BITWISE_NOT) {
				if (r != ReturnType.INT)
					return ReturnType.INVALID;
				else
					return ReturnType.INT;
			} else if (type == BqlParser.N_LOGICAL_NOT) {
				if (r != ReturnType.BOOL)
					return ReturnType.INVALID;
				else
					return ReturnType.BOOL;
			}
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
				type = ReturnType.INT;
				break;
			case BqlParser.N_FLOAT:
				value = new Double(node.getChild(0).getText());
				type = ReturnType.FLOAT;
				break;
			case BqlParser.N_STRING:
				value = new String(node.getChild(0).getText());
				type = ReturnType.STRING;
				break;
			default:
				type = ReturnType.INVALID;
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

	static public abstract class Function extends AbstractNode implements dremel.compiler.Expression.Function {

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

	static public abstract class AggFunction extends Function {
		dremel.compiler.Expression.Symbol symbol;

		/**
		 * @param node
		 * @param query
		 */
		public AggFunction(AstNode node, Query query) {
			super(node, query);

			assert (nodes.size() == 1);
			assert (nodes.get(0) instanceof dremel.compiler.Expression.Symbol);
			symbol = (dremel.compiler.Expression.Symbol) nodes.get(0);
		}

		public dremel.compiler.Expression.Symbol getSymbol() {
			return symbol;
		}
	}

	/*
	 * some build-in function: length, count, sum....
	 */
	static public class StringLengthFunc extends Function {
		public StringLengthFunc(AstNode node, Query query) {
			super(node, query);
			assert (nodes.size() == 1);
		}

		@Override
		public String generateCode() {
			return nodes.get(0).generateCode() + ".toString().length()"; // temporary
																			// use
																			// java
																			// string
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Node#getReturnType()
		 */
		@Override
		public ReturnType getReturnType() {
			ReturnType r = nodes.get(0).getReturnType();
			if (r != ReturnType.STRING)
				return ReturnType.INVALID;
			return ReturnType.INT;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Function#isAggregate()
		 */
	}

	static public class CountFunc extends AggFunction {
		dremel.compiler.Expression.Symbol symbol;

		public CountFunc(AstNode node, Query query) {
			super(node, query);
			assert (nodes.size() == 1);
			assert (nodes.get(0) instanceof dremel.compiler.Expression.Symbol);
			symbol = (dremel.compiler.Expression.Symbol) nodes.get(0);
			//assert (symbol.getReference() instanceof FieldDescriptor);
		}

		@Override
		public String generateCode() {
			return ((Symbol) nodes.get(0)).getJavaName() + "_" + hashCode() + "++";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Node#getReturnType()
		 */
		@Override
		public ReturnType getReturnType() {
			return ReturnType.INT;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Function#isAggregate()
		 */
	}

	static public class SumFunc extends AggFunction {
		dremel.compiler.Expression.Symbol symbol;

		public SumFunc(AstNode node, Query query) {
			super(node, query);
			assert (nodes.size() == 1);
			assert (nodes.get(0) instanceof dremel.compiler.Expression.Symbol);
			symbol = (dremel.compiler.Expression.Symbol) nodes.get(0);
			//assert (symbol.getReference() instanceof FieldDescriptor);
		}

		@Override
		public String generateCode() {
			return ((Symbol) nodes.get(0)).getJavaName() + "_" + hashCode() + " += " + getSymbol().getJavaName();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Node#getReturnType()
		 */
		@Override
		public ReturnType getReturnType() {
			Symbol s= (Symbol)getSymbol();
			
			if (s.isTypeInt())
				return ReturnType.INT;
			else if (s.isTypeFloat())
				return ReturnType.FLOAT;
			else
				return ReturnType.INVALID;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Function#isAggregate()
		 */
	}

	static public class Symbol extends AbstractNode implements dremel.compiler.Expression.Symbol {
		String symbol;
		Object reference;
		int sliceMappingIndex;

		public Symbol(String symbol, Query query) {
			super(query);
			this.symbol = symbol.toLowerCase();
			sliceMappingIndex = -1;
			reference = null;
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
			} else
				return getJavaName();
		}

		@Override
		public ReturnType getReturnType() {
			if (reference instanceof SchemaTree) {
				SchemaTree d = (SchemaTree) reference;
				if (d.isTypeInt64())
					return ReturnType.INT;
				else if (d.isTypeBool())
					return ReturnType.BOOL;
				else if (d.isTypeFloat())
					return ReturnType.FLOAT;
				else if (d.isTypeString())
					return ReturnType.STRING;
			} else if (reference instanceof Expression) {
				Expression exp = (Expression) reference;
				return exp.getRoot().getReturnType();
			}
			return ReturnType.INVALID;
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Symbol#getJavaName()
		 */
		@Override
		public String getJavaName() {
			return symbol.replaceAll("\\.", "_");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Symbol#isAlias()
		 */
		@Override
		public boolean isAlias() {
			return (reference instanceof Expression);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see dremel.compiler.Expression.Symbol#isColumnID()
		 */
		@Override
		public boolean isColumnID() {
			return (reference instanceof SchemaTree);
		}
	}

	Node root;
	String scope;
	String alias;
	int rLevel;
	int dLelel;
	int withinLevel;
	List<dremel.compiler.Expression.Symbol> symbols;
	ReturnType type;

	public Expression() {
		symbols = new LinkedList<dremel.compiler.Expression.Symbol>();
		type = ReturnType.NULL;
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
	public int getDefinitionLevel() {
		return dLelel;
	}
	
	public void setdLelel(int dLelel) {
		this.dLelel = dLelel;
	}
	
	@Override
	public int getRepetitionLevel() {
		return rLevel;
	}

	public void setRLevel(int rLevel) {
		this.rLevel = rLevel;
	}

	@Override
	public List<dremel.compiler.Expression.Symbol> getSymbols() {
		return symbols;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dremel.compiler.Expression#getReturnType()
	 */
	@Override
	public ReturnType getReturnType() {
		if (type == ReturnType.NULL)
			type = getRoot().getReturnType();
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dremel.compiler.Expression#getJavaName()
	 */
	@Override
	public String getJavaName() {
		if (alias != null)
			return alias.replaceAll("\\.", "_");
		else {
			return "noname_" + Integer.toHexString(this.hashCode());
		}
	}

	@Override
	public boolean isTypeInt() {
		return (getReturnType() == ReturnType.INT);
	}

	@Override
	public boolean isTypeFloat() {
		return (getReturnType() == ReturnType.FLOAT);
	}

	@Override
	public boolean isTypeBool() {
		return (getReturnType() == ReturnType.BOOL);
	}

	@Override
	public boolean isTypeString() {
		return (getReturnType() == ReturnType.STRING);
	}
}