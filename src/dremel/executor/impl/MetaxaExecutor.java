package dremel.executor.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IScriptEvaluator;

//import com.google.protobuf.Descriptors.FieldDescriptor;
//import com.google.protobuf.Descriptors.FieldDescriptor.Type;

import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Query;
import dremel.dataset.SchemaTree;
import dremel.dataset.Slice;
import dremel.dataset.SliceScanner;
import dremel.executor.AggResult;
import dremel.executor.Executor;

public class MetaxaExecutor implements Executor {

	public static class JavaLangScript implements Script {
		IScriptEvaluator se;

		public JavaLangScript(String code) throws Exception {
			se = CompilerFactoryFactory.getDefaultCompilerFactory().newScriptEvaluator();
			se.setReturnType(void.class);
			se.setDefaultImports(new String[] { "dremel.compiler.*", "dremel.compiler.expression.*" });
			se.setParameters(new String[] { "inSlice", "outSlice", "context1" }, new Class[] { Slice.class, Slice.class, Integer[].class });
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

	@Override
	public void execute() {
		Object[] context1 = new Integer[query.getAggregationFunctions().size() + 1];//fix-size context, hold static allocated elements
		context1[0] = new Integer(0); //selectLevel
		Iterator<Function> fIt = query.getAggregationFunctions().iterator();

		//allocate space for within aggregation function
		int i = 1;
		while (fIt.hasNext()) {
			dremel.compiler.impl.Expression.Function func = (dremel.compiler.impl.Expression.Function) fIt.next();
			
			if (func.getName().equalsIgnoreCase("count")) {
				AggResult r = new AggResult(func, new Integer(0));
				context1[i++] = r;
			} else if (func.getName().equalsIgnoreCase("sum")) {
				assert (func.getChildCount() == 1);
				assert (func.getChild(0) instanceof Symbol);
				Symbol symbol = (Symbol) func.getChild(0);
				assert (symbol.getReference() instanceof SchemaTree);
				SchemaTree d = (SchemaTree) symbol.getReference();
				if (d.isTypeInt64()) {
					AggResult r = new AggResult(func, new Integer(0));
					context1[i++] = r;
				} else if (d.isTypeFloat()) {
					AggResult r = new AggResult(func, new Float(0.0));
					context1[i++] = r;
				}
			}
		}

		while (scanner.hasMore()) {
			Slice inSlice = scanner.next();
			Slice outSlice = new OutSlice(query.getSelectExpressions().size());
			script.evaluate(new Object[] { inSlice, outSlice, context1 });
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
