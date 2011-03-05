package dremel.dataset;

import dremel.compiler.impl.FieldDescriptor;

public interface Table 
{
	String getTableName();
	SchemaTree getSchema();
	//String getDataDir();
}
