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
 * Circular Buffer for slice caching, temporal variable. This class should be
 * implemented in C to gain more performance
 * 
 * @author nhsan
 * 
 */
public class MemoryPool {
	ByteBuffer buffer;
	final static int BUF_SIZE = 1024 * 1024; // 1MB
	int start;
	int end;
	boolean gc_running;

	/**
	 * 
	 */
	public MemoryPool() {
		buffer = ByteBuffer.allocateDirect(BUF_SIZE + 4); // always have minimum
															// 4 byte for
															// circular
		start = 0;
		end = 0;
		gc_running = false;
	}

	/**
	 * @return the buffer
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}

	public synchronized int allocate(int size) {
		if (size <= 0) {
			return -1;
		}
		int r_size = size + 4; // including a pointer of block header
		if (end >= start) {
			if (BUF_SIZE - end >= r_size) {
				int ret = end + 4;
				buffer.putInt(end, r_size);
				end += r_size;
				return ret;
			} else if (start >= r_size) {
				buffer.putInt(end, 0); // pointer to zero posision
				buffer.putInt(0, r_size);
				end = r_size;
				return 4;
			}
		} else {
			int avail = start - end;
			if (avail < 0)
				avail = BUF_SIZE - end; // in case start is changed by another
										// thread
			if (avail > r_size) {
				int ret = end + 4;
				buffer.putInt(end, r_size);
				end += r_size;
				return ret;
			}
		}

		return -1;
	}

	public void free(int pos) {
		int size = -buffer.getInt(pos);
		buffer.putInt(size);
		if ((!gc_running) && (pos == start)) {
			gc_running = true;
			while (size <= 0 && start != end) {
				if (size == 0) {
					start = 0;
					size = buffer.getInt(0);
				} else {
					start -= size;
					size = buffer.getInt(start);
				}
			}
			gc_running = false;
		}
	}
}
