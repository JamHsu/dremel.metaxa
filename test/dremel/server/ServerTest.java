package dremel.server;

import static org.junit.Assert.*;

import org.junit.Test;

import dremel.compiler.Compiler;
import dremel.server.impl.ServerImpl;
import dremel.server.Server;


public class ServerTest {
	
	@Test
	public void aspectTest1() {
		ServerImpl srv = new ServerImpl();
		assertTrue(srv.getCompiler() instanceof dremel.compiler.impl.Compiler);
		assertTrue(srv.getStream() instanceof dremel.dataset.impl.LocalDirectory);
	}
}