
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import dremel.tableton.impl.encoding.common.notify.DataNotification;
/**
 * Implements memory input stream. 
 * Stream reads data by blocks which were previously written by MemoryStructuredOutputStream 
 * @author babay
 *
 */
public class MemoryStructuredInputStream extends InputStream {
	// stream's current position
	protected int bufDataOffset = MemoryBlockStructure.getBlockSize();
	protected int pos = bufDataOffset;
	protected int blockSize = MemoryBlockStructure.BLOCK_SIZE;
	protected int needles = 0;
	int readBlockBytes = 0;
	protected boolean readNextBlock = true;
	
	protected InputStream in;
	protected DataNotification notify;

	protected byte[] buf = new byte[MemoryBlockStructure.BLOCK_SIZE];

	// stores block's header
	MemoryBlockStructure mbs = new MemoryBlockStructure();
	
	/**
	 * Sets input stream 	
	 * @param in
	 */
	public void setInputStream(InputStream in, DataNotification notify) {
		this.in = in;
		this.notify = notify;
	}
	
	/**
	 * Gets number of written needles in the last block
	 */
	public int getNumberOfWrittenNeedlesInLastBlock() {
		return mbs.getDataSize();
	}

	/**
	 * Returns number of available bytes  
	 */
    public int available() throws IOException {
    	if (in.available() == 0 && pos >= readBlockBytes) {
    		return 0;
    	}
    	if (pos < readBlockBytes) {
    		return readBlockBytes - pos;
    	}
    	return in.available();
    }
	
	/**
	 * Closes this output stream and releases any system resources associated with this stream.
	 */
	public void close() {
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pos = 0;
	}
    
	/**
	 * Reads next byte
	 */
	public int read() throws IOException {
		if (readNextBlock) {
			Arrays.fill(buf, (byte)0);
			readBlockBytes = in.read(buf);
			if (mbs.read(buf, 0) != bufDataOffset) {
				throw new IOException("Invalid block's header");
			}
			pos = bufDataOffset;
			readNextBlock = false;
			if (notify != null) {
				notify.notifyDataBlockRead(this);
			}
		}
		byte t = buf[pos];
		pos++;
		if (pos >= readBlockBytes) {
			readNextBlock = true;
		}
		return (int)t;
	}
}
