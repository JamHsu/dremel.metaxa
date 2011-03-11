package dremel.dataset.impl;


public class Slice implements dremel.dataset.Slice {
	Object[] values;
	int count;
	boolean is_null;
	int fetchLevel;
	int selectLevel;

	public Slice(int count) {
		values = new Object[count];
		this.count = count;
		is_null = true;
	}

	@Override
	public int count() {
		return count;
	}

	@Override
	public Object getValue(int index) {
		return values[index];
	}

	@Override
	public void setValue(int index, Object val) {
		values[index]=val;
	}

	@Override
	public int getFetchLevel() {
		return fetchLevel;
	}

	@Override
	public int getSelectLevel() {
		return selectLevel;
	}

	@Override
	public void setFetchLevel(int level) {
		fetchLevel=level;
	}

	@Override
	public void setSelectLevel(int level) {
		selectLevel=level;
	}

	@Override
	public void setNull(boolean val) {
		is_null=val;
	}

	@Override
	public boolean isNull() {
		return is_null;
	}

	@Override
	public int intValue(int index) {
		if (values[index]==null) return 0;
		return (Integer)values[index];
	}

	@Override
	public float floatValue(int index) {
		if (values[index]==null) return 0;
		return (Float)values[index];
	}

	@Override
	public String stringValue(int index) {
		if (values[index]==null) return "";
		return (String)values[index];
	}

	@Override
	public boolean boolValue(int index) {
		if (values[index]==null) return false;
		return (Boolean)values[index];
	}

	@Override
	public void setValue(int index, int val) {
		Integer iVal = val;
		values[index]=iVal;
	}

	@Override
	public void setValue(int index, float val) {
		Float fVal = val;
		values[index]=fVal;
	}

	@Override
	public void setValue(int index, String val) {
		values[index]=val;
	}

	@Override
	public void setValue(int index, boolean val) {
		Boolean bVal = val;
		values[index]=bVal;
	}
}
