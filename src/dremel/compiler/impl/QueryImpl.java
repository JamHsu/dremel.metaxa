package dremel.compiler.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dremel.compiler.Expression;
import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Query;
import dremel.dataset.SchemaTree;
import dremel.dataset.Table;

/**
 * @author nhsan
 * 
 */
public class QueryImpl implements Query {
	List<Table> tables;
	List<dremel.compiler.Query> subQueries;
	List<Expression> selectExps;
	List<Symbol> groupByExps;
	List<Symbol> orderByExps;
	Map<String, Symbol> symbolTable;
	Expression filter;
	int limit;
	SchemaTree sourceSchema;
	SchemaTree targetSchema;
	List<dremel.compiler.Expression.Function> aggregationFunctions;

	public QueryImpl() {
		tables = new LinkedList<Table>();
		subQueries = new LinkedList<dremel.compiler.Query>();
		selectExps = new LinkedList<Expression>();
		groupByExps = new LinkedList<Symbol>();
		orderByExps = new LinkedList<Symbol>();
		symbolTable = new HashMap<String, dremel.compiler.Expression.Symbol>();
		aggregationFunctions = new LinkedList<dremel.compiler.Expression.Function>();
	}

	@Override
	public List<Table> getTables() {
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
	public SchemaTree getSourceSchema() {
		return sourceSchema;
	}

	@Override
	public SchemaTree getTargetSchema() {
		return targetSchema;
	}

	@Override
	public int getLimitCount() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setSourceSchema(SchemaTree sourceSchema) {
		this.sourceSchema = sourceSchema;
	}

	public void setTargetSchema(SchemaTree targetSchema) {
		this.targetSchema = targetSchema;
	}

	@Override
	public Map<String, Symbol> getSymbolTable() {
		return symbolTable;
	}

	@Override
	public List<Function> getAggregationFunctions() {
		return aggregationFunctions;
	}
	
	public void setFilter(Expression filter) {
		this.filter = filter;
	}
}
