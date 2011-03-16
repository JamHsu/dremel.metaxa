package dremel.compiler;

import java.util.List;

import dremel.dataset.SchemaTree;

//import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * Expression contains informations about an expression in
 * SELECT/WHERE/GROUPBY/ORDER BY clause.
 * 
 * @author nhsan
 * 
 */
public interface Expression {
	public enum ReturnType {
		INVALID, NULL, FLOAT, INT, STRING, BOOL
	}

	public interface Node {
		public Query getQuery();

		public int getChildCount();

		public Node getChild(int index);

		public String generateCode();

		public ReturnType getReturnType();
	}

	public interface BinaryOp {
		public Node getLeftNode();

		public Node getRightNode();

		public String getOperator();
	}

	public interface UnaryOp {
		public Node getNode();

		public String getOperator();
	}

	public interface Constant {
		public Object getValue();
	}

	public interface Symbol {
		public String getSymbol();

		public Object getReference();

		public void setReference(Object ref);

		public String getJavaName();

		public boolean isAlias();

		public boolean isColumnID();

		public boolean isTypeInt();

		public boolean isTypeFloat();

		public boolean isTypeBool();

		public boolean isTypeString();
	}

	public interface Function {
		public String getName();

		public int getArgumentCount();

		public Node getArgument(int index);

		public boolean isAggregate();
	}

	public String getAlias();

	public Node getRoot();

	public String getWithin();// null, record, node_name

	public int getWithinLevel();

	public int getRepetitionLevel();

	public List<SchemaTree> getRelatedFields();

	public ReturnType getReturnType();

	public String getJavaName();

	public boolean isTypeInt();

	public boolean isTypeFloat();

	public boolean isTypeBool();

	public boolean isTypeString();
}
