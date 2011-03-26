package dremel.dataset;

import org.junit.Test;

import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnReader;
import dremel.tableton.ColumnMetaData.ColumnType;
import dremel.tableton.ColumnMetaData.EncodingType;
import dremel.tableton.impl.SimpleIntColumnReader;
import dremel.tableton.impl.SimpleIntColumnWriter;

public class TabletonTests {
	
	@Test
	public void testWriter() {
		int count = 8000000;
		ColumnMetaData linksBackwardMetaData = new ColumnMetaData("Links.Backward", ColumnType.INT, EncodingType.NONE, "testdata\\LinksBackward", (byte) 1, (byte) 2);
		SimpleIntColumnWriter columnBuilder = new SimpleIntColumnWriter(linksBackwardMetaData);
		// write data
		int c = 0;
		long ts = System.currentTimeMillis();
		for (int i = 0; i < count; ++i) {
			columnBuilder.addIntDataTriple(0, ColumnReader.NULL, (byte) 0, (byte) 1);
			++c;
			columnBuilder.addIntDataTriple(10, ColumnReader.NOT_NULL, (byte) 0, (byte) 2);
			++c;
			columnBuilder.addIntDataTriple(30, ColumnReader.NOT_NULL, (byte) 1, (byte) 2);
			++c;
		}
		System.out.println("write " + (System.currentTimeMillis() - ts));
		ts = System.currentTimeMillis();
		columnBuilder.close();
		System.out.println("force " + (System.currentTimeMillis() - ts));
		System.out.println("wrote items: "+c);
		
/*		int bufLen = 8 * 1024;
		int pos = 0;
		int[] data = new int[bufLen];
		byte[] rep = new byte[bufLen];
		byte[] def = new byte[bufLen];
		boolean[] nulls = new boolean[bufLen];
		
		int[] inData = new int[] {0, 10, 30};
		boolean[] inNulls = new boolean[] { ColumnReader.NULL, ColumnReader.NOT_NULL, ColumnReader.NOT_NULL };
		byte[] inRep = new byte[] { 0, 0, 1 };
		byte[] inDef = new byte[] { 1, 2, 2 };
		
		int j = 0;
		int last = inData.length - 1;
		ColumnMetaData linksBackwardMetaData1 = new ColumnMetaData("Links.Backward1", ColumnType.INT, EncodingType.NONE, "testdata\\LinksBackward1", (byte) 1, (byte) 2);
		SimpleIntColumnWriter columnBuilder1 = new SimpleIntColumnWriter(linksBackwardMetaData1);
		// write data
		ts = System.currentTimeMillis();
		for (int i = 0; i < count; ++i) {
			if (pos == bufLen) {
				columnBuilder1.addIntDataTriple(data, nulls, rep, def);
				pos = 0;
			}
			data[pos] = inData[j];
			nulls[pos] = inNulls[j];
			rep[pos] = inRep[j];
			def[pos] = inDef[j];
			j = (j == last) ? 0 : j + 1;
			++pos;
		}
		System.out.println("write " + (System.currentTimeMillis() - ts));
		ts = System.currentTimeMillis();
		columnBuilder1.close();
		System.out.println("force " + (System.currentTimeMillis() - ts));*/
		
		SimpleIntColumnReader columnReader = new SimpleIntColumnReader(linksBackwardMetaData);
		int cnt = 0;
		ts = System.currentTimeMillis();
		while(true) {
//			if (columnReader.getRepetitionLevel() == 0) {
//				//System.out.println("--------- Slice ----------");
//			}
//			//System.out.println("rep = " + columnReader.getRepetitionLevel() + " def = " + columnReader.getDefLvl());
//			if (columnReader.isNull()) {
//				//System.out.println("data = NULL");
//			} else {
//				//System.out.println("data = " + columnReader.getIntValue());
//			}
//			if (columnReader.nextRepetitionLevel() == 0) {
//				//System.out.println("--------- END ------------");
//			}
			++cnt;
			if(! columnReader.next())
				break;
		}
		System.out.println("read " + (System.currentTimeMillis() - ts));
		System.out.println("read items: " + cnt);
	}
}
