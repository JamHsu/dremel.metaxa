package dremel.dataset;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import dremel.executor.Executor;
import dremel.executor.Executor.Script;
import dremel.executor.impl.MetaxaExecutor;
//import dremel.compiler.expression.Symbol;
//import dremel.compiler.impl.MetaxaQuery;
import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.dataset.SliceScanner;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.TabletIterator;
import dremel.tableton.ColumnMetaData.ColumnType;
import dremel.tableton.ColumnMetaData.EncodingType;
import dremel.tableton.impl.ColumnWriterImpl;
import dremel.tableton.impl.SchemaColumnarImpl;
import dremel.tableton.impl.TabletImpl;
import dremel.dataset.impl.TabletIteratorAdapter;
import dremel.compiler.Compiler;
import dremel.compiler.Query;
import dremel.compiler.impl.CompilerImpl;
import dremel.compiler.impl.DefaultQuery;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;

public class tabletIntegrationTest {
	
	public void buildLinkBackwardData(ColumnMetaData columnMetaData)
	{
		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		columnBuilder.addIntDataTriple(0, ColumnReader.NULL, (byte)0, (byte)1);
		columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte)0, (byte)2);
		columnBuilder.addIntDataTriple(30, ColumnReader.NOT_NULL, (byte)1, (byte)2);
		
		columnBuilder.close();
	
	}
	
	public void buildLinksForwardData(ColumnMetaData columnMetaData)
	{
		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		columnBuilder.addIntDataTriple(20, ColumnReader.NOT_NULL, (byte)0, (byte)2);
		columnBuilder.addIntDataTriple(40, ColumnReader.NOT_NULL, (byte)1, (byte)2);
		columnBuilder.addIntDataTriple(60, ColumnReader.NOT_NULL, (byte)1, (byte)2);
		columnBuilder.addIntDataTriple(80, ColumnReader.NOT_NULL, (byte)0, (byte)2);
		
		columnBuilder.close();	
	}
	
	private void buildDocIDData(ColumnMetaData docidMetaData) {
		
		ColumnWriterImpl columnBuilder = new ColumnWriterImpl(docidMetaData);
		// write data
		columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte)0, (byte)0);
		columnBuilder.addIntDataTriple(20, ColumnReader.NOT_NULL, (byte)0, (byte)0);
		
		
		columnBuilder.close();
		
	}
		
	
	public TabletIterator getPaperSchemaTabletIterator()
	{
		// build single column tablet for the input
		ColumnMetaData linksBackwardMetaData= new ColumnMetaData("links.Backward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksBackward", (byte)1, (byte)2);
		buildLinkBackwardData(linksBackwardMetaData);
		
		ColumnMetaData linksForwardMetaData= new ColumnMetaData("links.Forward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksForward", (byte)1, (byte)2);
		buildLinksForwardData(linksForwardMetaData);
		
		ColumnMetaData docidMetaData= new ColumnMetaData("DocId", ColumnType.INT, EncodingType.NONE, "testdata\\docid", (byte)0, (byte)0);
		buildDocIDData(docidMetaData);
				
		SchemaColumnar schema = new SchemaColumnarImpl();
		schema.addColumnMetaData(linksBackwardMetaData);
		schema.addColumnMetaData(linksForwardMetaData);
		schema.addColumnMetaData(docidMetaData);
		
		Tablet tablet = new TabletImpl(schema);
						
		return tablet.getIterator();
	}


	@Test
	public void testExecutorWithTablet() throws RecognitionException
	{
		//test with data in the paper for below BQL
		
		AstNode nodes = Parser.parseBql("SELECT \ndocid, links.forward as fwd, links.backward as bwd, links.forward+links.backward FROM [document] WHERE fwd>60;");
						
		//DefaultQuery query = new DefaultQuery(nodes);
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);
		
		TabletIterator tabletIterator = getPaperSchemaTabletIterator();
		
		List<String> fieldsOrder = new LinkedList<String>();
			
		buildFieldsOrder(query, fieldsOrder);
					
		SliceScanner scanner = new TabletIteratorAdapter(tabletIterator, fieldsOrder);
		
		try {
			Script script = new MetaxaExecutor.JavaLangScript(compiler.compileToScript(query));
			Executor executor = new MetaxaExecutor(query, scanner, script);
			executor.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void buildFieldsOrder(Query query, List<String> fieldsOrder) {
		Iterator<Symbol> it = query.getSymbolTable().values().iterator();
		int i = 0;
		while (it.hasNext()) {
			Symbol symbol = it.next();
			if (symbol.getReference() instanceof SchemaTree) {
				symbol.setSliceMappingIndex(i++);
				fieldsOrder.add(((SchemaTree) symbol.getReference()).getName());
			}
		}
	}

	
}
