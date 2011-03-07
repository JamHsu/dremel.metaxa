package dremel.server;

import static org.junit.Assert.*;

import org.junit.Test;

import dremel.compiler.Compiler;
import dremel.dataset.SchemaTree;
import dremel.dataset.impl.SchemaTreeImpl;
import dremel.dataset.impl.SchemaTreeImpl.NodeType;
import dremel.dataset.impl.SchemaTreeImpl.PrimitiveType;
import dremel.server.impl.ServerImpl;
import dremel.server.Server;


public class ServerTest {
	
	@Test
	public void aspectTest1() {
		ServerImpl srv = new ServerImpl();
		assertTrue(srv.getCompiler() instanceof dremel.compiler.impl.CompilerImpl);
		assertTrue(srv.getStream() instanceof dremel.dataset.impl.LocalDirectory);
	}
	
	@Test
	public void createSchemaTree() {
		SchemaTreeImpl schema = new SchemaTreeImpl("Document");
		SchemaTreeImpl f = SchemaTreeImpl.createPrimitive("DocId", PrimitiveType.INT);
		schema.addField(f);
		f = SchemaTreeImpl.createRecord("Links");
		SchemaTreeImpl f1 = SchemaTreeImpl.createPrimitive("Backward", PrimitiveType.INT);
		f.addField(f1);
		f1 = SchemaTreeImpl.createPrimitive("Forward", PrimitiveType.INT);
		f.addField(f1);
		schema.addField(f);
		//f = SchemaTreeImpl.createArray(NodeType.RECORD);
		System.out.println(schema.toString());
	}
}