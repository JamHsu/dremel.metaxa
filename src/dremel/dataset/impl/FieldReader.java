package dremel.dataset.impl;

/**
 * (this interface is taken from test assignment)
 * @author nhsan
 *
 */

public interface FieldReader
{
	public RowData take(String field); 
	public RowData head(String field);
}
