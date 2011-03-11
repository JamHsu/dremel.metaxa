package dremel.dataset;

public interface Slice 
{
	int count();
	Object getValue(int index);
	void setValue(int index, Object val);
	void setValue(int index, int val);
	void setValue(int index, float val);
	void setValue(int index, String val);
	void setValue(int index, boolean val);
	
	int intValue(int index);
	float floatValue(int index);
	String stringValue(int index);
	boolean boolValue(int index);
	
	int getFetchLevel();
	int getSelectLevel();
	void setFetchLevel(int level);
	void setSelectLevel(int level);

	void setNull(boolean val);
	boolean isNull();
}
