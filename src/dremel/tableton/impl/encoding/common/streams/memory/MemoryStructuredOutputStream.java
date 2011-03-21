
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
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Implements memory output stream. 
 * The written data is stored in structured blocks.
 * @author babay
 *
 */
public class MemoryStructuredOutputStream extends OutputStream {
	// stream's current position
	protected int bufDataOffset = MemoryBlockStructure.getBlockSize();
	protected int pos = bufDataOffset;
	protected int blockSize = MemoryBlockStructure.BLOCK_SIZE;
	protected int needles = 0;
	protected int needlesInPrevBlock = 0;
	protected OutputStream out;
	protected boolean flushed = false;

	protected byte[] buf = new byte[MemoryBlockStructure.BLOCK_SIZE];

	// stores block's header
	MemoryBlockStructure mbs = new MemoryBlockStructure();
	
	/**
	 * Sets output stream where data will be streamed 	
	 * @param in
	 */
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}
	
	/**
	 * Sets actual number of stored needles 
	 */
	public void setNumberOfWrittenNeedles(int needles) {
		this.needles = needles;
	}
	
	/**
	 * Closes this output stream and releases any system resources associated with this stream.
	 */
	public void close() {
		flush();
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pos = 0;
	}
    
	/**
	 * Flushes this output stream and forces any buffered output bytes to be written out.
	 */
	public void flush() {
		if (!flushed) {
			mbs.setDataSize(needles - needlesInPrevBlock);
			mbs.write(buf, 0);
			// writes and flushes data
			try {
				out.write(buf, 0, buf.length);				
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Arrays.fill(buf, (byte)0);
			pos = bufDataOffset;
			needlesInPrevBlock = needles;
			flushed = true;
		}
	}
    
	/**
	 * Writes b.length bytes from the specified byte array to this output stream. 
	 */
	public void write(byte[] b) {
		for(int i = 0; i < b.length; i++) {
			write(b[i]);
		}
	}
    
	/**
	 * Writes len bytes from the specified byte array starting at offset off to this output stream. 
	 */
	public void write(byte[] b, int off, int len) {
		int l = off + len;
		for(int i = off; i < l; i++) {
			write(b[i]);
		}
	}
    
	/**
	 * Writes the specified byte to this output stream.
	 */
	public void write(int b) {
		if (pos >= buf.length) {
			flush();
		}
		buf[pos] = (byte)b;
		pos++;
		flushed = false;
	}
	
}