package dremel.executor;

import dremel.dataset.Slice;



/**
 * - executor will put output slice to this stream for each loop iterator,
 * outslice can be contain some missing values in case of aggregation function
 * and will be filled latter when aggregation result is emitted. This class
 * should be thread safe because stream can be read by another thread
 * 
 * @author nhsan
 * 
 */
public interface SliceOutputStream{
	public void put(Slice slice);
	public void updateWithinValue(int col, Object value, Slice marker);
	
	public boolean hasReadySlice();
	public Slice get();
}
