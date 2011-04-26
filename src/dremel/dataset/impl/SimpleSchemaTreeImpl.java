/**
 * Copyright 2010, BigDataCraft.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dremel.dataset.impl;

import java.util.LinkedList;
import java.util.List;

import dremel.dataset.SchemaTree;
import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.ColumnMetaData.ColumnType;
import dremel.tableton.ColumnMetaData.EncodingType;
import dremel.tableton.impl.ColumnWriterImpl;
import dremel.tableton.impl.SchemaColumnarImpl;
import dremel.tableton.impl.TabletImpl;

/**
 * @author nhsan
 * 
 */
public class SimpleSchemaTreeImpl implements SchemaTree {

	public enum NodeType {
		REQUIRED, OPTIONAL, REPEATED
	};

	public enum DataType {
		RECORD, INT, BOOLEAN, LONG, FLOAT, DOUBLE, STRING, NOT_EXISTING_TYPE
	};

	NodeType nodeType = null;
	DataType dataType = null;

	List<SchemaTree> fieldList = null;
	String name;
	String fullName;
	int defLevel;
	int repLevel;
	SchemaTree parent;

	public SimpleSchemaTreeImpl(SchemaTree parent, String name, DataType dataType, NodeType nodeType) {
		this.parent = parent;
		this.name = name;
		this.nodeType = nodeType;
		this.dataType = dataType;
		if (dataType == DataType.RECORD) {
			fieldList = new LinkedList<SchemaTree>();
		}

		if (parent == null) {
			defLevel = -1;
			repLevel = 0;
			this.fullName = name;
		} else {
			assert (parent.isRecord());
			defLevel = parent.getDefLevel() + 1;
			if (nodeType == NodeType.REPEATED) {
				repLevel = parent.getRepLevel() + 1;
			}

			else {
				repLevel = parent.getRepLevel();
			}

			for (SchemaTree f : parent.getFieldsList()) {
				if (f.getName().equalsIgnoreCase(this.getName())) {
					throw new RuntimeException("Field " + f.getName() + " is duplicated in the record named " + this.name);
				}
			}
			parent.getFieldsList().add(this);
			
			if (parent.getDefLevel() >= 0) {
				this.fullName = parent.getFullName() + "." + name;
			}
			else
			{
				this.fullName = name;
			}
		}
		//System.out.println(fullName+":"+defLevel+","+repLevel);
	}

	@Override
	public SchemaTree getParent() {
		return parent;
	}

	@Override
	public int getDefLevel() {
		return defLevel;
	}

	@Override
	public int getRepLevel() {
		return repLevel;
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isRecord() {
		return (dataType == DataType.RECORD);
	}

	@Override
	public List<SchemaTree> getFieldsList() {
		return fieldList;
	}

	@Override
	public boolean isTypeInt64() {
		return (dataType == DataType.INT);
	}

	@Override
	public boolean isTypeString() {
		return (dataType == DataType.STRING);
	}

	@Override
	public boolean isTypeFloat() {
		return (dataType == DataType.FLOAT);
	}

	@Override
	public boolean isTypeBool() {
		return (dataType == DataType.BOOLEAN);
	}

	@Override
	public boolean isRepeated() {
		return (nodeType == NodeType.REPEATED);
	}

	@Override
	public boolean isRequired() {
		return (nodeType == NodeType.REQUIRED);
	}

	@Override
	public boolean isOptional() {
		return (nodeType == NodeType.OPTIONAL);
	}

	void printSchema(SchemaTree t, StringBuilder builder, int level)
	{
		for (int i=0;i<level;i++) builder.append("  ");
			
		if (t.isOptional()) builder.append("optional ");
		else if (t.isRepeated()) builder.append("repeated ");
		else if (t.isRequired()) builder.append("required ");
		
		builder.append(t.getName());
		
		if (t.isRecord())
		{
			builder.append(" {\n");
			for (SchemaTree f: t.getFieldsList())
			{
				printSchema(f, builder, level+1);
			}
			for (int i=0;i<level;i++) builder.append("  ");
			builder.append("}\n");
		}
		else
		{
			builder.append(";\n");
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		printSchema(this, builder, 0);
		return builder.toString();
		
	}
	// FOR PAPER SCHEMA
	public static SchemaTree buildPaperSchemaTree() {
		SimpleSchemaTreeImpl root = new SimpleSchemaTreeImpl(null, "Document", DataType.RECORD, NodeType.REQUIRED);
		SimpleSchemaTreeImpl docid = new SimpleSchemaTreeImpl(root, "DocId", DataType.INT, NodeType.REQUIRED);

		SimpleSchemaTreeImpl links = new SimpleSchemaTreeImpl(root, "Links", DataType.RECORD, NodeType.REQUIRED);
		SimpleSchemaTreeImpl forward = new SimpleSchemaTreeImpl(links, "Forward", DataType.INT, NodeType.REPEATED);
		SimpleSchemaTreeImpl backward = new SimpleSchemaTreeImpl(links, "Backward", DataType.INT, NodeType.REPEATED);

		SimpleSchemaTreeImpl name = new SimpleSchemaTreeImpl(root, "Name", DataType.RECORD, NodeType.REPEATED);
		SimpleSchemaTreeImpl language = new SimpleSchemaTreeImpl(name, "Language", DataType.RECORD, NodeType.REPEATED);
		SimpleSchemaTreeImpl code = new SimpleSchemaTreeImpl(language, "Code", DataType.STRING, NodeType.REQUIRED);
		SimpleSchemaTreeImpl country = new SimpleSchemaTreeImpl(language, "Country", DataType.STRING, NodeType.OPTIONAL);
		SimpleSchemaTreeImpl url = new SimpleSchemaTreeImpl(name, "Url", DataType.STRING, NodeType.OPTIONAL);
		return root;
	}

	static private void buildLinkBackwardData(ColumnMetaData columnMetaData, int m) {
		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		for (int i = 0; i < m; i++) {
			columnBuilder.addIntDataTriple(0, ColumnReader.NULL, (byte) 0, (byte) 1);
			columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte) 0, (byte) 2);
			columnBuilder.addIntDataTriple(30, ColumnReader.NOT_NULL, (byte) 1, (byte) 2);
		}

		columnBuilder.close();

	}

	static private void buildLinksForwardData(ColumnMetaData columnMetaData, int m) {
		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		for (int i = 0; i < m; i++) {
			columnBuilder.addIntDataTriple(20, ColumnReader.NOT_NULL, (byte) 0, (byte) 2);
			columnBuilder.addIntDataTriple(40, ColumnReader.NOT_NULL, (byte) 1, (byte) 2);
			columnBuilder.addIntDataTriple(60, ColumnReader.NOT_NULL, (byte) 1, (byte) 2);
			columnBuilder.addIntDataTriple(80, ColumnReader.NOT_NULL, (byte) 0, (byte) 2);
		}

		columnBuilder.close();
	}

	static private void buildDocIDData(ColumnMetaData docidMetaData, int m) {

		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(docidMetaData);
		// write data
		for (int i = 0; i < m; i++) {
			columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte) 0, (byte) 0);
			columnBuilder.addIntDataTriple(20, ColumnReader.NOT_NULL, (byte) 0, (byte) 0);
		}

		columnBuilder.close();

	}

	static public void buildPaperSchema(int m) {
		ColumnMetaData linksBackwardMetaData = new ColumnMetaData("Links.Backward", ColumnType.INT, EncodingType.NONE, "testdata/LinksBackward", (byte) 1, (byte) 2);
		buildLinkBackwardData(linksBackwardMetaData, m);

		ColumnMetaData linksForwardMetaData = new ColumnMetaData("Links.Forward", ColumnType.INT, EncodingType.NONE, "testdata/LinksForward", (byte) 1, (byte) 2);
		buildLinksForwardData(linksForwardMetaData, m);

		ColumnMetaData docidMetaData = new ColumnMetaData("DocId", ColumnType.INT, EncodingType.NONE, "testdata/docid", (byte) 0, (byte) 0);
		buildDocIDData(docidMetaData, m);
	}

	static public Tablet getPaperSchemaTablet() {
		// build single column tablet for the input
		ColumnMetaData linksBackwardMetaData = new ColumnMetaData("Links.Backward", ColumnType.INT, EncodingType.NONE, "testdata/LinksBackward", (byte) 1, (byte) 2);
		ColumnMetaData linksForwardMetaData = new ColumnMetaData("Links.Forward", ColumnType.INT, EncodingType.NONE, "testdata/LinksForward", (byte) 1, (byte) 2);
		ColumnMetaData docidMetaData = new ColumnMetaData("DocId", ColumnType.INT, EncodingType.NONE, "testdata/docid", (byte) 0, (byte) 0);

		SchemaColumnar schema = new SchemaColumnarImpl();
		schema.addColumnMetaData(linksBackwardMetaData);
		schema.addColumnMetaData(linksForwardMetaData);
		schema.addColumnMetaData(docidMetaData);
		// SchemaTree schemaTree = SchemaTreeLoader.loadSchema("[document]");
		SchemaTree schemaTree = SimpleSchemaTreeImpl.buildPaperSchemaTree();
		Tablet tablet = new TabletImpl(schemaTree, schema);

		return tablet;
	}
}
