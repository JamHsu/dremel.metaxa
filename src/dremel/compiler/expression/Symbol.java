package dremel.compiler.expression;

import dremel.compiler.impl.FieldDescriptor;

import dremel.compiler.ExpNode;
import dremel.compiler.Expression;
import dremel.compiler.Query;

/**
 * @author nhsan
 *
 */
public class Symbol extends AbstractNode{
	String symbol;
	Object reference;
	int sliceMappingIndex;

	public Symbol(String symbol, Query query) {
		super(query);
		this.symbol = symbol.toLowerCase();
		sliceMappingIndex=-1;
	}

	@Override
	public int getChildCount() {
		return 0;
	}

	@Override
	public ExpNode getChild(int index) {
		return null;
	}

	@Override
	public String generateCode() {
		if (reference instanceof Expression)
		{
			Expression exp=(Expression)reference;
			return exp.getRoot().generateCode();
		}
		else if ((reference instanceof FieldDescriptor) && (sliceMappingIndex!=-1))
		{
			return "inSlice.getValue("+sliceMappingIndex+")";
		}
		return symbol;
	}

	@Override
	public ReturnType getReturnType() {
		return null;
	}

	public String getSymbol() {
		return symbol;
	}

	public Object getReference() {
		return reference;
	}

	public void setReference(Object reference) {
		this.reference = reference;
	}
	
	public int getSliceMappingIndex() {
		return sliceMappingIndex;
	}
	
	public void setSliceMappingIndex(int sliceMappingIndex) {
		this.sliceMappingIndex = sliceMappingIndex;
	}
}
