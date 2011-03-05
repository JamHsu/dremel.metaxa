package dremel.dataset.impl;

import dremel.dataset.ISchemaTree;

public class TableImpl implements dremel.dataset.Table {

	String name;
	protected ISchemaTree schema;

	public TableImpl(String name) {
		this.name = name;
	}

	@Override
	public String getTableName() {
		return name;
	}

	@Override
	public ISchemaTree getSchema() {
		return schema;
	}
}
