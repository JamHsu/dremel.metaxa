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
import dremel.executor.Executor.Script;
import dremel.tableton.ColumnReader;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.impl.TabletImpl;

public class MetaxaExecutor implements Executor {

	public static class JavaLangScript implements Script {
		IScriptEvaluator se;

		public JavaLangScript(String code) throws Exception {
			se = CompilerFactoryFactory.getDefaultCompilerFactory().newScriptEvaluator();
			se.setReturnType(void.class);

			se.setDefaultImports(new String[] { "dremel.compiler.*", "dremel.compiler.expression.*", "dremel.executor.*", "dremel.tableton.*", "dremel.tableton.impl.*", "java.util.List", "java.util.LinkedList", "java.nio.ByteBuffer" });
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
	Script script;

	public MetaxaExecutor(Query query, String code) throws Exception {
		fields = new LinkedList<SchemaTree>();
		this.query = query;
		script = new MetaxaExecutor.JavaLangScript(code);
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
		SchemaColumnar schema = query.getTargetSchemaColumnar();

		script.evaluate(new Object[] { query.getTables().get(0), schema });

		//print result tablet
//		Tablet tablet = new TabletImpl(schema);
//
//		boolean hasMoreSlices = true;
//		int fetchLevel = 0;
//
//		while (hasMoreSlices) {
//			int nextLevel = 0;
//			hasMoreSlices = false;
//			for (dremel.compiler.Expression exp : query.getSelectExpressions()) {
//				ColumnReader nextReader = tablet.getColumns().get(exp.getJavaName());
//
//				if (nextReader.nextRepetitionLevel() >= fetchLevel) {
//					boolean isLastInReader = nextReader.next();
//					hasMoreSlices = hasMoreSlices || isLastInReader;
//					if (hasMoreSlices) {
//						if (nextReader.isNull()) {
//							System.out.print("NULL\t\t");
//						} else {
//							System.out.print(nextReader.getIntValue() + "\t\t");
//						}
//					}
//				} else {
//					System.out.print("N/A\t\t");
//				}
//				nextLevel = Math.max(nextLevel, nextReader.nextRepetitionLevel());
//			}
//			System.out.println();
//			fetchLevel = (byte) nextLevel;
//		}

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
