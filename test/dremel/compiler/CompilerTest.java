package dremel.compiler;

import static org.junit.Assert.assertTrue;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import dremel.compiler.Compiler;
import dremel.compiler.Expression.BinaryOp;
import dremel.compiler.Expression.Constant;
import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Query;
import dremel.compiler.impl.CompilerImpl;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;

public class CompilerTest {

	@Test
	public void expressionTest() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT (docid+links.forward-(links.backward*2)) as links.exp1, count(links.forward) as name.fwd FROM [document] WHERE name.fwd>30 ;");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		assertTrue(query.getSelectExpressions().size() == 2);
		assertTrue(query.getFilter() != null);
		dremel.compiler.Expression exp = query.getSelectExpressions().get(0);
		assertTrue(exp.getRoot() instanceof BinaryOp);
		//assertTrue(exp.getAlias() == null);
		assertTrue(exp.getRoot().getChild(0) instanceof BinaryOp);
		assertTrue(exp.getRoot().getChild(0).getChild(0) instanceof Symbol);
		assertTrue(exp.getRoot().getChild(1) instanceof BinaryOp);
		assertTrue(exp.getRoot().getChild(1).getChild(1) instanceof Constant);
		exp = query.getSelectExpressions().get(1);
		assertTrue(exp.getRoot() instanceof Function);
		assertTrue(exp.getAlias() != null);
	}

	@Test
	public void parseQueryTest() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT (docid), links.forward as fwd, links.forward+links.backward FROM [document], (SELECT (docid), links.forward, links.backward FROM [document]) WHERE fwd>30 GROUP BY links.forward;");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		assertTrue(query.getTables().size() == 1);
		assertTrue(query.getSubQueries().size() == 1);
		assertTrue(query.getSelectExpressions().size() == 3);
		assertTrue(query.getFilter() != null);
		assertTrue(query.getFilter().getAlias() == null);
		assertTrue(query.getFilter().getWithin() == null);
		assertTrue(query.getFilter().getRoot().getChild(0) instanceof Symbol);
		assertTrue(query.getFilter().getRoot().getChild(1) instanceof Constant);
		assertTrue(((Symbol) query.getFilter().getRoot().getChild(0)).getReference() == query.getSelectExpressions().get(1));
		assertTrue(query.getGroupByExpressions().size() == 1);
		Query query2 = query.getSubQueries().get(0);
		assertTrue(query2.getID() == 2);
	}
	
	@Test
	public void subQueryTest() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT \tdocid, links.forward as links.fwd, links.forward+links.backward FROM (SELECT \tdocid, links.forward, links.backward FROM [document]) WHERE links.fwd>30 GROUP BY links.forward;");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		//assertTrue(query.getTables().size() == 1);
		assertTrue(query.getSubQueries().size() == 1);
		assertTrue(query.getSelectExpressions().size() == 3);
		assertTrue(query.getFilter() != null);
		assertTrue(query.getFilter().getAlias() == null);
		assertTrue(query.getFilter().getWithin() == null);
		assertTrue(query.getFilter().getRoot().getChild(0) instanceof Symbol);
		assertTrue(query.getFilter().getRoot().getChild(1) instanceof Constant);
		assertTrue(((Symbol) query.getFilter().getRoot().getChild(0)).getReference() == query.getSelectExpressions().get(1));
		assertTrue(query.getGroupByExpressions().size() == 1);
		Query query2 = query.getSubQueries().get(0);
		//assertTrue(query2.getID() == 2);
	}
}
