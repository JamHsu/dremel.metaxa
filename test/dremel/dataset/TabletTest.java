package dremel.dataset;

import static org.junit.Assert.*;
import java.util.LinkedList;
import java.util.List;


import org.junit.Test;

import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.tableton.ColumnWriter;
import dremel.tableton.SchemaColumnar;
import dremel.tableton.Tablet;
import dremel.tableton.TabletIterator;
import dremel.tableton.ColumnMetaData.ColumnType;
import dremel.tableton.ColumnMetaData.EncodingType;
import dremel.tableton.impl.ColumnReaderImpl;
import dremel.tableton.impl.ColumnWriterImpl;
import dremel.tableton.impl.SchemaColumnarImpl;
import dremel.tableton.impl.TabletBuilderImpl;
import dremel.tableton.impl.TabletImpl;

public class TabletTest {


	
	
	public void buildBigStringData(ColumnMetaData columnMetaData, int numberOfStringsToGenerate, String exampleOfString)
	{
		ColumnWriter columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		for(int i =0; i<numberOfStringsToGenerate; i++)
		{
			columnBuilder.addStringDataTriple(exampleOfString, ColumnReader.NOT_NULL, (byte)0, (byte)2);
		}
		
		columnBuilder.close();
	
	}
	
	public void buildNameUrlData(ColumnMetaData columnMetaData)
	{
		ColumnWriter columnBuilder = new ColumnWriterImpl(columnMetaData);
		// write data
		columnBuilder.addStringDataTriple("http://A", ColumnReader.NOT_NULL, (byte)0, (byte)2);
		columnBuilder.addStringDataTriple("http://B", ColumnReader.NOT_NULL, (byte)1, (byte)2);
		columnBuilder.addStringDataTriple(null, ColumnReader.NULL, (byte)1, (byte)1);
		columnBuilder.addStringDataTriple("http://C", ColumnReader.NULL, (byte)0, (byte)2);
		
		columnBuilder.close();
	
	}
	
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
	
	
	public static float calculateMBsSpeed(long sizeInBytes, long timeInMills)
	{
		float fSize = sizeInBytes;
		float fTime = timeInMills;
		float res = (fSize/(1024*1024))/(fTime/1000);
		return res;
	}
	
	@Test 
	public void testStringColumnOnBiggerData()
	{
		ColumnMetaData bigStringMetaData= new ColumnMetaData("bigStringColumn", ColumnType.STRING, EncodingType.NONE, "testdata\\bigStringColumn", (byte)1, (byte)2);
		int NUMBER_OF_STRINGS = 100000;
		String exampleString = "SomeStringBiggerAndBigger";
		buildBigStringData(bigStringMetaData, NUMBER_OF_STRINGS, exampleString);
		ColumnReader reader = new ColumnReaderImpl(bigStringMetaData);
		boolean hasMoreData = true;
		long startTime = System.currentTimeMillis();
		byte[] dataBuffer = new byte[1024*264];
		while(hasMoreData)
		{
			int bytesRead = reader.fillStringRowData(dataBuffer);
			if(bytesRead == ColumnReader.NO_MORE_DATA)
			{
				hasMoreData = false;
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Speed of strings reading is "+calculateMBsSpeed(NUMBER_OF_STRINGS*exampleString.length(), (endTime-startTime)));
		
		assertTrue(true);
	}
	
	@Test 
	public void testStringColumn()
	{
		ColumnMetaData nameUrlMetaData= new ColumnMetaData("Name.url", ColumnType.STRING, EncodingType.NONE, "testdata\\nameUrl", (byte)1, (byte)2);
		buildNameUrlData(nameUrlMetaData);
		ColumnReader reader = new ColumnReaderImpl(nameUrlMetaData);
		
		
		// try to read  into buffer which is too small		
		byte[] smallDataBuffer = new byte[5]; // buffer too small even for one element
		int bytesRead = reader.fillStringRowData( smallDataBuffer);				
		assertEquals(ColumnReader.NOT_ENOUGH_SPACE, bytesRead);
		
		
		byte[] dataBuffer = new byte[1024];
		byte[] repetitionBuffer = new byte[3];
		boolean[] isNullBuffer = new boolean[3];
		bytesRead = reader.fillStringRowData( dataBuffer);		
				
		int elementsRead = reader.filllevelData(repetitionBuffer, isNullBuffer);
		assertEquals(elementsRead,3);
		
		List<String> columnElements  = readStringRowData(dataBuffer, bytesRead);
		
		
		assertEquals(repetitionBuffer[0],0);
		assertEquals(repetitionBuffer[1],1);
		assertEquals(repetitionBuffer[2],1);
		
		assertEquals(isNullBuffer[0],false);
		assertEquals(isNullBuffer[1],false);
		assertEquals(isNullBuffer[2],true);
		
		assertEquals(columnElements.get(0),"http://A");
		assertEquals(columnElements.get(1),"http://B");

		
		// try read when there is no more elements
		bytesRead = reader.fillStringRowData( dataBuffer);
		assertEquals(ColumnReader.NO_MORE_DATA, bytesRead);
		
		elementsRead = reader.filllevelData(repetitionBuffer, isNullBuffer);
		assertEquals(elementsRead, 1);
										
		assertEquals(repetitionBuffer[0],0);				
		assertEquals(isNullBuffer[0],false);				

	}
	
	
	 /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

	
	/**
	 * @param dataBuffer
	 * @param elementsRead
	 * @param isNullBuffer
	 * @return
	 */
	private List<String> readStringRowData(byte[] dataBuffer, int bytesInBuffer) {
		
		int bytesProcessed=0;
		List<String> result = new LinkedList<String>();
		//ByteArrayInputStream byteInput=new ByteArrayInputStream(dataBuffer);
		
		 
		
		int pos =0;
		while (bytesProcessed<bytesInBuffer)
		{
			int strLength = byteArrayToInt(dataBuffer,pos);
			pos+=4;
			String str=new String(dataBuffer, pos, strLength);
			pos+=strLength;
			bytesProcessed+=strLength+4;
			result.add(str);
		}
		return result;
	}

	@Test 
	public void testReadValuesInterface()
	{
		// build single column tablet for the input
		ColumnMetaData linksBackwardMetaData= new ColumnMetaData("Links.LinksBackward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksBackward", (byte)1, (byte)2);
		buildLinkBackwardData(linksBackwardMetaData);
		ColumnReader reader = new ColumnReaderImpl(linksBackwardMetaData);
		
		int bufferSize = 2;
		
		int[] dataBuffer = new int[bufferSize];
		byte[] repetitionBuffer = new byte[bufferSize];
		boolean[] isNullBuffer  = new boolean[bufferSize];;
		int readSize = reader.fillIntValues(dataBuffer, repetitionBuffer, isNullBuffer);
		assertEquals(bufferSize,readSize);
		
		int[] expectedDataBuffer = new int[bufferSize];
		expectedDataBuffer[0] = 0;
        expectedDataBuffer[1] = 10;
        
		
		byte[] expectedRepetitionBuffer = new byte[bufferSize];
		expectedRepetitionBuffer[0] = 0;
		expectedRepetitionBuffer[1] = 0;
		
		
		boolean[] expectedisNullBuffer  = new boolean[bufferSize];;
		expectedisNullBuffer[0] = true;
		expectedisNullBuffer[1] = false;
		
		
		assertArrayEquals(expectedDataBuffer,dataBuffer);
		assertArrayEquals(expectedRepetitionBuffer,repetitionBuffer);
		assertBooleanArraysEquals(expectedisNullBuffer,isNullBuffer);
		
		// read last value from for all buffers
		readSize = reader.fillIntValues(dataBuffer, repetitionBuffer, isNullBuffer);
		assertEquals(1,readSize);
		
		expectedDataBuffer[0] = 30;
		expectedRepetitionBuffer[0] = 1;
		expectedisNullBuffer[0] = false;
		
		readSize = reader.fillIntValues(dataBuffer, repetitionBuffer, isNullBuffer);		
		assertEquals(readSize, ColumnReader.NO_MORE_DATA);
	}
	
	/**
	 * @param expectedisNullBuffer
	 * @param isNullBuffer
	 */
	private void assertBooleanArraysEquals(boolean[] first,
			boolean[] second) {
		assertEquals(first.length, second.length);
		for(int i=0; i<first.length; i++)
		{
			assertEquals(first[i], second[i]);
		}		
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
		
		if(firstTablet.getSchemaColumnar().getColumnsMetaData().keySet().size() != secondTablet.getSchemaColumnar().getColumnsMetaData().keySet().size())
		{
			return false;
		}
		
		for(String columnName : firstTablet.getSchemaColumnar().getColumnsMetaData().keySet())
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
