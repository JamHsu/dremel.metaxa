package dremel.executor.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.commons.compiler.IScriptEvaluator;

import dremel.compiler.impl.FieldDescriptor;

import dremel.compiler.Query;
import dremel.dataset.Slice;
import dremel.dataset.SliceScanner;
import dremel.executor.Executor;

public class MetaxaExecutor implements Executor {

	List<FieldDescriptor> fields;
	Query query;
	SliceScanner scanner;
	IScriptEvaluator se;
	Integer[] context1;
	private SliceScanner resultScanner;

	public MetaxaExecutor(Query query, SliceScanner scanner, IScriptEvaluator se) {
		fields = new LinkedList<FieldDescriptor>();
		this.query = query;
		this.se = se;
		this.scanner = scanner;
	}

/*	@Override
	public List<GenericFieldDesc> getFieldList() {
		return fields;
	}*/

	@Override
	public void execute() {
		start();
		while (scanner.hasMore()) {
			Slice outSlice = step();

			//just print to stdout
			//				System.out.print("ISLICE:\t");
			//				for (int i = 0; i < inSlice.count(); i++) {
			//					System.out.print(inSlice.getValue(i) + "\t\t");
			//				}
			//System.out.println();
			if (outSlice.isNull())
				System.out.println("OSLICE:\tNULL");
			else {
				System.out.print("OSLICE:\t");
				for (int i = 0; i < outSlice.count(); i++) {
					System.out.print(outSlice.getValue(i) + "\t\t");
				}
				System.out.println();
			}
		}
	}

	public Slice step() {
		if (scanner.hasMore()) {
			Slice inSlice = scanner.next();
			Slice outSlice = new dremel.dataset.impl.Slice(query.getSelectExpressions().size());
			try {
				se.evaluate(new Object[] { inSlice, outSlice, context1 });
				return outSlice;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			// left for now, for debug
			System.out.print("ISLICE:\t");
			for (int i = 0; i < inSlice.count(); i++) {
				System.out.print(inSlice.getValue(i) + "\t\t");
			}
			System.out.println();
		}
		return null;
	}
	
	public void start() {
		context1 = new Integer[3];
		context1[0] = 0;
	}

	@Override
	public SliceScanner getScanner() {
		return scanner;
	}

	@Override
	public Query getQuery() {
		return query;
	}

	/* (non-Javadoc)
	 * @see dremel.executor.Executor#setEvaluator(org.codehaus.commons.compiler.IScriptEvaluator)
	 */
	@Override
	public void setEvaluator(IScriptEvaluator se) {
		this.se = se;
	}

	/* (non-Javadoc)
	 * @see dremel.executor.Executor#getResultScanner()
	 */
	@Override
	public SliceScanner getResultScanner() {
		return resultScanner;
	}

}
