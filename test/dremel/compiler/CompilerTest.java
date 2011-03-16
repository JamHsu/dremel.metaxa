package dremel.compiler;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.antlr.runtime.RecognitionException;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.junit.Test;

import com.google.protobuf.Descriptors.FieldDescriptor;

//import schema.Schema.Document;

import dremel.compiler.Compiler;
import dremel.compiler.Expression.BinaryOp;
import dremel.compiler.Expression.Constant;
import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Node;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Query;
import dremel.compiler.impl.CompilerImpl;
import dremel.compiler.impl.Expression;
import dremel.executor.Executor;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;

public class CompilerTest {

	@Test
	public void testExpression() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT (3+w-6)+count(*)-length(concat(f1,f2)) as exp1 FROM [table1]");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		AstNode node = (AstNode) nodes.getChild(1);
		Node exp = Expression.buildNode((AstNode) node.getChild(0).getChild(0), query);

		assertTrue(exp instanceof BinaryOp); // subtract
		BinaryOp bin1 = (BinaryOp) exp;
		assertTrue(bin1.getOperator().equals("-"));
		assertTrue(exp.getChild(0) instanceof BinaryOp); // add
		BinaryOp bin2 = (BinaryOp) exp.getChild(0);
		assertTrue(bin2.getOperator().equals("+"));
		assertTrue(exp.getChild(1) instanceof Function);
		assertTrue(((Function) exp.getChild(1)).getName().equals("length"));
		assertTrue(exp.getChild(1).getChild(0) instanceof Function);
		assertTrue(exp.getChild(1).getChild(0).getChildCount() == 2);
		assertTrue(exp.getChild(1).getChild(0).getChild(0).generateCode().equals("f1"));
		assertTrue(exp.getChild(1).getChild(0).getChild(1).generateCode().equals("f2"));
		assertTrue(exp.generateCode().equals("((((3 + w) - 6) + count(*)) - length(concat(f1, f2)))"));
	}

	@Test
	public void testQuery1() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT word as w, f1, sum(m1.f2) within m1 as s FROM [table1] where w>30 AND s<1000");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);

		assertTrue(query.getTables().size() == 1);
		//assertTrue(query.getTables().get(0).getTableName().equals("[table1]"));
		assertTrue(query.getSubQueries().size() == 0);// no subqueries
		assertTrue(query.getFilter() != null);
		assertTrue(query.getFilter().getRoot() instanceof BinaryOp);
		assertTrue(((BinaryOp) query.getFilter().getRoot()).getOperator().equals("&&"));
		assertTrue(query.getSelectExpressions().size() == 3);
		assertTrue(query.getSelectExpressions().get(0).getAlias().equals("w"));
		assertTrue(query.getSelectExpressions().get(0).getWithin() == null);
		assertTrue(query.getSelectExpressions().get(0).getRoot() instanceof Symbol);
		assertTrue(query.getSelectExpressions().get(1).getAlias().equals("f1"));
		assertTrue(query.getSelectExpressions().get(1).getWithin() == null);
		assertTrue(query.getSelectExpressions().get(1).getRoot() instanceof Symbol);
		assertTrue(query.getSelectExpressions().get(2).getAlias().equals("s"));
		assertTrue(query.getSelectExpressions().get(2).getWithin().equals("m1"));
		assertTrue(query.getSelectExpressions().get(2).getRoot() instanceof Function);

		assertTrue(query.getGroupByExpressions().size() == 0);
		assertTrue(query.getOrderByExpressions().size() == 0);
	}

	@Test
	public void testQuery2() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT word as w, count(f1) within record FROM [table1]");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		assertTrue(query.getFilter() == null);
		assertTrue(query.getSelectExpressions().get(1).getWithin().equals("RECORD"));

	}

	@Test
	public void testCompiler1() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT \ndocid, links.forward as fwd, links.forward+links.backward FROM [document] WHERE fwd>30;");
		// FIXME: \ndocid -> error somewhere in BQL parser, not accept a field
		// (alias) started with letter 'd'
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);

		assertTrue(query.getTables().size() == 1);
		//assertTrue(query.getTables().get(0).getTableName().equals("[document]"));
		//assertTrue(query.getTables().get(0).getSchema() == Document.getDescriptor());
		assertTrue(query.getSubQueries().size() == 0);

		Iterator<Symbol> it = query.getSymbolTable().values().iterator();

		while (it.hasNext()) {
			Symbol symbol = it.next();
			assertTrue(symbol.getReference() != null);// no symbol without
														// reference
		}
		assertTrue(query.getSelectExpressions().size() == 3);
		assertTrue(query.getSelectExpressions().get(0).getRepetitionLevel() == 0);
		assertTrue(query.getSelectExpressions().get(1).getRepetitionLevel() == 1);
		assertTrue(query.getSelectExpressions().get(2).getRepetitionLevel() == 1);
		assertTrue(query.getSelectExpressions().get(0).getRoot() instanceof Symbol);
		assertTrue(((Symbol) query.getSelectExpressions().get(0).getRoot()).getReference() instanceof FieldDescriptor);
		assertTrue(query.getFilter() != null);
		assertTrue(query.getFilter().getAlias() == null);
		assertTrue(query.getFilter().getWithin() == null);
		assertTrue(query.getFilter().getRoot().getChild(0) instanceof Symbol);
		assertTrue(query.getFilter().getRoot().getChild(1) instanceof Constant);
		assertTrue(((Symbol) query.getFilter().getRoot().getChild(0)).getReference() == query.getSelectExpressions().get(1));
		assertTrue(query.getFilter().getRoot().generateCode().equals("(links.forward > 30)"));
		assertTrue(query.getSymbolTable().size() == 4);
	}

	@Test
	public void testCompiler2() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT \ndocid, count(name.language.code) within name.language FROM [document];");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);
		assertTrue(query.getSelectExpressions().get(1).getWithin().equals("name.language"));
		assertTrue(query.getSelectExpressions().get(1).getWithinLevel() == 2);
	}

	@Test
	public void testCompiler3() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT abc FROM [document];"); // abc
																		// is
																		// not a
																		// field
																		// name
																		// of
																		// Document
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);
		assertTrue(query.getSelectExpressions().get(0).getRoot() instanceof Symbol);
		assertTrue(((Symbol) query.getSelectExpressions().get(0).getRoot()).getReference() == null);
	}

//	@Test
//	public void testJaninoScript() throws Exception {
//		AstNode nodes = Parser.parseBql("SELECT \ndocid, links.forward as fwd, links.forward+links.backward FROM [document] WHERE fwd>30;");
//		Compiler compiler = new CompilerImpl();
//		Query query = compiler.parse(nodes);
//		compiler.analyse(query);
//		String code = ((CompilerImpl) compiler).generateCode(query);
//		// System.out.println(code);
//		IScriptEvaluator se = ((CompilerImpl) compiler).createScript(code);
//
//		dremel.dataset.impl.Slice inSlice = new dremel.dataset.impl.Slice(3);
//
//		inSlice.setValue(0, 100);
//		
//		inSlice.setValue(1, null); // null as 0
//		inSlice.setValue(2, 300);
//
//		dremel.dataset.impl.Slice outSlice = new dremel.dataset.impl.Slice(3);
//		Integer[] context1 = new Integer[3];
//		context1[0] = 0;
//
//		se.evaluate(new Object[] { inSlice, outSlice, context1 });
//
//		assertTrue(outSlice.getValue(0) == inSlice.getValue(0));
//		assertTrue(outSlice.intValue(1) == inSlice.intValue(2));
//		assertTrue(outSlice.intValue(2) == inSlice.intValue(2) + inSlice.intValue(1));
//
//		inSlice.setValue(0, 123);
//		inSlice.setValue(1, 4);
//		inSlice.setValue(2, 5);// filter is false
//		context1[0] = 0;
//
//		outSlice = new dremel.dataset.impl.Slice(3);
//		se.evaluate(new Object[] { inSlice, outSlice, context1 });
//		assertTrue(outSlice.intValue(0) == 0);
//		assertTrue(outSlice.intValue(1) == 0);
//		assertTrue(outSlice.intValue(2) == 0);
//
//		inSlice.setValue(0, 123);
//		inSlice.setValue(1, 5);
//		inSlice.setValue(2, 100);
//		context1[0] = 1; // select level
//
//		outSlice = new dremel.dataset.impl.Slice(3);
//		se.evaluate(new Object[] { inSlice, outSlice, context1 });
//		assertTrue(outSlice.intValue(0) == 0);
//		assertTrue(outSlice.intValue(1) == 100);
//		assertTrue(outSlice.intValue(2) == 105);
//	}

	@Test
	public void testExecutor() throws RecognitionException {
		// test with data in the paper for below BQL
		AstNode nodes = Parser.parseBql("SELECT \ndocid, links.forward as fwd, links.forward+links.backward FROM [document] WHERE fwd>0;");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);
		Executor executor = compiler.compile(query);
		executor.execute();
	}
}
