package dremel.dataset;

public interface Slice 
{
	int count();
	Integer getValue(int index);
	void setValue(int index, Integer val);
	int getFetchLevel();
	int getSelectLevel();
	void setFetchLevel(int level);
	void setSelectLevel(int level);
	
	void setNull(boolean val);
	boolean isNull();
}
