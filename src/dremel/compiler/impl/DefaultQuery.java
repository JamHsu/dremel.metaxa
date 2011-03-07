package dremel.compiler.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dremel.compiler.Expression;
import dremel.compiler.Query;
import dremel.compiler.expression.Function;
import dremel.compiler.expression.Symbol;
import dremel.dataset.SchemaTree;
import dremel.dataset.Table;

/**
 * @author nhsan
 *
 */
public class DefaultQuery implements Query {
	List<Table> tables;
	List<dremel.compiler.Query> subQueries;
	List<Expression> selectExps;
	List<Expression> groupByExps;
	List<Expression> orderByExps;
	Map<String, Symbol> symbolTable;
	Expression filter;
	int limit;
	SchemaTree sourceSchema;
	SchemaTree targetSchema;
	List<Function> aggregationFunctions;

	public DefaultQuery() {
		tables = new LinkedList<Table>();
		subQueries = new LinkedList<dremel.compiler.Query>();
		selectExps = new LinkedList<Expression>();
		groupByExps = new LinkedList<Expression>();
		orderByExps = new LinkedList<Expression>();
		symbolTable = new HashMap<String, Symbol>();
		aggregationFunctions=new LinkedList<Function>();
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
	public List<Expression> getGroupByExpressions() {
		return groupByExps;
	}

	@Override
	public List<Expression> getOrderByExpressions() {
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
}
