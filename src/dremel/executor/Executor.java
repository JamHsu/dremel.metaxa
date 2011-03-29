package dremel.executor;

import java.util.List;

//import com.google.protobuf.Descriptors.FieldDescriptor;

import dremel.compiler.Query;
import dremel.dataset.SchemaTree;
import dremel.dataset.SliceScanner;

public interface Executor {
	
	public interface Script
	{
		public void evaluate(Object[] objs);
	}
	
	public List<SchemaTree> getFieldList();
	public void execute();
	public Query getQuery();
	public Script getScript();
}
