package dremel.dataset;

import java.util.List;

import dremel.compiler.impl.FieldDescriptor;

public interface SliceScanner {
	boolean hasMore();
	Slice next();
	//List<FieldDescriptor> getFieldList();
}
