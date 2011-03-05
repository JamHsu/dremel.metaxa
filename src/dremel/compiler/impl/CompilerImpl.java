package dremel.compiler.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IScriptEvaluator;

import dremel.compiler.ExpNode;
import dremel.compiler.Query;
import dremel.compiler.expression.Function;
import dremel.compiler.expression.Symbol;
import dremel.dataset.ISchemaTree;
import dremel.dataset.Slice;
import dremel.dataset.impl.SchemaTree;
import dremel.executor.Executor;

/**
 * @author nhsan
 * @author Constantine Peresypkin
 * 
 */
public class CompilerImpl implements dremel.compiler.Compiler {

	/**
	 * calMaxLevel is a recursive function to calculate repetition level of
	 * fields in schema
	 * 
	 * @param descriptor
	 * @param level
	 * @param maxLevels
	 */
	private void calMaxLevel(ISchemaTree descriptor, int level, Map<ISchemaTree, Integer> maxLevels) {
		List<ISchemaTree> fs = descriptor.getFieldsList();
		for (int i = 0; i < fs.size(); i++) {
			ISchemaTree fd = fs.get(i);
			if (fd.isRepeated()) {

				if (fd.isRecord()) {
					calMaxLevel(fd, level + 1, maxLevels);
					maxLevels.put(fd, level + 1);
				} else {
					maxLevels.put(fd, level + 1);
				}
			} else {
				if (fd.isRecord()) {
					calMaxLevel(fd, level, maxLevels);
					maxLevels.put(fd, level);
				} else {
					maxLevels.put(fd, level);
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
	int getRLevel(ExpNode node, int level, Map<ISchemaTree, Integer> maxLevels) {
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
				ExpNode n = node.getChild(i);
				level = getRLevel(n, level, maxLevels);
			}
		}
		return level;
	}

	void getRelatedFields(ExpNode node, List<FieldDescriptor> fields) {
		if (node instanceof Symbol) {
			Symbol symbol = (Symbol) node;
			Object o = symbol.getReference();
			if (o instanceof FieldDescriptor) {
				if (!fields.contains(o))
					fields.add((FieldDescriptor) o);
			} else if (o instanceof Expression) {
				Expression exp = (Expression) o;
				List<FieldDescriptor> lst = exp.getRelatedFields();
				Iterator<FieldDescriptor> it = lst.iterator();

				while (it.hasNext()) {
					FieldDescriptor d = it.next();
					if (!fields.contains(d))
						fields.add(d);
				}
			}
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				ExpNode n = node.getChild(i);
				getRelatedFields(n, fields);
			}
		}
	}

	/**
	 * get aggregation function list of an expression
	 * 
	 * @param node
	 * @param aggFuncs
	 */
	void getAggregationFunction(ExpNode node, List<Function> aggFuncs) {
		if (node instanceof Function) {
			Function func = (Function) node;
			if (func.getFunctionName().equalsIgnoreCase("count") || func.getFunctionName().equalsIgnoreCase("sum"))
				aggFuncs.add(func);
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				ExpNode n = node.getChild(i);
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
	int getWithinLevel(String nodeName, Map<ISchemaTree, Integer> maxLevels) {
		if (nodeName == null)
			return -1;
		if (nodeName.equalsIgnoreCase("record"))
			return 0;

		Iterator<ISchemaTree> it = maxLevels.keySet().iterator();

		while (it.hasNext()) {
			ISchemaTree d = it.next();

			if (d.isRecord()) // within node must be group
			{
				//String name = getFieldName(d.getFullName());
				String name = d.getName();
				// System.out.println(name);
				if (name.equalsIgnoreCase(nodeName))
					return maxLevels.get(d);
			}
		}
		return -1;
	}

	String getFieldName(String name) {
		// input schema.Document.*
		// trim prefix: schema.Document.
		int p = name.indexOf('.');
		if (p > 0) {
			p = name.indexOf('.', p + 1);
			if (p > 0) {
				name = name.substring(p + 1);
			}
		}
		return name;
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
		ISchemaTree descriptor = query.getTables().get(0).getSchema();
		Map<ISchemaTree, Integer> maxLevels = new HashMap<ISchemaTree, Integer>();

		// bind field+exp to symbols
		calMaxLevel(descriptor, 0, maxLevels);
		Iterator<ISchemaTree> fIt = maxLevels.keySet().iterator();
		while (fIt.hasNext()) {
			ISchemaTree d = fIt.next();
			//String name = getFieldName(d.getFullName());
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
			int level = getRLevel(exp.getRoot(), 0, maxLevels);
			exp.setRLevel(level);
			getAggregationFunction(exp.getRoot(), query.getAggregationFunctions());
			int scopeLevel = getWithinLevel(exp.getWithin(), maxLevels);
			exp.setWithinLevel(scopeLevel);
			getRelatedFields(exp.getRoot(), exp.getRelatedFields());

			// Iterator<GenericFieldDesc> _it =
			// exp.getRelatedFields().iterator();
			// System.out.println(exp.getRoot().generateCode());
			// while (_it.hasNext())
			// {
			// GenericFieldDesc d= _it.next();
			// System.out.println("\t"+d.getFullName());
			// }
		}

		Expression exp = (Expression) query.getFilter();
		if (exp != null) {
			int level = getRLevel(exp.getRoot(), 0, maxLevels);
			exp.setRLevel(level);
			getAggregationFunction(exp.getRoot(), query.getAggregationFunctions());
		}
	}

	public String generateCode(Query query) {
		StringBuilder builder = new StringBuilder();
		Iterator<Symbol> it = query.getSymbolTable().values().iterator();
		builder.append("// INPUT: inSlice, outSlice, context1, context2\n");

		int i = 0;
		while (it.hasNext()) {
			Symbol symbol = it.next();
			if (symbol.getReference() instanceof FieldDescriptor) {
				symbol.setSliceMappingIndex(i);
				builder.append("// inSlice.getValue[" + (i++) + "] -> " + ((ISchemaTree) symbol.getReference()).getName() + "\n");

			}
		}
		builder.append("// context1[0] -> selectLevel \n");
		for (i = 0; i < query.getAggregationFunctions().size(); i++) {
			Function func = query.getAggregationFunctions().get(i);
			builder.append("// context1[" + (i + 1) + "] -> " + func.generateCode() + "\n");
		}

		Iterator<dremel.compiler.Expression> eIt = query.getSelectExpressions().iterator();
		i = 0;
		while (eIt.hasNext()) {
			dremel.compiler.Expression exp = eIt.next();
			if (exp.getAlias() != null) {
				builder.append("// outSlice[" + (i++) + "] = " + exp.getAlias() + " = " + exp.getRoot().generateCode() + "\n");
			} else {
				builder.append("// outSlice[" + (i++) + "] = " + exp.getRoot().generateCode() + "\n");
			}
		}
		builder.append("int selectLevel=context1[0];\n");
		builder.append("int fetchLevel=inSlice.getFetchLevel();\n");
		if (query.getFilter() != null) {
			dremel.compiler.Expression filter = query.getFilter();
			builder.append("if ((selectLevel <= " + filter.getRepetitionLevel() + ") && (" + filter.getRoot().generateCode() + ")) {\n");
			eIt = query.getSelectExpressions().iterator();
			i = 0;
			while (eIt.hasNext()) {
				dremel.compiler.Expression exp = eIt.next();
				builder.append("\tif(selectLevel<=" + exp.getRepetitionLevel() + ") outSlice.setValue(" + (i++) + "," + exp.getRoot().generateCode() + ");\n");
			}
			builder.append("\n\tcontext1[0] = fetchLevel;\n");
			builder.append("\n\toutSlice.setSelectLevel(fetchLevel);\n");
			builder.append("} else {\n\toutSlice.setNull(true);\n");
			builder.append("\tif(selectLevel > fetchLevel) context1[0]=fetchLevel;\n");
			builder.append("}\n");

		}
		return builder.toString();
	}

	public IScriptEvaluator createScript(String code) throws Exception {
		IScriptEvaluator se = CompilerFactoryFactory.getDefaultCompilerFactory().newScriptEvaluator();
		se.setReturnType(void.class);
		se.setDefaultImports(new String[] { "dremel.compiler.*", "dremel.compiler.expression.*" });

		se.setParameters(new String[] { "inSlice", "outSlice", "context1" }, new Class[] { Slice.class, Slice.class, Integer[].class });

		se.cook(code);
		return se;
	}

	@Override
	public Executor compile(Query query) {
/*		try {
			IScriptEvaluator se = compileToScript(query);
			//assert (query.getTables().get(0).getSchema() == Document.getDescriptor());
			SliceScanner scanner = new SimpleSliceScanner(fields, query.getTables().get(0).getDataDir());

			Executor executor = new MetaxaExecutor(query, scanner, se);
			return executor;
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		return null;
	}
	
	public static void main(String[] args) {
		
	}

	/* (non-Javadoc)
	 * @see dremel.compiler.Compiler#compileToScript(dremel.compiler.Query)
	 */
	@Override
	public IScriptEvaluator compileToScript(Query query) {
		
		List<FieldDescriptor> fields = new LinkedList<FieldDescriptor>();

		StringBuilder builder = new StringBuilder();
		try {
			Iterator<Symbol> it = query.getSymbolTable().values().iterator();
			int i = 0;
			while (it.hasNext()) {
				Symbol symbol = it.next();
				if (symbol.getReference() instanceof FieldDescriptor) {
					symbol.setSliceMappingIndex(i++);
					fields.add((FieldDescriptor) symbol.getReference());
				}
			}

			builder.append("int selectLevel=context1[0];\n");
			builder.append("int fetchLevel=inSlice.getFetchLevel();\n");
			if (query.getFilter() != null) {
				dremel.compiler.Expression filter = query.getFilter();
				builder.append("if ((selectLevel <= " + filter.getRepetitionLevel() + ") && (" + filter.getRoot().generateCode() + ")) {\n");
				Iterator<dremel.compiler.Expression> eIt = query.getSelectExpressions().iterator();
				i = 0;
				while (eIt.hasNext()) {
					dremel.compiler.Expression exp = eIt.next();
					builder.append("\tif(selectLevel<=" + exp.getRepetitionLevel() + ") outSlice.setValue(" + (i++) + "," + exp.getRoot().generateCode() + ");\n");
				}
				builder.append("\n\tcontext1[0] = fetchLevel;\n");
				builder.append("\n\toutSlice.setSelectLevel(fetchLevel);\n");
				builder.append("\n\toutSlice.setNull(false);\n");
				builder.append("} else {\n\toutSlice.setNull(true);\n");
				builder.append("\tif(selectLevel > fetchLevel) context1[0]=fetchLevel;\n");
				builder.append("}\n");

			}

			//System.out.println(builder.toString());
			IScriptEvaluator se = createScript(builder.toString());
			return se;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
