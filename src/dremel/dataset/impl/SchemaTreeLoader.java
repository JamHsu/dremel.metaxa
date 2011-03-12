package dremel.dataset.impl; 

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dremel.dataset.SchemaTree;
import dremel.dataset.impl.SchemaTreeDb.DataType;
import dremel.dataset.impl.SchemaTreeDb.EnumConverter;
import dremel.dataset.impl.SchemaTreeDb.NodeType;

public class SchemaTreeLoader {

	public static class ReverseEnumMap<V extends Enum<V> & EnumConverter>{
		  private Map<Short, V> map = new HashMap<Short, V>();
		  public ReverseEnumMap(Class<V> valueType) {
		    for (V v : valueType.getEnumConstants()) {
		      map.put(v.convert(), v);
		    }
		  }

		  public V get(short num) {
		    return map.get(num);
		  }
	}
	
	private static ReverseEnumMap<NodeType> nodeTypeMap;
	private static ReverseEnumMap<DataType> dataTypeMap;
	
	//private static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	//private static final String database = "jdbc:derby:tabletdb;create=true";
	private static final String driver = "org.hsqldb.jdbc.JDBCDriver";
	private static final String database = "jdbc:hsqldb:tablet.db/tabletdb";
	
	
	private static Connection conn;
	
	static {
		if ("org.apache.derby.jdbc.EmbeddedDriver".equals(driver))
			System.setProperty("derby.system.home", System.getProperty("user.home", "."));
		if ("org.hsqldb.jdbc.JDBCDriver".equals(driver))
			System.setProperty("hsqldb.system.home", System.getProperty("user.home", "."));
		try {
			Class.forName(driver).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        conn = getDBConnection();
        nodeTypeMap = new ReverseEnumMap<SchemaTreeDb.NodeType>(NodeType.class);
        dataTypeMap = new ReverseEnumMap<SchemaTreeDb.DataType>(DataType.class);
	}

	private static Connection getDBConnection()
	{
		Connection c = null;
		try {
			c = DriverManager.getConnection(database,"","");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return c;
	}
	
	private static PreparedStatement getStatement(String query) {
		try {
            if (conn == null)
            	conn = getDBConnection();
            return conn.prepareStatement(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
	}
	
	private static PreparedStatement getStatement(String query, int opts) {
		try {
            if (conn == null)
            	conn = getDBConnection();
            return conn.prepareStatement(query, opts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
	}
	
	private static void closeStatement(Statement stmt)
	{
		if (stmt == null)
			return;
		try {
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static SchemaTree loadSchema(String name) {
		SchemaTreeDb rootNode = getRootNodeByName(name.toUpperCase());
		attachChildNodes(rootNode);
		return rootNode;
	}

	private static void attachChildNodes(SchemaTreeDb node) {
		if (node == null)
			return;
		List<Long> fields = getEdgesById(node.getId());
		if (fields != null) {
			List<SchemaTree> list = new LinkedList<SchemaTree>();
			for (Long id : fields) {
				SchemaTreeDb n = getNodeById(id);
				attachChildNodes(n);
				list.add(n);
			}
			node.setFields(list);
		}
	}

	private static SchemaTreeDb getNodeById(Long id) {
		String q = "select id, ntype, dtype, name from node where id = ?";
		PreparedStatement st = getStatement(q);
		SchemaTreeDb result = null;
		try {
			st.setLong(1, id);
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				Short nt = rs.getShort(2);
				Short dt = rs.getShort(3);
				String name = rs.getString(4);
				result = new SchemaTreeDb(
						rs.getLong(1),
						name,
						nodeTypeMap.get(nt),
						dataTypeMap.get(dt)
				);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
		return result;
	}

	private static List<Long> getEdgesById(Long id) {
		String q = "select toid from edge where fromid = ?";
		PreparedStatement st = getStatement(q);
		List<Long> result = null;
		try {
			st.setLong(1, id);
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				result = new LinkedList<Long>();
				result.add(rs.getLong(1));
			}
			while (rs.next()) {
				result.add(rs.getLong(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
		return result;
	}

	private static SchemaTreeDb getRootNodeByName(String name) {
		String q = "select rootid from root where name = ?";
		PreparedStatement st = getStatement(q);
		Long rootId = null;
		try {
			st.setString(1, name);
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				rootId = rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
		if (rootId == null)
			return null;
		return getNodeById(rootId);
	}

	public static void saveSchema(SchemaTree schema) {
		SchemaTreeDb root = getRootNodeByName(schema.getName());
		Long id = saveNode(schema);
		saveRoot(schema.getName(), id);
		if (root != null) {
			deleteSchemaTree(root.getId());
		}
	}
	
	public static void cloneSchema(SchemaTree schema, String name) {
		if (getRootNodeByName(name.toUpperCase()) != null) {
			saveSchema(schema);
			return;
		}
		SchemaTreeDb newSchema = new SchemaTreeDb((long) 0, name, NodeType.NONE, DataType.NONE);
		newSchema.setFields(schema.getFieldsList());
		saveSchema(newSchema);
	}

	private static void deleteSchemaTree(Long id) {
		List<Long> flds = getEdgesById(id);
		deleteEdges(id);
		for (Long i : flds) {
			deleteSchemaTree(i);
		}
		deleteNode(id);
	}

	private static void deleteNode(Long id) {
		String q = "delete from node where id = ?";
		PreparedStatement st = getStatement(q);
		try {
			st.setLong(1, id);
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
	}

	private static void deleteEdges(Long id) {
		String q = "delete from edge where fromid = ?";
		PreparedStatement st = getStatement(q);
		try {
			st.setLong(1, id);
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
	}

	private static void saveRoot(String name, Long id) {
		String q = "select id from root where name = ?";
		PreparedStatement st = getStatement(q);
		Long schemaId = null;
		try {
			st.setString(1, name);
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				schemaId = rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
		q = "insert into root (name, rootid) values (?, ?)";
		st = getStatement(q);
		try {
			st.setString(1, name);
			st.setLong(2, id);
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
		if (schemaId != null) {
			q = "delete from root where id = ?";
			st = getStatement(q);
			try {
				st.setLong(1, schemaId);
				st.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeStatement(st);
			}
		}
	}

	private static Long saveNode(SchemaTree node) {
		String q = "insert into node (ntype, dtype, name) values (?, ?, ?)";
		PreparedStatement st = getStatement(q, Statement.RETURN_GENERATED_KEYS);
		Long id = null;
		try {
			st.setShort(1, getNodeType(node));
			st.setShort(2, getDataType(node));
			st.setString(3, node.getName());
			st.executeUpdate();
			ResultSet rs = st.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
		List<SchemaTree> fieldList = node.getFieldsList();
		if (fieldList != null) {
			List<Long> flds = new LinkedList<Long>();
			for (SchemaTree n : node.getFieldsList()) {
				Long i = saveNode(n);
				flds.add(i);
			}
			saveEdges(id, flds);
		}
		return id;
	}

	private static short getDataType(SchemaTree node) {
		if (node.isRecord())
			return DataType.GROUP.convert();
		if (node.isTypeBool())
			return DataType.BOOL.convert();
		if (node.isTypeFloat())
			return DataType.FLOAT.convert();
		if (node.isTypeInt64())
			return DataType.LONG.convert();
		if (node.isTypeString())
			return DataType.STRING.convert();
		return 0;
	}

	private static short getNodeType(SchemaTree node) {
		if (node.isRepeated())
			return NodeType.REPEATED.convert();
		if (node.isRequired())
			return NodeType.REQUIRED.convert();
		if (node.isOptional())
			return NodeType.OPTIONAL.convert();
		return 0;
	}

	private static void saveEdges(Long id, List<Long> flds) {
		String q = "insert into edge values (?, ?)";
		PreparedStatement st = getStatement(q);
		try {
			st.setLong(1, id);
			for (Long i : flds) {
				st.setLong(2, i);
				st.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStatement(st);
		}
	}
}
