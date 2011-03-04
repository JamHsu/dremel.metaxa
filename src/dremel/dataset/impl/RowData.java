package dremel.dataset.impl;

/**
 * (this class is taken from test assignment)
 * @author nhsan
 *
 */

public class RowData
{
	Object value;
	int rlevel;
	int dlevel;

	public RowData(Object value, int rlevel, int dlevel)
	{
		this.value = value;
		this.rlevel = rlevel;
		this.dlevel = dlevel;
	}

	public Object getValue()
	{
		return value;
	}

	public int getRlevel()
	{
		return rlevel;
	}

	public int getDlevel()
	{
		return dlevel;
	}
}
