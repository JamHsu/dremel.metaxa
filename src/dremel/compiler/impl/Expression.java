package dremel.compiler.impl;

import java.util.LinkedList;
import java.util.List;

import dremel.compiler.impl.FieldDescriptor;

import dremel.compiler.ExpNode;

/**
 * @author nhsan
 * 
 */
public class Expression implements dremel.compiler.Expression {
	ExpNode root;
	String scope;
	String alias;
	int rLevel;
	int withinLevel;
	List<FieldDescriptor> fields;

	public Expression() {
		fields = new LinkedList<FieldDescriptor>();
	}

	@Override
	public ExpNode getRoot() {
		return root;
	}

	public void setRoot(ExpNode root) {
		this.root = root;
	}

	@Override
	public String getWithin() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	public int getWithinLevel() {
		return withinLevel;
	}

	public void setWithinLevel(int withinLevel) {
		this.withinLevel = withinLevel;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public int getRepetitionLevel() {
		return rLevel;
	}

	public void setRLevel(int rLevel) {
		this.rLevel = rLevel;
	}

	@Override
	public List<FieldDescriptor> getRelatedFields() {
		return fields;
	}

}
