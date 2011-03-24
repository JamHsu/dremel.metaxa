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
package dremel.tableton.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import dremel.tableton.ColumnMetaData;
import dremel.tableton.ColumnWriter;
import dremel.tableton.impl.encoding.EncodingFactory;
import dremel.tableton.impl.encoding.StreamEncoder;



public class ColumnWriterImpl implements ColumnWriter
{
	DataOutputStream dataOutput;
	DataOutputStream repOutput;
	DataOutputStream defOutput;
	
	public ColumnWriterImpl(ColumnMetaData metaData)
	{
		try {
			
		ColumnFileSet fileSet = metaData.getFileSet();
						
		dataOutput = new DataOutputStream(new FileOutputStream(new File(fileSet.getDataFileName())));
		repOutput = new DataOutputStream(new FileOutputStream(new File(fileSet.getRepFileName())));
		defOutput = new DataOutputStream(new FileOutputStream(new File(fileSet.getDefFileName())));
		
		int dataMagicBytes = DataSetConstants.getMagicByColumnType(metaData.getColumnType());
		
		dataOutput.writeInt(dataMagicBytes);
		repOutput.writeInt(DataSetConstants.REPETITION_COLUMN_MAGIC);
		defOutput.writeInt(DataSetConstants.DEFINITION_COLUMN_MAGIC);
		
		// attach encoding streams if needed
		dataOutput = getEncoderStream(dataOutput, metaData);
		repOutput  = getEncoderStream(repOutput, metaData);
		defOutput  = getEncoderStream(defOutput, metaData);
			
		} catch (IOException e) {
			throw new RuntimeException("Open output file failed ", e);
		}		
	}
		
	
	private DataOutputStream getEncoderStream(DataOutputStream originalStream,
			ColumnMetaData metaData) {
		if(metaData.getEncoding() == ColumnMetaData.EncodingType.NONE)
		{
			return originalStream;
		}
		
		StreamEncoder encoder = EncodingFactory.getEncoder(metaData, originalStream);
		return new DataOutputStream(encoder);
		
	}




	@Override
	public void addIntDataTriple(int data, boolean isNull, byte repLevel, byte defLevel)
	{
		try {

			if(!isNull)
			{ // NULL values are not written to the storage. They are deduced from the definition level during the read
				dataOutput.writeInt(data);				
			}			
				saveLevels(repLevel, defLevel);				
		
			} catch (IOException e) {
				throw new RuntimeException("Writing data to the column failed", e);
		}
	}

	private void saveLevels(byte repLevel, byte defLevel)
	{
		try {
			repOutput.writeByte(repLevel);
			defOutput.writeByte(defLevel);
		} catch (IOException e) {
			throw new RuntimeException("saveLevels data to the column failed", e);
		}
	}

	public void close() {		
		try {
			flashCurrentStringBuffer();
			dataOutput.close();		
			repOutput.close();
			defOutput.close();		
		} catch (IOException e) {
			throw new RuntimeException("Closing data streams failed ",e);
		}
	}

	@Override
	public void setNullValue(byte repLevel, byte defLevel) {
		saveLevels(repLevel, defLevel);			
	}

	 public static byte[] intToByteArray(int value) {
	        byte[] b = new byte[4];
	        for (int i = 0; i < 4; i++) {
	            int offset = (b.length - 1 - i) * 8;
	            b[i] = (byte) ((value >>> offset) & 0xFF);
	        }
	        return b;
	    }


	/* (non-Javadoc)
	 * @see dremel.tableton.ColumnWriter#addStringDataTriple(int, boolean, byte, byte)
	 */
	ByteArrayOutputStream temporaryStringBlock = new ByteArrayOutputStream();
	int currentBytesInBlock = 0;
	
	public static int MAX_STRING_BLOCK_SIZE=1024*64;
	
	@Override
	public void addStringDataTriple(String data, boolean isNull, byte repLevel,
			byte defLevel) {
			
		// write to the repetition and definition levels as usual
		saveLevels(repLevel, defLevel);
		
		if(!isNull)
		{
			if(data.length()+4>MAX_STRING_BLOCK_SIZE)
			{
				throw new RuntimeException("Sting length is too big. Maximum is "+(MAX_STRING_BLOCK_SIZE-4));
			}
			
			try {
				if(currentBytesInBlock+4+data.length()>MAX_STRING_BLOCK_SIZE)
				{
					flashCurrentStringBuffer();
				}
				// add the data into the buffer
					
				temporaryStringBlock.write(intToByteArray(data.length()));
				currentBytesInBlock+=4;
				
				temporaryStringBlock.write(data.getBytes());
				currentBytesInBlock+=data.getBytes().length;
								
			} catch (IOException ex) {
				throw new RuntimeException("addStringDataTriple failed", ex);
			}
		}
	}

	/**
	 * 
	 */
	private void flashCurrentStringBuffer() {		
		try {
			if(temporaryStringBlock.size() > 0)
			{
				dataOutput.write(intToByteArray(temporaryStringBlock.size()));
				byte[] blockData = temporaryStringBlock.toByteArray();
				dataOutput.write(blockData);
				temporaryStringBlock.reset();
			}
		} catch (IOException ex) {
			throw new RuntimeException("flashCurrentStringBuffer failed ", ex);
		}
		
	}
}