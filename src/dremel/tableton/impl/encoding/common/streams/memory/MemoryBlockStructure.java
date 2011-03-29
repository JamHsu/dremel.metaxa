
/**
   Copyright 2010, BigDataCraft.Com Ltd.
   Oleg Gibaev

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
package dremel.tableton.impl.encoding.common.streams.memory;

import java.io.*;
import dremel.tableton.impl.encoding.common.utils.Bits;

/**
 * Describes structure of the block
 * @author babay
 *
 */
public class MemoryBlockStructure {
	public static final int BLOCK_SIZE = 4096;
	public static final int SIGNATURE = 0x1234;
	protected static final int BYTES_IN_INTEGER = Integer.SIZE / Byte.SIZE; 
	
	protected int signature = SIGNATURE;
	protected int dataSize;
	
	/**
	 * Returns actual block size
	 * @return
	 */
	public static int getBlockSize() {
		return BYTES_IN_INTEGER * 2;
	}
	
	/**
	 * Sets size of data which will be stored in this block 
	 * @param dataSize
	 */
	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}
	
	/**
	 * Returns size of data stored in given block
	 * @return
	 */
	public int getDataSize() {
		return this.dataSize;
	}
	
	/**
	 * Writes object into the given buffer
	 * @param buf - user allocated buffer where memory block is written
	 * @param pos - starting position; usually is 0
	 * @return
	 */
	public int write(byte[] buf, int pos) {
		int data[] = {signature, dataSize}; 
		
		for(int i = 0; i < data.length; i++) {
			Bits.putInt(buf, pos, data[i]);
			pos += BYTES_IN_INTEGER;
		}
		return pos;
	}

	/**
	 * Reads block's data from the given buffer and fills internal members
	 * @param buf
	 * @param pos
	 * @return
	 */
	public int read(byte[] buf, int pos) throws IOException {
		int data[] = {0, 0}; 
		
		for(int i = 0; i < data.length; i++) {
			data[i] = Bits.getInt(buf, pos);
			pos += BYTES_IN_INTEGER;
		}
		signature = data[0];
		if (signature != SIGNATURE) {
			throw new IOException("Invalid signature; read '" + signature + "' instead of the '" + SIGNATURE + "'");
		}
		dataSize = data[1];
		return pos;
	}
}
