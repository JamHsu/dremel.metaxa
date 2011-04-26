package dremel.dataset.impl;

import java.util.List;

import dremel.dataset.SchemaTree;

public class SchemaTreeDb implements SchemaTree {
	
	public enum NodeType implements EnumConverter { 
		NONE(0), REQUIRED(1), OPTIONAL(2), REPEATED(3);

		private short value;

		private NodeType(int value) {
			this.value = (short) value;
		}

		public short convert() {
			return value;
		}
	};
	
	public enum DataType implements EnumConverter {
		NONE(0), GROUP(1), INT(2), LONG(3), STRING(4), BOOL(5), FLOAT(6), DOUBLE(7);
		
		private short value;

		private DataType(int value) {
			this.value = (short) value;
		}

		public short convert() {
			return value;
		}
	};
	
	Long id;
	String name;
	NodeType ntype;
	DataType dtype;
	
	List<SchemaTree> fields;
	
	public SchemaTreeDb(Long id, String name, NodeType ntype, DataType dtype) {
		this.id = id;
		this.name = name;
		this.ntype = ntype;
		this.dtype = dtype;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isRepeated() {
		if (ntype == NodeType.REPEATED)
			return true;
		return false;
	}

	@Override
	public boolean isRecord() {
		if (dtype == DataType.GROUP)
			return true;
		return false;
	}

	@Override
	public List<SchemaTree> getFieldsList() {
		return fields;
	}

	@Override
	public boolean isTypeInt64() {
		if (dtype == DataType.LONG)
			return true;
		return false;
	}

	@Override
	public boolean isTypeString() {
		if (dtype == DataType.STRING)
			return true;
		return false;
	}

	@Override
	public boolean isTypeFloat() {
		if (dtype == DataType.FLOAT)
			return true;
		return false;
	}

	@Override
	public boolean isTypeBool() {
		if (dtype == DataType.BOOL)
			return true;
		return false;
	}
	
	public void setFields(List<SchemaTree> fields) {
		this.fields = fields;
	}

	public Long getId() {
		return id;
	}
	
	public interface EnumConverter {
		  public short convert();
	}

	@Override
	public boolean isRequired() {
		if (ntype == NodeType.REQUIRED)
			return true;
		return false;
	}

	@Override
	public boolean isOptional() {
		if (ntype == NodeType.OPTIONAL)
			return true;
		return false;
	}
	/* (non-Javadoc)
	 * @see dremel.dataset.SchemaTree#getFullName()
	 */
	@Override
	public String getFullName() {
		return null;
	}

	/* (non-Javadoc)
	 * @see dremel.dataset.SchemaTree#getDefLevel()
	 */
	@Override
	public int getDefLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see dremel.dataset.SchemaTree#getRepLevel()
	 */
	@Override
	public int getRepLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see dremel.dataset.SchemaTree#getParent()
	 */
	@Override
	public SchemaTree getParent() {
		// TODO Auto-generated method stub
		return null;
	}
}

