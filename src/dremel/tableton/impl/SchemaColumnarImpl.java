/**
   Copyright 2010, BigDataCraft.Com Ltd.
   David Gruzman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.Ope
*/
package dremel.tableton.impl;

import java.util.HashMap;
import java.util.Map;

import dremel.dataset.SchemaTree;
import dremel.dataset.impl.SchemaTreeImpl;
import dremel.dataset.impl.SchemaTreeImpl.NodeType;
import dremel.tableton.ColumnMetaData;
import dremel.tableton.SchemaColumnar;

public class SchemaColumnarImpl implements SchemaColumnar {

	Map<String, ColumnMetaData> columnsMetaData = new HashMap<String, ColumnMetaData>();
	
	public SchemaColumnarImpl()
	{
		
	}
	
	@Override
	public void addColumnMetaData(ColumnMetaData newColumn)
	{
		if(columnsMetaData.containsKey(newColumn.getColumnName().toLowerCase()))
		{
			throw new RuntimeException("Column with name "+newColumn.getColumnName() +"Already exists in the schema");
		}
		columnsMetaData.put(newColumn.getColumnName().toLowerCase(), newColumn);
	}
	
	@Override
	public ColumnMetaData getColumnMetaData(String columnName) {
		if(!columnsMetaData.containsKey(columnName))
		{
			throw new RuntimeException("Column with name "+columnName +" is not exists in the schema");
		}
		return columnsMetaData.get(columnName);
	}

	@Override
	public Map<String, ColumnMetaData> getColumnsMetaData() {		
		return columnsMetaData;
	}
}
