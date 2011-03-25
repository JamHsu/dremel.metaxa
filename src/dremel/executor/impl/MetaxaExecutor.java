package dremel.executor.impl;

import java.lang.reflect.InvocationTargetException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IScriptEvaluator;

import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Node;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Expression;
import dremel.compiler.Query;
import dremel.compiler.impl.Expression.AggFunction;
import dremel.dataset.SchemaTree;
import dremel.dataset.Slice;
import dremel.dataset.SliceScanner;
import dremel.executor.AggResult;
import dremel.executor.Executor;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;

public class MetaxaExecutor implements Executor {

	public static class JavaLangScript implements Script {
		IScriptEvaluator se;

		public JavaLangScript(String code) throws Exception {
			se = CompilerFactoryFactory.getDefaultCompilerFactory().newScriptEvaluator();
			se.setReturnType(void.class);

			se.setDefaultImports(new String[] { "dremel.compiler.*", "dremel.compiler.expression.*", "dremel.executor.*", "dremel.tableton.*","dremel.tableton.impl.*", "java.util.List", "java.util.LinkedList", "java.nio.ByteBuffer" });
			se.setParameters(new String[] { "sourceTablet", "resultSchema" }, new Class[] { Tablet.class, SchemaColumnar.class });
			se.cook(code);
		}

		@Override
		public void evaluate(Object[] objs) {
			try {
				se.evaluate(objs);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	List<SchemaTree> fields;
	Query query;
	SliceScanner scanner;
	Script script;

	public MetaxaExecutor(Query query, SliceScanner scanner, Script script) {
		fields = new LinkedList<SchemaTree>();
		this.query = query;
		this.script = script;
		this.scanner = scanner;
	}

	@Override
	public List<SchemaTree> getFieldList() {
		return fields;
	}

	int getIndex(Node node) {
		for (int i = 0; i < query.getSelectExpressions().size(); i++) {
			Expression exp = query.getSelectExpressions().get(i);

			if (exp.getRoot() == node)
				return i;
		}
		return -1;
	}

	@Override
	public void execute() {
		Object[] context1 = new Object[query.getAggregationFunctions().size() + 1];// fix-size
																					// context,
																					// hold
																					// static
																					// allocated
																					// elements
		context1[0] = new Integer(0); // selectLevel
		Iterator<AggFunction> fIt = query.getAggregationFunctions().iterator();

		// allocate space for within aggregation function
		int i = 1;
		while (fIt.hasNext()) {
			dremel.compiler.impl.Expression.Function func = (dremel.compiler.impl.Expression.Function) fIt.next();

			if (func.getName().equalsIgnoreCase("count")) {
				AggResult r = new AggResult(func, new Integer(0));
				r.setIndex(getIndex(func));
				context1[i++] = r;
			} else if (func.getName().equalsIgnoreCase("sum")) {
				assert (func.getChildCount() == 1);
				assert (func.getChild(0) instanceof Symbol);
				Symbol symbol = (Symbol) func.getChild(0);
				assert (symbol.getReference() instanceof SchemaTree);
				SchemaTree d = (SchemaTree) symbol.getReference();
				if (d.isTypeInt64()) {
					AggResult r = new AggResult(func, new Integer(0));
					r.setIndex(getIndex(func));
					context1[i++] = r;
				} else if (d.isTypeFloat()) {
					AggResult r = new AggResult(func, new Float(0.0));
					r.setIndex(getIndex(func));
					context1[i++] = r;
				}
			}
		}
	}

	@Override
	public SliceScanner getScanner() {
		return scanner;
	}

	@Override
	public Query getQuery() {
		return query;
	}

	@Override
	public Script getScript() {
		return script;
	}
}
