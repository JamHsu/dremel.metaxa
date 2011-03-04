package dremel.dataset.impl;

import java.io.FileNotFoundException;
import java.util.List;

import com.google.protobuf.Descriptors.FieldDescriptor;


import dremel.dataset.Slice;
import dremel.dataset.SliceScanner;

public class SimpleSliceScanner implements SliceScanner {
	List<FieldDescriptor> fields;
	String sourceDir;
	FieldReader reader;
	boolean has_more;
	int fetchLevel;
	Slice slice;

	public SimpleSliceScanner(List<FieldDescriptor> fields, String sourceDir) throws FileNotFoundException {
		this.fields = fields;
		this.sourceDir = sourceDir;
		reader = new FileFieldReader(sourceDir);
		fetchLevel = 0;
		has_more = true;
		slice = null;
	}

	@Override
	public boolean hasMore() {
		slice = fetch();
		if (slice.isNull())
			return false;
		return true;
	}

	@Override
	public Slice next() {
		if (slice == null) {
			slice = fetch();
		}

		Slice ret = slice;
		slice = null;
		return ret;
	}

	//Appendix D fetch function
	private Slice fetch() {
		Slice ret = new dremel.dataset.impl.Slice(fields.size());
		ret.setNull(true);
		int nextLevel = 0;
		for (int i = 0; i < fields.size(); i++) {
			FieldDescriptor field = fields.get(i);
			RowData data = reader.head(field.getFullName());
			if (data != null && data.getRlevel() >= fetchLevel) {
				if ("NULL".equals(((String) data.getValue()))) {
					ret.setValue(i, null);
				} else {
					ret.setValue(i, new Integer((String) data.getValue()));
				}
				reader.take(field.getFullName());
				ret.setNull(false);
			} else
				ret.setValue(i, null);
			data = reader.head(field.getFullName());
			if (data != null && nextLevel < data.getRlevel())
				nextLevel = data.getRlevel();
		}
		fetchLevel = nextLevel;
		ret.setFetchLevel(nextLevel);
		
		return ret;
	}

//	@Override
//	public List<FieldDescriptor> getFieldList() {
//		return null;
//	}
}
