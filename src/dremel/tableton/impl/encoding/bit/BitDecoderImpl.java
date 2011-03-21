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
package dremel.tableton.impl.encoding.bit;

import java.io.IOException;
import java.io.InputStream;

import dremel.tableton.impl.encoding.common.utils.Bits;
import dremel.tableton.impl.encoding.StreamDecoder;
import dremel.tableton.impl.encoding.common.streams.memory.MemoryStructuredInputStream;
import dremel.tableton.impl.encoding.common.notify.DataNotification;
/**
 * Implements Bit decoding scheme
 * @author babay
 *
 */
public class BitDecoderImpl extends StreamDecoder implements DataNotification {

	private static final int MAX_BITS = Integer.SIZE;
	
	byte needle = 0;
	int availableData = 1;
	int data = 0;
	int needleBitWidth = 0;
	int packedNeedlesPerBundle = 0;
	int needlesPerBlock = 0;
	boolean firstTime = true;
	byte[] bin = new byte[MAX_BITS / Byte.SIZE];
	
	public static BitDecoderImpl instance(InputStream encodedIn) {
		assert(encodedIn != null);
		return new BitDecoderImpl(encodedIn);
	}
	
	/**
	 * Constructor
	 */
	private BitDecoderImpl(InputStream encodedIn) {
		MemoryStructuredInputStream msi = new MemoryStructuredInputStream();		
		msi.setInputStream(encodedIn, this);
		super.setInStream(msi);
	}

	/**
	 * Called when new data block has been read
	 * @return
	 */
	public int notifyDataBlockRead(java.io.InputStream in) {
		int needlesRemainedFromPrevBlock = 0;
		if (in instanceof MemoryStructuredInputStream) {
			if (needlesPerBlock != 0) {
				needlesRemainedFromPrevBlock = needlesPerBlock;
			}
			needlesPerBlock = ((MemoryStructuredInputStream)in).getNumberOfWrittenNeedlesInLastBlock() + needlesRemainedFromPrevBlock;
		}
		else {
			needlesPerBlock = -1;
		}
		return 0;
	}
	
	/**
	 * Returns the number of bytes that can be read (or skipped over) from this input stream without blocking by the next caller of a method for this input stream.
	 * Overridden method
	 */
	public int available() {
		try {
			if ((encodedIn.available() == 0 && data == 0) || (error)) {
				availableData = 0;
			}
		} catch (IOException e) {
			error = true;
			availableData = 0;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return availableData;
	}
	
	/**
	 * Closes this input stream and releases any system resources associated with the stream.
	 * Overridden method
	 */
	public void close() {
		super.close();
	}
	
	/**
	 * Reads the next needle from the raw input stream.
	 * Implemented method 
	 */
	public int read() throws IOException {
		checkStreamsDataAvailability();
		if (firstTime) {
			firstTime = false;
			// read bits width
			try {
				for(int i = 0, j = 0; i < MAX_BITS; i += Byte.SIZE, j++) {
					bin[j] = (byte)encodedIn.read();
				}
				needleBitWidth = Bits.getInt(bin, 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				error = true;
				throw e;
			}
			if (needleBitWidth == -1) {
				error = true;
			}
		}
		if (data == 0) {
			try {
				for(int i = 0, j = 0; i < MAX_BITS; i += Byte.SIZE, j++) {
					bin[j] = (byte)encodedIn.read();
				}
				data = Bits.getInt(bin, 0);
			} catch (IOException e) {
				error = true;
				throw e;
			}
		}
		decode();
		if (needle == 0 && needlesPerBlock == -1) {
			throw new IOException();
		}				
		return needle;
	}
	
	/**
	 * implements Bit decoding algorithm 
	 * @throws IOException 
	 */
	protected void decode() {
		needle = (byte)(data & ((1 << needleBitWidth) - 1));
		data >>>= needleBitWidth;
		if (needlesPerBlock >= 0) needlesPerBlock--;
	}
	
}
