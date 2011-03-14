package dremel.executor.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import dremel.dataset.Slice;
import dremel.executor.SliceOutputStream;

public class SimpleOutputStream implements SliceOutputStream {
	List<Slice> slices;

	public SimpleOutputStream() {
		slices = new LinkedList<Slice>();
	}

	@Override
	public void put(Slice slice) {
		slices.add(slice);
	}

	@Override
	public void updateWithinValue(int col, Object value, Slice marker) {
		if (slices.size() == 0)
			return;
		ListIterator<Slice> it = slices.listIterator(slices.size());
		Slice s;
		do {
			s = it.previous();
			s.setValue(col, value);
			s.setMissingCount(s.missingCount() - 1);//one aggregate value is emited
		} while (s != marker);
	}

	@Override
	public boolean hasReadySlice() {
		if (slices.size() == 0)
			return false;
		return (slices.get(0).missingCount() == 0);
	}

	@Override
	public Slice get() {
		return slices.remove(0);
	}
}
