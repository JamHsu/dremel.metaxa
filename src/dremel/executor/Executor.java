package dremel.executor;

import org.codehaus.commons.compiler.IScriptEvaluator;

//import dremel.compiler.impl.GenericFieldDesc;

import dremel.compiler.Query;
import dremel.dataset.Slice;
import dremel.dataset.SliceScanner;

public interface Executor {
	//public List<GenericFieldDesc> getFieldList();
	public void execute();
	public SliceScanner getScanner();
	public Query getQuery();
	public void setEvaluator(IScriptEvaluator se);
	public Slice step();
	public SliceScanner getResultScanner();
}
