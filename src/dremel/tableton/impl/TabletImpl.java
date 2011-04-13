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
import java.util.List;
import java.util.Map;

import dremel.dataset.SchemaTree;
import dremel.tableton.ColumnReader;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.TabletIterator;

public class TabletImpl implements Tablet {

	SchemaColumnar schemaColumnar;
	SchemaTree schemaTree;
	Map<String, ColumnReader> columnReaders = null;
	
	public TabletImpl(SchemaColumnar forSchema) {
		schemaColumnar = forSchema;
		initReaders();
	}
	
	public TabletImpl(SchemaTree forSchemaTree, SchemaColumnar forSchemaColumnar) {
		schemaTree = forSchemaTree;
		schemaColumnar = forSchemaColumnar;
		initReaders();
	}

	private void initReaders() {
		
		columnReaders = new HashMap<String, ColumnReader>();
		
		for(String columnName : schemaColumnar.getColumnsMetaData().keySet())
		{
			SimpleIntColumnReader reader = new SimpleIntColumnReader(schemaColumnar.getColumnMetaData(columnName));
			columnReaders.put(columnName, reader);
		}
		
	}

	@Override
	public TabletIterator getIterator() {
		TabletIterator tabletIter = new TabletIteratorImpl(columnReaders, schemaColumnar);
		return tabletIter;
	}

	@Override
	public TabletIterator getProjectionIterator(List<String> columnsInProjection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SchemaColumnar getSchemaColumnar() {
		
		return schemaColumnar;
	}

	@Override
	public Map<String, ColumnReader> getColumns() {
		return columnReaders;
	}

	/* (non-Javadoc)
	 * @see dremel.tableton.Tablet#getSchemaTree()
	 */
	@Override
	public SchemaTree getSchemaTree() {
		return schemaTree;
	}
}
