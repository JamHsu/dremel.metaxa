package dremel.dataset;

import dremel.compiler.impl.FieldDescriptor;

public interface Table 
{
	String getTableName();
	ISchemaTree getSchema();
	//String getDataDir();
}
