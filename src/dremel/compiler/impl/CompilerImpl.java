package dremel.compiler.impl;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
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
import dremel.compiler.Query;
import dremel.compiler.impl.Expression.AggFunction;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;
import dremel.compiler.parser.impl.BqlParser;
import dremel.dataset.SchemaTree;
import dremel.dataset.Slice;
import dremel.dataset.Table;
import dremel.dataset.impl.SchemaTreeLoader;
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
import dremel.tableton.impl.TabletImpl;

/**
 * @author nhsan
 * 
 */
public class CompilerImpl implements dremel.compiler.Compiler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see dremel.compiler.Compiler#parse(dremel.parser.AstNode)
	 */
	@Override
	public Query parse(AstNode root) {
		Query query = new QueryImpl();
		parseSelectStatement(root, query);
		return query;
	}

	public static String getID(AstNode node) {
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

	void parseSelectStatement(AstNode node, Query query) {
		assert (node.getType() == BqlParser.N_SELECT_STATEMENT);
		int count = node.getChildCount();
		assert (count >= 2);
		parseFromClause((AstNode) node.getChild(0), query);
		parseSelectClause((AstNode) node.getChild(1), query);
		parseWhereClause((AstNode) node.getChild(2), query);
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
				queries.add(parse(node2));
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
			alias.append(getID((AstNode) node.getChild(0).getChild(0)));
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
		within.append(getID((AstNode) node.getChild(0)));
	}

	private void parseColumnAlias(AstNode node, StringBuffer alias) {
		assert (node.getType() == BqlParser.N_ALIAS);
		int count = node.getChildCount();
		assert ((count == 1));
		node = (AstNode) node.getChild(0);
		assert (node.getType() == BqlParser.N_NAME);
		assert (node.getChildCount() == 1);
		alias.append(node.getChild(0).getText());
	}

	/**
	 * calMaxLevel is a recursive function to calculate repetition level of
	 * fields in schema
	 * 
	 * @param desc
	 * @param rlevel
	 * @param maxRLevels
	 */
	private void calMaxRLevel(SchemaTree desc, int rlevel, Map<SchemaTree, Integer> maxRLevels) {
		List<SchemaTree> fs = desc.getFieldsList();
		for (int i = 0; i < fs.size(); i++) {
			SchemaTree d = fs.get(i);
			if (d.isRepeated()) {

				if (d.isRecord()) {
					calMaxRLevel(d, rlevel + 1, maxRLevels);
					maxRLevels.put(d, rlevel + 1);
				} else {
					maxRLevels.put(d, rlevel + 1);
				}
			} else {
				if (d.isRecord()) {
					calMaxRLevel(d, rlevel, maxRLevels);
					maxRLevels.put(d, rlevel);
				} else {
					maxRLevels.put(d, rlevel);
				}
			}
		}
	}

	private void calMaxDLevel(SchemaTree desc, int dlevel, Map<SchemaTree, Integer> maxDLevels) {
		List<SchemaTree> fs = desc.getFieldsList();
		for (int i = 0; i < fs.size(); i++) {
			SchemaTree d = fs.get(i);
			if (d.isRepeated()) {

				if (d.isRecord()) {
					calMaxRLevel(d, dlevel + 1, maxDLevels);
					maxDLevels.put(d, dlevel + 1);
				} else {
					maxDLevels.put(d, dlevel + 1);
				}
			} else {
				if (d.isRecord()) {
					calMaxRLevel(d, dlevel, maxDLevels);
					maxDLevels.put(d, dlevel);
				} else {
					maxDLevels.put(d, dlevel);
				}
			}
		}
	}

	/**
	 * calculate repetition level of an expression= max repetition level of
	 * fields used in expression
	 * 
	 * @param node
	 * @param level
	 * @param maxLevels
	 * @return
	 */
	int getRLevel(Node node, int level, Map<SchemaTree, Integer> maxLevels) {
		if (node instanceof Symbol) {
			Symbol symbol = (Symbol) node;
			Object o = symbol.getReference();
			if (o instanceof SchemaTree) {
				int l = maxLevels.get(o);
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
				level = getRLevel(n, level, maxLevels);
			}
		}
		return level;
	}

	int getDLevel(Node node, int level, Map<SchemaTree, Integer> maxLevels) {
		if (node instanceof Symbol) {
			Symbol symbol = (Symbol) node;
			Object o = symbol.getReference();
			if (o instanceof SchemaTree) {
				int l = maxLevels.get(o);
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
				level = getDLevel(n, level, maxLevels);
			}
		}
		return level;
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
	int getWithinLevel(String nodeName, Map<SchemaTree, Integer> maxLevels) {
		if (nodeName == null)
			return -1;
		if (nodeName.equalsIgnoreCase("record"))
			return 0;

		Iterator<SchemaTree> it = maxLevels.keySet().iterator();

		while (it.hasNext()) {
			SchemaTree d = it.next();

			if (d.isRecord()) // within node must be group
			{
				String name = d.getName();
				if (name.equalsIgnoreCase(nodeName))
					return maxLevels.get(d);
			}
		}
		return -1;
	}

	SchemaTree getCommonRoot(List<SchemaTree> fields) {
		return null;
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
		assert (query.getTables().size() == 1);// one table only
		assert (query.getSubQueries().size() == 0);// no sub-queries
		SchemaTree SchemaTree = query.getTables().get(0).getSchemaTree();
		Map<SchemaTree, Integer> maxRLevels = new HashMap<SchemaTree, Integer>();
		Map<SchemaTree, Integer> maxDLevels = new HashMap<SchemaTree, Integer>();

		// bind field+exp to symbols
		calMaxRLevel(SchemaTree, 0, maxRLevels);
		calMaxDLevel(SchemaTree, 0, maxDLevels);
		Iterator<SchemaTree> fIt = maxRLevels.keySet().iterator();
		while (fIt.hasNext()) {
			SchemaTree d = fIt.next();
			String name = d.getName();

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
			int level = getRLevel(exp.getRoot(), 0, maxRLevels);
			exp.setRLevel(level);
			level = getDLevel(exp.getRoot(), 0, maxDLevels);
			exp.setDLelel(level);
			getAggregationFunction(exp.getRoot(), query.getAggregationFunctions());
			int scopeLevel = getWithinLevel(exp.getWithin(), maxRLevels);
			exp.setWithinLevel(scopeLevel);
			getRelatedFields(exp.getRoot(), exp.getSymbols());

			assert (exp.getReturnType() != ReturnType.INVALID);
		}

		Expression exp = (Expression) query.getFilter();
		if (exp != null) {
			int level = getRLevel(exp.getRoot(), 0, maxRLevels);
			exp.setRLevel(level);
			getAggregationFunction(exp.getRoot(), query.getAggregationFunctions());
			assert (exp.getReturnType() == ReturnType.BOOL); // filter must be
																// logical
																// expression
		}
		SchemaColumnar resultSchema = generateResultSchema(query);
		((QueryImpl) query).setTargetSchema(resultSchema);
	}

	public SchemaColumnar generateResultSchema(Query query) {
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

	@Override
	public Executor compile(Query query) {
		return null;
	}

	@Override
	public String compileToScript(Query query) {
		VelocityContext context = new VelocityContext();

		try {
			context.put("query", query);
			context.put("sourceTablet", query.getTables().get(0));

			Template template = null;

			try {
				if (query.getAggregationFunctions().size() == 0)
					template = Velocity.getTemplate("src/dremel/executor/stna_executor.vm");
				else if (query.getGroupByExpressions().size() == 0) {
					template = Velocity.getTemplate("src/dremel/executor/stwa_executor3.vm");
				} else
					throw new RuntimeException("Not support query type");
			} catch (Exception e) {
				e.printStackTrace();
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
			return getPaperSchemaTablet();

		} else
			throw new RuntimeException("Can not get tablet");
	}

	private void buildLinkBackwardData(ColumnMetaData columnMetaData) {
		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		columnBuilder.addIntDataTriple(0, ColumnReader.NULL, (byte) 0, (byte) 1);
		columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte) 0, (byte) 2);
		columnBuilder.addIntDataTriple(30, ColumnReader.NOT_NULL, (byte) 1, (byte) 2);

		columnBuilder.close();

	}

	private void buildLinksForwardData(ColumnMetaData columnMetaData) {
		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		columnBuilder.addIntDataTriple(20, ColumnReader.NOT_NULL, (byte) 0, (byte) 2);
		columnBuilder.addIntDataTriple(40, ColumnReader.NOT_NULL, (byte) 1, (byte) 2);
		columnBuilder.addIntDataTriple(60, ColumnReader.NOT_NULL, (byte) 1, (byte) 2);
		columnBuilder.addIntDataTriple(80, ColumnReader.NOT_NULL, (byte) 0, (byte) 2);

		columnBuilder.close();
	}

	private void buildDocIDData(ColumnMetaData docidMetaData) {

		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(docidMetaData);
		// write data
		columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte) 0, (byte) 0);
		columnBuilder.addIntDataTriple(20, ColumnReader.NOT_NULL, (byte) 0, (byte) 0);

		columnBuilder.close();

	}

	public Tablet getPaperSchemaTablet() {
		// build single column tablet for the input
		ColumnMetaData linksBackwardMetaData = new ColumnMetaData("Links.Backward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksBackward", (byte) 1, (byte) 2);
		buildLinkBackwardData(linksBackwardMetaData);

		ColumnMetaData linksForwardMetaData = new ColumnMetaData("Links.Forward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksForward", (byte) 1, (byte) 2);
		buildLinksForwardData(linksForwardMetaData);

		ColumnMetaData docidMetaData = new ColumnMetaData("DocId", ColumnType.INT, EncodingType.NONE, "testdata\\docid", (byte) 0, (byte) 0);
		buildDocIDData(docidMetaData);

		SchemaColumnar schema = new SchemaColumnarImpl();
		schema.addColumnMetaData(linksBackwardMetaData);
		schema.addColumnMetaData(linksForwardMetaData);
		schema.addColumnMetaData(docidMetaData);
		SchemaTree schemaTree = SchemaTreeLoader.loadSchema("[document]");
		Tablet tablet = new TabletImpl(schemaTree, schema);

		return tablet;
	}

	public static void main(String[] args) throws Exception {

//		 AstNode nodes = Parser.parseBql("SELECT \ndocid, links.forward, links.backward, links.backward+\ndocid, \ndocid+links.forward, links.forward+links.backward, 3+2 FROM [document] where \ndocid>0 and links.forward>30");
		AstNode nodes = Parser.parseBql("SELECT \ndocid, count(docid) within record, links.forward as exp3, sum(links.forward) within links, links.backward, count(links.backward) within record, 2*3+5 FROM [document] where \ndocid>0 and links.forward>30");
		CompilerImpl compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);
		String code = compiler.compileToScript(query);
		MetaxaExecutor executor = new MetaxaExecutor(query, code);
		executor.execute();
	}
}