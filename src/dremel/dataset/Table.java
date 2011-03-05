package dremel.dataset;

import dremel.compiler.impl.FieldDescriptor;

public interface Table 
{
	String getTableName();
	FieldDescriptor getSchema();
	//String getDataDir();
}
