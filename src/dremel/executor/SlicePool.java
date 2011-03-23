/**
 * Copyright 2010, BigDataCraft.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dremel.executor;

import java.nio.ByteBuffer;

/**
 * Circular Buffer for slice caching. This implementation is not thread safe but
 * can access by two threads (one for reading, one for writing). This class is
 * accessed by well generated executor code so this implementation will reduce
 * almost memory checking operations to gain the performance.
 * 
 * @author nhsan
 * 
 */

public class SlicePool {

	int buffer_size; // can be maximum 2GB
	ByteBuffer r_buffer;
	ByteBuffer w_buffer;
	int count;

	/**
	 * 
	 */
	public SlicePool(int bufferSize) {
		buffer_size = bufferSize;
		w_buffer = ByteBuffer.allocateDirect(buffer_size + 4); // always have
																// minimum
																// 4 bytes for
																// circular
																// pointer
		r_buffer = w_buffer.duplicate();
		count = 0;
	}

	/**
	 * @return the buffer
	 */
	public ByteBuffer getReadBuffer() {
		return r_buffer;
	}

	/**
	 * @return the buffer
	 */
	public ByteBuffer getWriteBuffer() {
		return w_buffer;
	}

	/**
	 * - check for available free block in buffer, do rewind if required
	 * 
	 * @param size
	 *            size of slice
	 * @return true if buffer is available for a slice with size bytes
	 */
	public boolean prepareWrite(int size) {
		int end = w_buffer.position();
		int start = r_buffer.position();

		int r_size = size + 4; // including a pointer of block header
		if (end >= start) {
			if (buffer_size - end >= r_size) {
				w_buffer.putInt(r_size);
				return true;
			} else if (start >= r_size) {
				w_buffer.putInt(0); // pointer to zero position
				w_buffer.position(0); // return to head
				w_buffer.putInt(r_size);
				return true;
			}
		} else {
			int avail = start - end;
			if (avail < 0)
				avail = buffer_size - end;
			if (avail >= r_size) {
				w_buffer.putInt(r_size);
				return true;
			}
		}
		return false;
	}

	public void endWrite() {
		count++;
	}

	/**
	 * - get size of next slice, do rewind if required
	 * 
	 * @return size of next slice
	 */
	public int prepareRead() {
		int size = r_buffer.getInt(); // get size of next slice
		if (size == 0) // need return to head
		{
			r_buffer.position(0);
		}
		size = r_buffer.getInt();
		return size;
	}

	public void endRead() {
		count--;
	}

	public int sliceCount() {
		return count;
	}
}
