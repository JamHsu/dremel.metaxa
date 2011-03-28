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
import dremel.executor.Executor.Script;
import dremel.executor.impl.MetaxaExecutor;
import dremel.tableton.ColumnReader;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.impl.TabletImpl;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;
import dremel.dataset.SchemaTree;

public class CompilerTest {

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
		assertTrue(((Symbol) query.getSelectExpressions().get(0).getRoot()).getReference() instanceof SchemaTree);
		assertTrue(query.getFilter() != null);
		assertTrue(query.getFilter().getAlias() == null);
		assertTrue(query.getFilter().getWithin() == null);
		assertTrue(query.getFilter().getRoot().getChild(0) instanceof Symbol);
		assertTrue(query.getFilter().getRoot().getChild(1) instanceof Constant);
		assertTrue(((Symbol) query.getFilter().getRoot().getChild(0)).getReference() == query.getSelectExpressions().get(1));
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

	@Test
	public void testExecutor() throws Exception {
		// test with data in the paper for below BQL
		// AstNode nodes =
		// Parser.parseBql("SELECT \ndocid, links.forward, links.backward, links.backward+\ndocid, \ndocid+links.forward, links.forward+links.backward, 3+2 FROM [document] where \ndocid>0 and links.forward>30");
		AstNode nodes = Parser.parseBql("SELECT \ndocid, count(docid) within record, links.forward as exp3, sum(links.forward) within links, links.backward, count(links.backward) within record, 2*3+5 FROM [document] where \ndocid>0 and links.forward>30");
		// AstNode nodes =
		// Parser.parseBql("SELECT \ndocid, links.forward, count(links.forward) within record FROM [document] where \ndocid>0");
		CompilerImpl compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);
		String code = compiler.compileToScript(query);
		Script script = new MetaxaExecutor.JavaLangScript(code);

		SchemaColumnar schema = query.getTargetSchema();

		script.evaluate(new Object[] { query.getTables().get(0), schema });

		Tablet tablet = new TabletImpl(schema);

		boolean hasMoreSlices = true;
		int fetchLevel = 0;

		while (hasMoreSlices) {
			int nextLevel = 0;
			hasMoreSlices = false;
			for (dremel.compiler.Expression exp : query.getSelectExpressions()) {
				ColumnReader nextReader = tablet.getColumns().get(exp.getJavaName());

				if (nextReader.nextRepetitionLevel() >= fetchLevel) {
					boolean isLastInReader = nextReader.next();
					hasMoreSlices = hasMoreSlices || isLastInReader;
					if (hasMoreSlices)
					{
						if (nextReader.isNull())
						{
							System.out.print("NULL\t\t");
						}
						else
						{
							System.out.print(nextReader.getIntValue()+ "\t\t");
						}
					}
				}
				else
				{
					System.out.print("N/A\t\t");
				}
				nextLevel = Math.max(nextLevel, nextReader.nextRepetitionLevel());
			}
			System.out.println();
			fetchLevel = (byte) nextLevel;
		}
	}
}
