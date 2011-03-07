package dremel.compiler;

import java.util.List;

import dremel.dataset.SchemaTree;

/**
 * Expression contains informations about an expression in SELECT/WHERE/GROUPBY/ORDER BY clause.
 * 
 * @author nhsan
 *
 */
public interface Expression {

	public String getAlias();
	public ExpNode getRoot();

	public String getWithin();// null, record, node_name

	public int getWithinLevel();
	
	public int getRepetitionLevel();
	public List<SchemaTree> getRelatedFields();

}
