package dremel.compiler;

/**
 * ExpNode represent a node in expression tree
 * 
 * @author nhsan
 *
 */
public interface ExpNode {

	public enum ReturnType {
		INVALID, FLOAT, INT, STRING, BOOL
	}

	public Query getQuery();
	
	public int getChildCount();

	public ExpNode getChild(int index);

	public String generateCode();

	public ReturnType getReturnType();
}
