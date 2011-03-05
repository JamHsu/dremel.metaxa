package dremel.dataset;

public interface Table 
{
	String getTableName();
	SchemaTree getSchema();
	//String getDataDir();
}
