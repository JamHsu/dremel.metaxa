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
import java.io.OutputStream;

import dremel.tableton.impl.encoding.common.utils.Bits;
import dremel.tableton.impl.encoding.StreamEncoder;
import dremel.tableton.impl.encoding.common.streams.memory.MemoryStructuredOutputStream;

/**
 * Implements RLE encoding scheme. This class overrides InputStream's functionality
 * @author babay
 *
 */
public class BitEncoderImpl extends StreamEncoder {
	
	private static final int MAX_BITS = Integer.SIZE;
	
	int maxBitsWidth = 0;
	int data = 0;
	int numOfUsedBits = 0;
	int encodedNeedles = 0;
	boolean firstTime = true;
	MemoryStructuredOutputStream mso = new MemoryStructuredOutputStream();
	byte[] bout = new byte[MAX_BITS / Byte.SIZE];
	
	public static BitEncoderImpl instance(OutputStream out, int maxBitsWidth) {
		assert(maxBitsWidth >= 0);
		return new BitEncoderImpl(out, maxBitsWidth);
	}
	
	/**
	 * Constructor
	 */
	private BitEncoderImpl(OutputStream out, int maxBitsWidth) {
		super.setOutputStream(out);
		mso.setOutputStream(out);
		this.maxBitsWidth = maxBitsWidth;
	}
	
	/**
	 * Encodes the specified byte and writes encoded data to given output stream
	 */
	public void write(int b) {
		if (firstTime) {
			firstTime = false;			
			data = maxBitsWidth; // storing max. bandwidth, we need its value for the decoding operation
			writeEncodedData();
		}
		encode(b);
	}
	
	/**
	 * Flushes this output stream and forces any buffered output bytes to be written out.
	 */
	public void flush() {
		super.flush();
		if (data != 0) {
			writeEncodedData();
			mso.flush();
		}
	}

	/**
	 * Closes this output stream and releases any system resources associated with this stream.
	 */
	public void close() {
		super.close();
		flush();
		mso.close();
	}
	
	/**
	 * implements BIT encoding algorithm 
	 * @throws IOException 
	 */
	protected void encode(int b) {
		encodedNeedles++;		
		if (numOfUsedBits + maxBitsWidth > MAX_BITS) {
			writeEncodedData();		
		}				
		data |= b << numOfUsedBits;
		numOfUsedBits += maxBitsWidth;
	}
	
	protected void writeEncodedData() {
		Bits.putInt(bout, 0, data);
		mso.setNumberOfWrittenNeedles(encodedNeedles);
		mso.write(bout);
		data = 0;
		numOfUsedBits = 0;
	}
}
