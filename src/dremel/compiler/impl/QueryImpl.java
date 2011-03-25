package dremel.compiler.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dremel.compiler.Expression;
import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Query;
import dremel.compiler.impl.Expression.AggFunction;
import dremel.dataset.SchemaTree;
import dremel.dataset.Table;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;

/**
 * @author nhsan
 * 
 */
public class QueryImpl implements Query {
	List<Tablet> tables;
	List<dremel.compiler.Query> subQueries;
	List<Expression> selectExps;
	List<Symbol> groupByExps;
	List<Symbol> orderByExps;
	Map<String, Symbol> symbolTable;
	Expression filter;
	int limit;
	SchemaColumnar sourceSchema;
	SchemaColumnar targetSchema;
	List<AggFunction> aggregationFunctions;

	public QueryImpl() {
		tables = new LinkedList<Tablet>();
		subQueries = new LinkedList<dremel.compiler.Query>();
		selectExps = new LinkedList<Expression>();
		groupByExps = new LinkedList<Symbol>();
		orderByExps = new LinkedList<Symbol>();
		symbolTable = new HashMap<String, dremel.compiler.Expression.Symbol>();
		aggregationFunctions = new LinkedList<AggFunction>();
	}

	@Override
	public List<Tablet> getTables() {
		return tables;
	}

	@Override
	public List<dremel.compiler.Query> getSubQueries() {
		return subQueries;
	}

	@Override
	public List<Expression> getSelectExpressions() {
		return selectExps;
	}

	@Override
	public List<Symbol> getGroupByExpressions() {
		return groupByExps;
	}

	@Override
	public List<Symbol> getOrderByExpressions() {
		return orderByExps;
	}

	@Override
	public Expression getFilter() {
		return filter;
	}

	@Override
	public SchemaColumnar getSourceSchema() {
		return sourceSchema;
	}

	@Override
	public SchemaColumnar getTargetSchema() {
		return targetSchema;
	}

	@Override
	public int getLimitCount() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setSourceSchema(SchemaColumnar sourceSchema) {
		this.sourceSchema = sourceSchema;
	}

	public void setTargetSchema(SchemaColumnar targetSchema) {
		this.targetSchema = targetSchema;
	}

	@Override
	public Map<String, Symbol> getSymbolTable() {
		return symbolTable;
	}

	@Override
	public List<AggFunction> getAggregationFunctions() {
		return aggregationFunctions;
	}
	
	public void setFilter(Expression filter) {
		this.filter = filter;
	}
}
