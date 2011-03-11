package dremel.executor.impl;

import dremel.dataset.impl.Slice;

public class OutSlice extends Slice{
	int missingCount;
	public OutSlice(int count) {
		super(count);
		missingCount=0;
	}
	
	public void setMissingCount(int missingCount) {
		this.missingCount = missingCount;
	}
	
	public int getMissingCount() {
		return missingCount;
	}
}
