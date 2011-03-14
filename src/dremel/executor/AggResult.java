package dremel.executor;

import dremel.compiler.Expression.Function;
import dremel.dataset.impl.Slice;

/**
 * - hold temporal result of aggregation function - currently not support
 * complex expression like: count(docid)+5,
 * count(links.forward)+count(links.backward), sum(length(code)) - no groupby
 * support
 * 
 * @author nhsan
 * 
 */
public class AggResult {
	dremel.dataset.Slice marker;
	Object value;
	Function function;
	int index; //index of aggregation function in output slice

	public AggResult(Function function, Object value) {
		this.function = function;
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object val) {
		value = val;
	}

	public void reset(dremel.dataset.Slice marker) {
		this.marker = marker;
		if (value instanceof Integer)
			value = 0;
		else if (value instanceof Float)
			value = 0.0;
	}

	public int intValue() {
		return (Integer) value;
	}

	public void setMarker(Slice marker) {
		this.marker = marker;
	}

	public dremel.dataset.Slice getMarker() {
		return marker;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public Function getFunction() {
		return function;
	}
}
