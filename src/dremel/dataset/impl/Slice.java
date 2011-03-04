package dremel.dataset.impl;

public class Slice implements dremel.dataset.Slice {
	Integer[] values;
	int count;
	boolean is_null;
	int fetchLevel;
	int selectLevel;

	public Slice(int count) {
		values = new Integer[count];
		this.count = count;
		is_null = true;
	}

	@Override
	public int count() {
		return count;
	}

	@Override
	public Integer getValue(int index) {
		if (values[index]==null) return 0;
		return values[index];
	}

	@Override
	public void setValue(int index, Integer val) {
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
}
