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
	
	SchemaTree sourceSchemaTree;
	SchemaTree targetSchemaTree;

	List<AggFunction> aggregationFunctions;
	Query parent;
	int id;

	public QueryImpl(int id) {
		tables = new LinkedList<Tablet>();
		subQueries = new LinkedList<dremel.compiler.Query>();
		selectExps = new LinkedList<Expression>();
		groupByExps = new LinkedList<Symbol>();
		orderByExps = new LinkedList<Symbol>();
		symbolTable = new HashMap<String, dremel.compiler.Expression.Symbol>();
		aggregationFunctions = new LinkedList<AggFunction>();
		parent = null;
		this.id = id;
	}

	public QueryImpl(int id, Query parent) {
		tables = new LinkedList<Tablet>();
		subQueries = new LinkedList<dremel.compiler.Query>();
		selectExps = new LinkedList<Expression>();
		groupByExps = new LinkedList<Symbol>();
		orderByExps = new LinkedList<Symbol>();
		symbolTable = new HashMap<String, dremel.compiler.Expression.Symbol>();
		aggregationFunctions = new LinkedList<AggFunction>();
		this.parent = parent;
		this.id = id;
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
	public SchemaColumnar getSourceSchemaColumnar() {
		return sourceSchema;
	}

	@Override
	public SchemaColumnar getTargetSchemaColumnar() {
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

	@Override
	public Query getParent() {
		return parent;
	}

	@Override
	public int getID() {
		return this.id;
	}
	
	@Override
	public String getStringID() {
		return "QUERY"+this.id;
	}

	@Override
	public SchemaTree getSourceSchemaTree() {
		return sourceSchemaTree;
	}

	@Override
	public SchemaTree getTargetSchemaTree() {
		return targetSchemaTree;
	}
	
	public void setSourceSchemaTree(SchemaTree sourceSchema) {
		this.sourceSchemaTree = sourceSchema;
	}

	public void setTargetSchemaTree(SchemaTree targetSchema) {
		this.targetSchemaTree = targetSchema;
	}

}
