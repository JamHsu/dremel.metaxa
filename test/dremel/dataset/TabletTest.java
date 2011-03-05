package dremel.dataset;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.tableton.ColumnWriter;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.TabletIterator;
import dremel.tableton.ColumnMetaData.ColumnType;
import dremel.tableton.ColumnMetaData.EncodingType;
import dremel.tableton.impl.ColumnWriterImpl;
import dremel.tableton.impl.SchemaColumnarImpl;
import dremel.tableton.impl.TabletBuilderImpl;
import dremel.tableton.impl.TabletImpl;

public class TabletTest {
	
	
	public void buildLinkBackwardData(ColumnMetaData columnMetaData)
	{
		ColumnWriter columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		columnBuilder.addIntDataTriple(0, ColumnReader.NULL, (byte)0, (byte)1);
		columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte)0, (byte)2);
		columnBuilder.addIntDataTriple(30, ColumnReader.NOT_NULL, (byte)1, (byte)2);
		
		columnBuilder.close();
	
	}
	
	public void buildLinksForwardData(ColumnMetaData columnMetaData)
	{
		ColumnWriter columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		columnBuilder.addIntDataTriple(20, ColumnReader.NOT_NULL, (byte)0, (byte)2);
		columnBuilder.addIntDataTriple(40, ColumnReader.NOT_NULL, (byte)1, (byte)2);
		columnBuilder.addIntDataTriple(60, ColumnReader.NOT_NULL, (byte)1, (byte)2);
		columnBuilder.addIntDataTriple(80, ColumnReader.NOT_NULL, (byte)0, (byte)2);
		
		columnBuilder.close();
	
	}
		
	@Test
	public void twoColumnsTabletRoundtripTest()
	{
		// build single column tablet for the input
		ColumnMetaData linksBackwardMetaData= new ColumnMetaData("Links.LinksBackward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksForward", (byte)1, (byte)2);
		buildLinkBackwardData(linksBackwardMetaData);
		
		ColumnMetaData linksForwardMetaData= new ColumnMetaData("Links.LinksForward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksForward", (byte)1, (byte)2);
		buildLinksForwardData(linksForwardMetaData);
				
		SchemaColumnar schema = new SchemaColumnarImpl();
		schema.addColumnMetaData(linksBackwardMetaData);
		schema.addColumnMetaData(linksForwardMetaData);
		
		Tablet tablet = new TabletImpl(schema);
						
		checkNullCopy(schema, tablet);
	}
	
	@Test
	public void singleColumnTabletRoundtripTest()
	{
		// build single column tablet for the input
		ColumnMetaData columnMetaData= new ColumnMetaData("Links.LinksForward", ColumnType.INT, EncodingType.RLE, "testdata\\LinksForward", (byte)1, (byte)2);
		buildLinkBackwardData(columnMetaData);
		
		
		SchemaColumnar schema = new SchemaColumnarImpl();
		schema.addColumnMetaData(columnMetaData);
		
		Tablet tablet = new TabletImpl(schema);
						
		checkNullCopy(schema, tablet);
	}

	private void checkNullCopy(SchemaColumnar schema, Tablet tablet) {
		TabletIterator tabletIterator = tablet.getIterator();		
		
		// create output tablet
		TabletBuilderImpl outputTablet = new TabletBuilderImpl();
		outputTablet.buildTablet("testdata\\testOutput", tabletIterator);
		// to make fetch / insert loop.
		while(tabletIterator.fetch())
		{
			outputTablet.pushSlice(tabletIterator.getFetchLevel());// pass fetch level as select level because it is NULL-query
		}
		outputTablet.close();		
		// create input tablet over the output
		SchemaColumnar resultSchema = outputTablet.getSchema();
		Tablet resultTablet = new TabletImpl(resultSchema);
		// compare input and output data
		Tablet originalTablet = new TabletImpl(schema);
		assertTrue(compareTablets(resultTablet, originalTablet));
	}

	private boolean compareTablets(Tablet firstTablet, Tablet secondTablet) {
		boolean tabletEquals = true;
		
		if(firstTablet.getSchema().getColumnsMetaData().keySet().size() != secondTablet.getSchema().getColumnsMetaData().keySet().size())
		{
			return false;
		}
		
		for(String columnName : firstTablet.getSchema().getColumnsMetaData().keySet())
		{
			ColumnReader firstColumnReader = firstTablet.getColumns().get(columnName);
			ColumnReader secondColumnReader = secondTablet.getColumns().get(columnName);
			
			tabletEquals = tabletEquals && compareColumnReaders(firstColumnReader, secondColumnReader);
		}
		
		return tabletEquals;
	}

	private boolean compareColumnReaders(ColumnReader firstReader,
			ColumnReader secondReader) {
		boolean columnsEquals = true;
		
		if(firstReader.getDataType() != secondReader.getDataType())
		{
			return false;
		}
		
		while(firstReader.next())
		{
			if(!secondReader.next())
			{
				// we have more values in the first column
				return false;
			}
			
			switch(firstReader.getDataType())
			{
			case INT: 
				columnsEquals = columnsEquals && (firstReader.getIntValue() == secondReader.getIntValue());
				break;
			default: throw new RuntimeException("Unsupported data type");
			}
		}
		
		if(secondReader.next())
		{
			// we have more values in the second column
			return false;
		}
		
		
		return columnsEquals;
	}
}
