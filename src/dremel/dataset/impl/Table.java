package dremel.dataset.impl;

//import schema.Schema.Document;

import dremel.compiler.impl.FieldDescriptor;

public class Table implements dremel.dataset.Table {

	String name;

	public Table(String name) {
		this.name = name;
	}

	@Override
	public String getTableName() {
		return name;
	}

	@Override
	public FieldDescriptor getSchema() {
//		if (name.equalsIgnoreCase("[document]")) //for test only
//			return Document.getDescriptor();
		return null;
	}

	@Override
	public String getDataDir() {
		if (name.equalsIgnoreCase("[document]")) //for test only
			return "src/dremel/test/data";
		return null;
	}
}
