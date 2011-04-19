package dremel.compiler;

import java.util.List;
import java.util.Map;
//import com.google.protobuf.Descriptors.Descriptor;

import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.impl.Expression.AggFunction;
import dremel.dataset.SchemaTree;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;

/**
 * 
 * Query represent semantic model of a BQL
 * 
 * @author nhsan
 * 
 */
public interface Query {
	public List<Tablet> getTables(); // FROM clause

	public List<Query> getSubQueries(); // FROM clause

	public List<Expression> getSelectExpressions();// SELECT clause

	public List<Symbol> getGroupByExpressions();// GROUP BY clause (field or
												// alias)

	public List<Symbol> getOrderByExpressions();// ORDER BY clause (field or
												// alias)

	public Expression getFilter(); // WHERE clause

	public SchemaColumnar getSourceSchemaColumnar(); // common schema of tables
														// or
	// sub-queries in FROM clause

	public SchemaColumnar getTargetSchemaColumnar(); // schema for result set,
														// can be
	// source
	// schema for parent query if
	// this
	// query is sub-queries

	public SchemaTree getSourceSchemaTree();

	public SchemaTree getTargetSchemaTree();

	public int getLimitCount(); // LIMIT clause

	public Map<String, Symbol> getSymbolTable(); // field name + alias

	public List<AggFunction> getAggregationFunctions();

	public Query getParent();

	public int getID();

	public String getStringID();
}
