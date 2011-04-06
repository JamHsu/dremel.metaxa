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

/**
 * Circular Buffer for slice caching. This implementation is not thread safe but
 * can access by two threads (one for reading, one for writing). This class is
 * accessed by well generated executor code so this implementation will reduce
 * almost memory checking operations to gain the performance.
 * 
 * @author nhsan
 * 
 */

public class SlicePool2 {

	int buffer_size; // can be maximum 2GB
	byte[] buffer;
	int slice_count;
	int end;
	int start;
	byte lastState;
	boolean more = true;

	Object readLock = new Object();
	Object writeLock = new Object();

	public boolean hasMore() {
		return (more || (slice_count > 0));
	}

	public void endPool() {
		more = false;
	}

	public SlicePool2(int size) {
		slice_count = 0;
		buffer_size = size;
		buffer = new byte[size];
		end = 0;
		start = 0;
		lastState = -1;
	}

	public boolean prepareSliceRead() {
		if (slice_count > 0) {
			int missing = buffer[start];

			if (missing == -1) {
				start = 0;
				missing = buffer[0];
			}

			if (missing == 0) {
				start++;
				return true;
			}
		}

		synchronized (readLock) {
			try {
				// System.out.println("Wait for read:"+this.hashCode());
				readLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public byte getLevel() {
		byte tmp = buffer[start];
		start++;
		return tmp;
	}

	public byte getLastState() {
		return lastState;
	}

	public int readInt() {
		lastState = buffer[start++];

		if (lastState == 0) {
			// big-endian
			int tmp = (buffer[start] << 24) + ((buffer[start + 1] & 0xFF) << 16) + ((buffer[start + 2] & 0xFF) << 8) + (buffer[start + 3] & 0xFF);
			start += 4;
			return tmp;
		}
		return 0;
	}

	public double readFloat() {
		lastState = buffer[start++];

		if (lastState == 0) {
			// big-endian
			long tmp = ((long) (0xff & buffer[start]) << 56 | (long) (0xff & buffer[start + 1]) << 48 | (long) (0xff & buffer[start + 2]) << 40 | (long) (0xff & buffer[start + 3]) << 32 | (long) (0xff & buffer[start + 4]) << 24
					| (long) (0xff & buffer[start + 5]) << 16 | (long) (0xff & buffer[start + 6]) << 8 | (long) (0xff & buffer[start + 7]));
			start += 8;
			return Double.longBitsToDouble(tmp);
		}
		return 0;
	}

	public String readString() {
		return null;
	}

	public void endSliceRead() {
		slice_count--;
		synchronized (writeLock) {
			writeLock.notify();
		}
	}

	public int writeSlice(byte[] slice, int length) {
		int st = start; // avoid start changed
		if (end >= st) {
			if (end + length < buffer_size) {
				System.arraycopy(slice, 0, buffer, end, length);
				end += length;
				endSliceWrite();
				return (end - length);
			} else {
				if (length < st) {
					buffer[end] = -1;
					System.arraycopy(slice, 0, buffer, 0, length);
					end = length;
					endSliceWrite();
					return 0;
				}
			}
		} else if (end + length < st) {
			System.arraycopy(slice, 0, buffer, end, length);
			end += length;
			endSliceWrite();
			return (end - length);
		}

		synchronized (writeLock) {
			try {
				writeLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return -1;
	}

	public void endSliceWrite() {
		slice_count++;
		synchronized (readLock) {
			readLock.notify();
		}
	}

	public void updateIntAggValue(int slicePos, int offset, int val) {
		buffer[slicePos]--;// = buffer[slicePos]-1;
		buffer[slicePos + offset] = (byte) (val >> 24);
		buffer[slicePos + offset + 1] = (byte) (val >> 16);
		buffer[slicePos + offset + 2] = (byte) (val >> 8);
		buffer[slicePos + offset + 3] = (byte) (val);
	}
}
