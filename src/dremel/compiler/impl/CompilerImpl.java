package dremel.compiler.impl;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.apache.avro.Schema;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IScriptEvaluator;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Node;
import dremel.compiler.Expression.ReturnType;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Compiler;
import dremel.compiler.ParserTest;
import dremel.compiler.Query;
import dremel.compiler.impl.Expression.AggFunction;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;
import dremel.compiler.parser.impl.BqlParser;
import dremel.dataset.SchemaTree;
import dremel.dataset.Slice;
import dremel.dataset.Table;
import dremel.dataset.impl.SchemaTreeImpl;
import dremel.dataset.impl.SchemaTreeLoader;
import dremel.dataset.impl.SimpleSchemaTreeImpl;
import dremel.dataset.impl.SimpleSchemaTreeImpl.DataType;
import dremel.dataset.impl.SimpleSchemaTreeImpl.NodeType;
import dremel.executor.Executor;
import dremel.executor.Executor.Script;
import dremel.executor.impl.MetaxaExecutor;
import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.TabletIterator;
import dremel.tableton.ColumnMetaData.ColumnType;
import dremel.tableton.ColumnMetaData.EncodingType;
import dremel.tableton.impl.ColumnWriterImpl;
import dremel.tableton.impl.SchemaColumnarImpl;
import dremel.tableton.impl.SimpleIntColumnWriter;
import dremel.tableton.impl.TabletImpl;

/**
 * @author nhsan
 * 
 */
public class CompilerImpl implements dremel.compiler.Compiler {

	static int gid = 0;

	public static int getNextId() {
		return gid++;
	}

	public static String idNode2String(AstNode node) {
		StringBuilder ret = new StringBuilder();
		assert (node.getType() == BqlParser.N_ID);
		assert (node.getChildCount() >= 1);
		AstNode node2 = (AstNode) node.getChild(0);
		assert (node2.getType() == BqlParser.N_NAME);
		assert (node2.getChildCount() == 1);
		ret.append(node2.getChild(0).toString());

		for (int i = 1; i < node.getChildCount(); i++) {
			node2 = (AstNode) node.getChild(i);
			assert (node2.getType() == BqlParser.N_NAME);
			assert (node2.getChildCount() == 1);
			ret.append(".");
			ret.append(node2.getChild(0).toString());
		}
		return ret.toString();
	}

	@Override
	public Query parse(AstNode root) {
		Query query = new QueryImpl(getNextId());
		parseSelectStatement(root, query);
		analyse(query);
		return query;
	}
	
	private Query parse(AstNode root, Query parent) {
		Query query = new QueryImpl(getNextId(), parent);
		parseSelectStatement(root, query);
		analyse(query);
		return query;
	}
	

	void parseSelectStatement(AstNode node, Query query) {
		assert (node.getType() == BqlParser.N_SELECT_STATEMENT);
		int count = node.getChildCount();
		assert (count >= 2);
		parseFromClause((AstNode) node.getChild(0), query);
		parseSelectClause((AstNode) node.getChild(1), query);
		int curNode = 2;
		if (node.getChild(2) != null && node.getChild(2).getType() == BqlParser.N_WHERE) {
			parseWhereClause((AstNode) node.getChild(2), query);
			curNode++;
		}

		if (node.getChild(curNode) != null && node.getChild(curNode).getType() == BqlParser.N_GROUPBY) {
			parseGroupBy((AstNode) node.getChild(curNode), query);
		}
	}

	void parseFromClause(AstNode node, Query query) {
		assert (node.getType() == BqlParser.N_FROM);
		int count = node.getChildCount();
		assert (count > 0);
		for (int i = 0; i < count; i++) {
			AstNode node2 = (AstNode) node.getChild(i);
			if (node2.getType() == BqlParser.N_TABLE_NAME) {

				assert (node2.getChildCount() == 1);
				AstNode node3 = (AstNode) node2.getChild(0);
				List<Tablet> tables = query.getTables();
				tables.add(getTablet(node3.getText())); // get tablet
			} else if (node2.getType() == BqlParser.N_SELECT_STATEMENT) {
				List<dremel.compiler.Query> queries = query.getSubQueries();
				queries.add(parse(node2, query));
			} else
				assert (false);
		}
	}

	void parseSelectClause(AstNode node, Query query) {
		assert (node.getType() == BqlParser.N_SELECT);
		int count = node.getChildCount();
		assert (count > 0);
		for (int i = 0; i < count; i++) {
			parseCreateColumn((AstNode) node.getChild(i), query);
		}
	}

	void parseWhereClause(AstNode node, Query query) {
		if (node == null) {
			((QueryImpl) query).setFilter(null);
			return;
		}
		assert (node.getType() == BqlParser.N_WHERE);
		int count = node.getChildCount();
		assert (count == 1);
		Node filterNode = Expression.buildNode((AstNode) node.getChild(0), query);
		dremel.compiler.impl.Expression f = new dremel.compiler.impl.Expression();
		f.setRoot(filterNode);
		((QueryImpl) query).setFilter(f);
	}

	void parseCreateColumn(AstNode node, Query query) {
		StringBuffer alias = new StringBuffer();
		StringBuffer within = new StringBuffer();
		boolean isWithinRecord = false;
		boolean hasAlias = false;
		assert (node.getType() == BqlParser.N_CREATE_COLUMN);
		int count = node.getChildCount();
		assert ((count >= 1) && (count <= 3));
		if (count == 3) {
			parseColumnAlias((AstNode) node.getChild(1), alias);
			hasAlias = true;
			if (node.getChild(2).getType() == BqlParser.N_WITHIN)
				parseWithinClause((AstNode) node.getChild(2), within);
			else if (node.getChild(2).getType() == BqlParser.N_WITHIN_RECORD)
				isWithinRecord = parseWithinRecordClause((AstNode) node.getChild(2), query);
		} else if (count == 2) {
			if (node.getChild(1).getType() == BqlParser.N_ALIAS) {
				hasAlias = true;
				parseColumnAlias((AstNode) node.getChild(1), alias);
			} else if (node.getChild(1).getType() == BqlParser.N_WITHIN)
				parseWithinClause((AstNode) node.getChild(1), within);
			else if (node.getChild(1).getType() == BqlParser.N_WITHIN_RECORD)
				isWithinRecord = parseWithinRecordClause((AstNode) node.getChild(1), query);
			else {
				assert (false);
			}
		} else if (node.getChild(0).getChildCount() == 1 && node.getChild(0).getChild(0).getType() == BqlParser.N_ID) {
			alias.append(idNode2String((AstNode) node.getChild(0).getChild(0)));
		}

		assert (node.getChild(0).getType() == BqlParser.N_EXPRESSION);
		Node root = Expression.buildNode((AstNode) node.getChild(0), query);
		dremel.compiler.impl.Expression expression = new dremel.compiler.impl.Expression();

		if (isWithinRecord)
			expression.setScope("RECORD");
		else if (within.toString().length() > 0)
			expression.setScope(within.toString());
		else
			expression.setScope(null);

		if (hasAlias) {
			String aStr = alias.toString();
			expression.setAlias(aStr);
			assert (!query.getSymbolTable().containsKey(aStr));
			Symbol symbol = new dremel.compiler.impl.Expression.Symbol(aStr, query);
			symbol.setReference(expression);
			query.getSymbolTable().put(aStr, symbol);
		} else if (alias.toString().length() > 0) {
			expression.setAlias(alias.toString());
		}

		expression.setRoot(root);
		query.getSelectExpressions().add(expression);
	};

	private boolean parseWithinRecordClause(AstNode node, Query query) {
		assert (node.getType() == BqlParser.N_WITHIN_RECORD);
		return true;
	}

	private void parseWithinClause(AstNode node, StringBuffer within) {
		assert (node.getType() == BqlParser.N_WITHIN);
		int count = node.getChildCount();
		assert ((count == 1));
		within.append(idNode2String((AstNode) node.getChild(0)));
	}

	private void parseColumnAlias(AstNode node, StringBuffer alias) {
		assert (node.getType() == BqlParser.N_ALIAS);
		int count = node.getChildCount();
		assert ((count == 1));
		node = (AstNode) node.getChild(0);
		assert (node.getType() == BqlParser.N_ID);
		alias.append(idNode2String((AstNode) node));
	}

	private void parseGroupBy(AstNode node, Query query) {
		if (node == null)
			return;
		assert (node.getType() == BqlParser.N_GROUPBY);
		int count = node.getChildCount();
		assert ((count == 1)); // support for one integer field only
		node = (AstNode) node.getChild(0);
		assert (node.getType() == BqlParser.N_ID);
		String colName = idNode2String((AstNode) node);
		if (query.getSymbolTable().containsKey(colName)) {
			Symbol s = query.getSymbolTable().get(colName);
			query.getGroupByExpressions().add(s);
		} else {
			Symbol s = new dremel.compiler.impl.Expression.Symbol(colName, query);
			query.getGroupByExpressions().add(s);
		}
	}

	/**
	 * calculate repetition level of an expression = max repetition level of
	 * fields used in expression
	 * 
	 * @param node
	 * @param level
	 * @param maxLevels
	 * @return
	 */
	int getRepLevel(Node node, int level) {
		if (node instanceof Symbol) {
			Symbol symbol = (Symbol) node;
			Object o = symbol.getReference();
			if (o instanceof SchemaTree) {
				SchemaTree t = (SchemaTree) o;
				int l = t.getRepLevel();
				if (l > level)
					level = l;
			} else if (o instanceof Expression) {
				Expression exp = (Expression) o;
				int l = exp.getRepetitionLevel();
				if (l > level)
					level = l;
			}
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				Node n = node.getChild(i);
				level = getRepLevel(n, level);
			}
		}
		return level;
	}

	int getDefLevel(Node node, int level) {
		if (node instanceof Symbol) {
			Symbol symbol = (Symbol) node;
			Object o = symbol.getReference();
			if (o instanceof SchemaTree) {
				SchemaTree t = (SchemaTree) o;
				int l = t.getDefLevel();
				if (l > level)
					level = l;
			} else if (o instanceof Expression) {
				Expression exp = (Expression) o;
				int l = exp.getDefinitionLevel();
				if (l > level)
					level = l;
			}
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				Node n = node.getChild(i);
				level = getDefLevel(n, level);
			}
		}
		return level;
	}

	void getFieldList(SchemaTree node, List<SchemaTree> fieldList) {
		List<SchemaTree> fs = node.getFieldsList();
		for (int i = 0; i < fs.size(); i++) {
			SchemaTree d = fs.get(i);
			fieldList.add(d);
			if (d.isRecord()) {
				getFieldList(d, fieldList);
			}
		}
	}

	void getRelatedFields(Node node, List<Symbol> symbols) {
		if (node instanceof Symbol) {
			Symbol symbol = (Symbol) node;
			Object o = symbol.getReference();
			if (o instanceof SchemaTree) {
				if (!symbols.contains(symbol))
					symbols.add(symbol);
			} else if (o instanceof Expression) {
				Expression exp = (Expression) o;
				List<Symbol> lst = exp.getSymbols();
				Iterator<Symbol> it = lst.iterator();

				while (it.hasNext()) {
					Symbol d = it.next();
					if (!symbols.contains(d))
						symbols.add(d);
				}
			}
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				Node n = node.getChild(i);
				getRelatedFields(n, symbols);
			}
		}
	}

	/**
	 * get aggregation function list of an expression
	 * 
	 * @param node
	 * @param aggFuncs
	 */
	void getAggregationFunction(Node node, List<AggFunction> aggFuncs) {
		if (node instanceof Function) {
			Function func = (Function) node;
			if (func instanceof AggFunction)
				aggFuncs.add((AggFunction) func);
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				Node n = node.getChild(i);
				getAggregationFunction(n, aggFuncs);
			}
		}
	}

	/**
	 * resolve within node and return level of within node
	 * 
	 * @param nodeName
	 * @param maxLevels
	 * @return
	 */
	int getWithinLevel(String nodeName, List<SchemaTree> fieldList) {
		if (nodeName == null)
			return -1;
		if (nodeName.equalsIgnoreCase("record"))
			return 0;

		Iterator<SchemaTree> it = fieldList.iterator();

		while (it.hasNext()) {
			SchemaTree d = it.next();

			if (d.isRecord()) // within node must be group
			{
				String name = d.getName();
				if (name.equalsIgnoreCase(nodeName))
					return d.getRepLevel();
			}
		}
		return -1;
	}

	/*
	 * Validation rules: - check table name: enclose by [] - check field names
	 * against schema (field name in expressions + WITHIN clause) - check alias:
	 * alias ~ expression in SELECT clause, can be used in order by, group by,
	 * and where clause - check WITHIN clause: must contains aggregation
	 * function, field in aggregation function must be child of WITHIN node -
	 * check data type of expression - check WHERE clause: Aggregate functions
	 * cannot be used in the WHERE clause - check GROUPBY clause: Non-aggregated
	 * fields in the SELECT clause must be listed in the GROUP BY clause - check
	 * GROUPBY clause: Fields in the GROUP BY clause must be listed in the
	 * SELECT clause - and more...
	 */

	/*
	 * Analyze rule: - build symbol table: fields + alias - find related field
	 * list (ordered, will be order in slice) - calculate repetition level for
	 * expressions, within nodes, aggregation functions
	 */

	@Override
	public void analyse(Query query) {
		// assert (query.getTables().size() == 1);// one table only
		// assert (query.getSubQueries().size() == 0);// no sub-queries

		// TODO: validation of schema (compare schema of tables and sub-queries)

		if (query.getTables().size() >= 1) {
			SchemaTree schemaTree = query.getTables().get(0).getSchemaTree();
			((QueryImpl) query).setSourceSchemaTree(schemaTree);
		} else if (query.getSubQueries().size() >= 1) {
			SchemaTree schemaTree = query.getSubQueries().get(0).getTargetSchemaTree();
			((QueryImpl) query).setSourceSchemaTree(schemaTree);
		}

		assert (query.getSourceSchemaTree() != null);

		List<SchemaTree> fieldList = new LinkedList<SchemaTree>();
		getFieldList(query.getSourceSchemaTree(), fieldList);

		Iterator<SchemaTree> fIt = fieldList.iterator();
		while (fIt.hasNext()) {
			SchemaTree d = fIt.next();
			String name = d.getFullName();

			Symbol symbol = query.getSymbolTable().get(name.toLowerCase());
			if (symbol != null) {
				if (symbol.getReference() == null) {
					symbol.setReference(d);
				} else {
					assert (symbol.getReference() == d);// no duplicate symbol
				}
			}
		}

		Iterator<Symbol> it = query.getSymbolTable().values().iterator();
		while (it.hasNext()) {
			Symbol symbol = it.next();
			assert (symbol.getReference() != null);// no symbol without
													// reference
		}

		Iterator eIt = query.getSelectExpressions().iterator();
		while (eIt.hasNext()) {
			Expression exp = (Expression) eIt.next();
			int level = getRepLevel(exp.getRoot(), 0);
			exp.setRLevel(level);
			level = getDefLevel(exp.getRoot(), 0);
			exp.setDLelel(level);
			getAggregationFunction(exp.getRoot(), query.getAggregationFunctions());
			int scopeLevel = getWithinLevel(exp.getWithin(), fieldList);
			exp.setWithinLevel(scopeLevel);
			getRelatedFields(exp.getRoot(), exp.getSymbols());

			assert (exp.getReturnType() != ReturnType.INVALID);
		}

		Expression exp = (Expression) query.getFilter();
		if (exp != null) {
			int level = getRepLevel(exp.getRoot(), 0);
			exp.setRLevel(level);
			getAggregationFunction(exp.getRoot(), query.getAggregationFunctions());
			assert (exp.getReturnType() == ReturnType.BOOL); // filter must be
																// logical
																// expression
		}
		SchemaColumnar resultSchema = generateResultSchemaColumnar(query);
		((QueryImpl) query).setTargetSchema(resultSchema);

		SchemaTree outSchemaTree = generateResultSchemaTree(query);
		((QueryImpl) query).setTargetSchemaTree(outSchemaTree);
		System.out.println(outSchemaTree.toString());
	}

	public SchemaColumnar generateResultSchemaColumnar(Query query) {
		ColumnType type = ColumnType.INT;
		SchemaColumnar schema = new SchemaColumnarImpl();
		for (dremel.compiler.Expression exp : query.getSelectExpressions()) {
			if (exp.isTypeFloat())
				type = ColumnType.FLOAT;
			else if (exp.isTypeString())
				type = ColumnType.STRING;
			ColumnMetaData metaData = new ColumnMetaData(exp.getJavaName(), type, EncodingType.NONE, "testdata/out_" + exp.getJavaName(), (byte) exp.getRepetitionLevel(), (byte) exp.getDefinitionLevel());
			schema.addColumnMetaData(metaData);
		}
		return schema;
	}

	void copySchemaStructure(SchemaTree in, SchemaTree out, Map<SchemaTree, SchemaTree> schemaMapping) {
		assert (in.isRecord());
		schemaMapping.put(in, out);
		for (SchemaTree field : in.getFieldsList()) {
			if (field.isRecord()) {
				// System.out.println(field.getName());
				SchemaTree fieldOut = null;
				if (field.isRepeated()) {
					fieldOut = new SimpleSchemaTreeImpl(out, field.getName(), DataType.RECORD, NodeType.REPEATED);
				} else if (field.isOptional()) {
					fieldOut = new SimpleSchemaTreeImpl(out, field.getName(), DataType.RECORD, NodeType.OPTIONAL);
				} else if (field.isRequired()) {
					// System.out.println(field.getName());
					fieldOut = new SimpleSchemaTreeImpl(out, field.getName(), DataType.RECORD, NodeType.REQUIRED);
				}
				copySchemaStructure(field, fieldOut, schemaMapping);
			}
		}
	}

	public SchemaTree generateResultSchemaTree(Query query) {
		
		Map<dremel.compiler.Expression, SchemaTree> resultParent = new HashMap<dremel.compiler.Expression, SchemaTree>();
		
		for (dremel.compiler.Expression exp : query.getSelectExpressions()) {
			List<SchemaTree> fields = new LinkedList<SchemaTree>();
			for (Symbol s: exp.getSymbols())
			{
				assert (s.getReference() instanceof SchemaTree);
				SchemaTree f = (SchemaTree) s.getReference();
				fields.add(f);
			}
			SchemaTree parent = SimpleSchemaTreeImpl.commonAncestor(fields);
			resultParent.put(exp, parent);
		}
		
		Map<SchemaTree, SchemaTree> schemaMapping = new HashMap<SchemaTree, SchemaTree>();
		
		SchemaTree inSchema = query.getSourceSchemaTree();
		SchemaTree outSchema = new SimpleSchemaTreeImpl(null, query.getStringID(), DataType.RECORD, NodeType.REQUIRED);

		copySchemaStructure(inSchema, outSchema, schemaMapping);
		
		for (dremel.compiler.Expression exp : query.getSelectExpressions())
		{
			SchemaTree t = schemaMapping.get(resultParent.get(exp));
			resultParent.put(exp, t);
		}

		for (dremel.compiler.Expression exp : query.getSelectExpressions()) {

			NodeType ntype = NodeType.REQUIRED; // required
			DataType dtype = DataType.INT;
			String name = "";
			SchemaTree parent = resultParent.get(exp);
			//SchemaTree parent = outSchema;

			// node type: {REPEATED, OPTIONAL, REQUIRED}

			if (exp.getWithin() != null) // scoped aggregation
			{
				ntype = NodeType.REQUIRED;
			} else {
				for (Symbol sym : exp.getSymbols()) {
					assert (sym.isColumnID());
					SchemaTree field = (SchemaTree) sym.getReference();
					assert (!field.isRecord());
					if (field.isRepeated()) {
						ntype = NodeType.REPEATED;
						break;
					} else if (field.isOptional()) {
						ntype = NodeType.OPTIONAL;
					}
				}
			}

			// data type
			if (exp.isTypeBool())
				dtype = DataType.BOOLEAN;
			else if (exp.isTypeString())
				dtype = DataType.STRING;
			else if (exp.isTypeFloat())
				dtype = DataType.FLOAT;

			// parent and name
			
			if (exp.getAlias() != null) {
				
				String[] lst = exp.getAlias().split("\\.");
//				for (int i = 0; i < lst.length - 1; i++) {
//					List<SchemaTree> fieldList = parent.getFieldsList();
//					SchemaTree newParent = parent;
//					for (SchemaTree f : fieldList) {
//						if (f.getName().equalsIgnoreCase(lst[i])) {
//							newParent = f;
//							break;
//						}
//					}
//					assert (newParent != parent);
//					parent = newParent;
//				}
				name = lst[lst.length - 1];
			} else {
				// put this field as child of root0
				name = exp.getJavaName();
			}

			new SimpleSchemaTreeImpl(parent, name, dtype, ntype);
		}

		return outSchema;
	}

	@Override
	public Executor compile(Query query) {
		return null;
	}

	void getQueryList(Query query, List<Query> queries) {

		for (Query q : query.getSubQueries()) {
			getQueryList(q, queries);
		}
		assert (query.getTables().size() + query.getSubQueries().size() == 1);
		queries.add(query);
	}

	Tablet getTablet(Query query) {
		if (query.getTables().size() == 1)
			return query.getTables().get(0);

		for (Query q : query.getSubQueries()) {
			return getTablet(q);
		}
		return null;
	}

	@Override
	public String compileToScript(Query query) {
		VelocityContext context = new VelocityContext();

		try {
			Template template = null;
			if (query.getSubQueries().size() == 0) {
				context.put("query", query);
				if (query.getAggregationFunctions().size() == 0)
					template = Velocity.getTemplate("src/dremel/executor/stna_executor.vm");
				else if (query.getGroupByExpressions().size() == 0) {
					template = Velocity.getTemplate("src/dremel/executor/stwa_executor3.vm");
				} else {
					throw new RuntimeException("Not support query type");
				}
			} else {
				List<Query> queries = new LinkedList<Query>();
				getQueryList(query, queries);
				context.put("queries", queries);
				template = Velocity.getTemplate("src/dremel/executor/query_main.vm");
			}

			StringWriter sw = new StringWriter();
			template.merge(context, sw);

			return sw.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	Tablet getTablet(String name) {
		if (name.equalsIgnoreCase("[document]")) {
			return SimpleSchemaTreeImpl.getPaperSchemaTablet();
		} else
			throw new RuntimeException("Can not get tablet");
	}

	public static void main(String[] args) throws Exception {

		CompilerImpl compiler = new CompilerImpl();
		SimpleSchemaTreeImpl.buildPaperSchema(1);

//		 AstNode nodes =
//		 Parser.parseBql("SELECT \ndocid, links.forward, links.backward, links.backward+\ndocid, \ndocid+links.forward, links.forward+links.backward, 3+2 FROM [document] where \ndocid>0 and links.forward>30");

//		 AstNode nodes = Parser
//		 .parseBql("SELECT \ndocid, count(docid) within record as c_id, links.forward as exp3, sum(links.forward) within links, links.backward, count(links.backward) within record, 2*3+5 FROM [document] where \ndocid>0 and links.forward>30");

		AstNode nodes = Parser.parseBql("SELECT \ndocid, count(links.forward) as count_fwd,  \tdocid+links.backward FROM (SELECT \tdocid, links.forward, links.backward FROM [document]) WHERE \ndocid>0");

		final Query query = compiler.parse(nodes);
		//String code = compiler.compileToScript(query);
		//MetaxaExecutor executor = new MetaxaExecutor(query, code);
		// executor.execute();
	}
}
