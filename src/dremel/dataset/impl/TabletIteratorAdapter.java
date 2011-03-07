package dremel.dataset.impl;

import java.util.List;
import java.util.Map;

import com.google.protobuf.Descriptors.FieldDescriptor;

import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.dataset.Slice;
import dremel.tableton.TabletIterator;

public class TabletIteratorAdapter implements dremel.dataset.SliceScanner{

	TabletIterator tabletIterator = null;
	Slice slice;
	List<String> fieldsOrder;
	
	public TabletIteratorAdapter(TabletIterator forTabletIterator, List<String> forFieldsOrder)
	{
		tabletIterator = forTabletIterator;
		slice = null;
		fieldsOrder = forFieldsOrder;
	}
	@Override
	public boolean hasMore() {
		slice = fetchSliceFromIterator();
		if (slice.isNull())
			return false;
		return true;
	}

	@Override
	public Slice next() {
		if (slice == null) {
			slice = fetchSliceFromIterator();
		}

		Slice ret = slice;
		slice = null;
		return ret;
	}

	private Slice fetchSliceFromIterator() {
		boolean hasData = tabletIterator.fetch();
		
		Slice resultSlice = new dremel.dataset.impl.Slice(fieldsOrder.size());
		
		if(!hasData)
		{
			resultSlice.setNull(true);
			return resultSlice;
		}
		
		
		Map<String, ColumnReader> columnReaders = tabletIterator.getColumnsMap();
		
		int index =0;
		for(String field : fieldsOrder)
		{
			if(!columnReaders.containsKey(field)){
				throw new RuntimeException("The field "+field +" not found in the tablet");
			}
			ColumnReader reader = columnReaders.get(field);
			
			assert(reader.getDataType() == ColumnMetaData.ColumnType.INT); // only integers are supported by the Slice class
			Integer value;
			if(reader.isNull())
			{
				value = null;
			}else
			{
				value = new Integer(reader.getIntValue());
			}
			
			resultSlice.setValue(index, value);
			
			resultSlice.setSelectLevel(tabletIterator.getFetchLevel());
			resultSlice.setFetchLevel(tabletIterator.getFetchLevel());
			resultSlice.setNull(false);
			 
			index++;
		}
		
		
		return resultSlice;
	}
/*	@Override
	public List<FieldDescriptor> getFieldList() {
		// TODO Auto-generated method stub
		return null;
	}*/

}
