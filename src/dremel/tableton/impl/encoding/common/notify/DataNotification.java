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
package dremel.tableton.impl.encoding.common.notify;

/**
 * This interface s is used in order to notify stream that new data has been read 
 * @author babay
 *
 */
public interface DataNotification {
	/**
	 * Called when new data block has been read
	 * @return
	 */
	public int notifyDataBlockRead(java.io.InputStream in);
}
