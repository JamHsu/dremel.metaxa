package dremel.dataset;

import java.util.List;

public interface SliceScanner {
	boolean hasMore();
	Slice next();
	//List<FieldDescriptor> getFieldList();
}
