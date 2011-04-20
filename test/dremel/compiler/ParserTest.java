package dremel.compiler;
import static org.junit.Assert.assertTrue;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;

public class ParserTest {

	public static void printNode(AstNode node, int level) {
		for (int i = 0; i < level; i++)
			System.out.print("  ");

		System.out.println(node.toString());

		for (int i = 0; i < node.getChildCount(); i++) {
			AstNode n = (AstNode) node.getChild(i);
			printNode(n, level + 1);
		}
	}

	@Test
	public void SimpleBQLTest() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT (docid), links.forward FROM [document] where (docid=10)");
		assertTrue(nodes.getChildCount()==3); //SELECT+FROM+WHERE
	}
}
