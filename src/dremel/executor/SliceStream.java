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
 * @author nhsan
 * 
 */
public class SliceStream {
	ByteBuffer r_buffer;
	ByteBuffer w_buffer;
	int _count;
	int first;
	int last;

	/**
	 * 
	 */
	public SliceStream(ByteBuffer buffer) {
		r_buffer = buffer.duplicate();
		w_buffer = buffer.duplicate();
		_count = 0;
	}

	public void prepareWrite(int size) {
	}

	public void endWrite(int pos) {
		if (_count == 0) {
			first = pos;
			last = pos;
		} else {
			w_buffer.putInt(last+4,pos);
		}
		_count++;
	}
	
	public void prepareRead() {
	}

	public int count() {
		return _count;
	}
}
