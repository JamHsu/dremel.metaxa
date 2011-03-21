/**
   Copyright 2010, BigDataCraft.Com Ltd.
   David Gruzman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.Ope
*/
package dremel.tableton;

/**
 * This interface of the ColumnReader. It enable both regular value by value access, and the compression aware access.
 * @author David.Gruzman
 *
 */
public interface ColumnReader {
	public static final int EOF_REPETITION_LEVEL = 0; // value to be returned when nextRepetitionLevel is called for the last element in column
	public static final boolean NOT_NULL = false;
	public static final boolean NULL = true;
		
	public ColumnMetaData.ColumnType getDataType();
	public boolean isNull();
	public int getRepetitionLevel();
	public int nextRepetitionLevel();
	
	public boolean next();
	
	/**
	 * GetXXXValue methods should be called according to the result of getDataType() method result
	 * @return
	 */
	public int getIntValue();
	public int getLongValue();
	public byte getByteValue();
	public String getStringValue();
	
	// high performance methods for the data reading.
	/**
	 * fillXXXValues method get as a parameter buffer to be filled with the decoded values, the buffer of repetition values and isNull indications 
	 * return value indicates how many elements was actually filled. if there is enough data left in the column - the result
	 * expected to be equal to dataBuffer.length
	 * @param dataBuffer
	 * @return indicates how many elements was actually filled. if there is enough data left in the column - the result. NO_MORE_DATA indicates taht there is no more data.
	 */
	
	public int fillByteValues(byte[] dataBuffer, byte[] repetitionBuffer, boolean[] isNullBuffer);
	
	public int fillIntValues(int[] dataBuffer, byte[] repetitionBuffer, boolean[] isNullBuffer);
	
	
	
	//----------------------- String columns related methods -----------------------
	/**
	 * @return size in bytes needed to store the column's dictionary.
	 */
	public int getDictionaryDataSize();
	/**
	 * Fills the given buffer with the string dictionary in the following format: String length, String bytes, String length ... (similar to the COM's BSTR).
	 * String bytes can be used as is if QP is smart enough of String can be instantiated from the bytes if needed. Bytes are built
	 * using UTF8 encoding. Selection of the best suited encoding require a bit more investigation.
	 * @param dataBuffer - allocated buffer to hold the dictionary. It is supposed to be big enough to hold it in full.
	 * @return size in bytes of the data filled in the dataBuffer. If data buffer is not big enough for the dictionary NOT_ENOUGH_SPACE constant is returned 
	 */
	public int fillStringDictionary(byte[] dataBuffer);
	/**
	 * This method fills the given array with the offsets of the corresponding strings in the dictionary returned by the fillStringDictionary call 
	 * @param dataBuffer
	 * @return number of entries actually filled in the dataBuffer. NO_MORE_DATA returned if there is no more data.
	 */
	public int fillStringOffsets(int[] dataBuffer, byte[] repetitionBuffer, boolean[] isNullBuffer);
		
	//------------------------ constants ---------------------------------
	public static final int NOT_ENOUGH_SPACE = -2;
	public static final int NO_MORE_DATA = -1;
}
