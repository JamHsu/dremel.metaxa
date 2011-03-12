package dremel.dataset.impl;

import dremel.dataset.SchemaTree;

public class TableImpl implements dremel.dataset.Table {

	String name;
	protected SchemaTree schema;

	public TableImpl(String name) {
		this.name = name;
		this.schema = SchemaTreeLoader.loadSchema(name);
	}

	@Override
	public String getTableName() {
		return name;
	}

	@Override
	public SchemaTree getSchema() {
		return schema;
	}
	
/*	public void createSchemaTree() {
		schema = new SchemaTreeImpl("document");
		SchemaTreeImpl f = SchemaTreeImpl.createPrimitive("DocId", PrimitiveType.INT);
		schema.addField(f);
		f = SchemaTreeImpl.createRecord("links");
		SchemaTreeImpl f1 = SchemaTreeImpl.createPrimitive("links.Backward", PrimitiveType.INT);
		f.addField(f1);
		f1 = SchemaTreeImpl.createPrimitive("links.Forward", PrimitiveType.INT);
		f.addField(f1);
		schema.addField(f);
		//f = SchemaTreeImpl.createArray(NodeType.RECORD);
		//System.out.println(schema.toString());
	}*/
}
