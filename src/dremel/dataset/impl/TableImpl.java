package dremel.dataset.impl;

import dremel.dataset.SchemaTree;

public class TableImpl implements dremel.dataset.Table {

	String name;
	protected SchemaTree schema;

	public TableImpl(String name) {
		this.name = name;
	}

	@Override
	public String getTableName() {
		return name;
	}

	@Override
	public SchemaTree getSchema() {
		return schema;
	}
}
