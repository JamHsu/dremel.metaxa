package dremel.compiler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import dremel.compiler.impl.Descriptor;
import dremel.compiler.impl.FieldDescriptor;

import dremel.compiler.expression.Function;
import dremel.compiler.expression.Symbol;
import dremel.dataset.Table;


/**
 * 
 * Query represent semantic model of a BQL
 * 
 * @author nhsan
 *
 */
public interface Query {
	public List<Table> getTables(); // FROM clause

	public List<Query> getSubQueries(); // FROM clause

	public List<Expression> getSelectExpressions();// SELECT clause

	public List<Expression> getGroupByExpressions();// GROUP BY clause

	public List<Expression> getOrderByExpressions();// ORDER BY clause

	public Expression getFilter(); // WHERE clause

	public Descriptor getSourceSchema(); // common schema of tables or
											// sub-queries in FROM clause

	public Descriptor getTargetSchema(); // schema for result set, can be source
											// schema for parent query if this
											// query is sub-queries

	public int getLimitCount(); // LIMIT clause
	
	public Map<String, Symbol> getSymbolTable(); //field name + alias
	
	public List<Function> getAggregationFunctions();
}
