package dremel.parser;

public class ParserTest {

	public static void printNode(AstNode node, int level)
	{
		for (int i=0;i<level;i++) System.out.print("  ");
		
		System.out.println(node.toString());
		
		for (int i=0;i<node.getChildCount();i++)
		{
			AstNode n = (AstNode)node.getChild(i);
			printNode(n, level+1);
		}
	}
	
	public static void main(String[] args) {

		try {
			AstNode nodes = Parser
					.parseBql("SELECT word within a.b.c AS w, LENGTH(word/abc,abc,def,123.456), 3*(2)-(1+sin(5))+abc()  FROM [bigquery/samples/shakespeare], (select f1 from table2)  WHERE w = \"word\" order by w;");
			
			ParserTest.printNode(nodes,0);
			//System.out.println();
			//System.out.println(nodes.toStringTree());
			//System.out.println("abc");
			//dremel.compiler.Compiler compiler=new Compiler();
			//compiler.parse(nodes);
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
}
