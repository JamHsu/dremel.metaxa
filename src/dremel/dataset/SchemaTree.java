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
package dremel.dataset;

import java.util.List;


/**
 * @author Kit
 *
 */
public interface SchemaTree {

	public abstract String getName();
	public abstract String getFullName();
	public int getDefLevel();
	public int getRepLevel();
	public SchemaTree getParent();

	/**
	 * @return
	 */
	public abstract boolean isRepeated();

	/**
	 * @return
	 */
	public abstract boolean isRecord();

	/**
	 * @return
	 */
	public abstract List<SchemaTree> getFieldsList();

	/**
	 * @return
	 */
	public abstract boolean isTypeInt64();

	/**
	 * @return
	 */
	public abstract boolean isTypeString();

	/**
	 * @return
	 */
	public abstract boolean isTypeFloat();

	/**
	 * @return
	 */
	public abstract boolean isTypeBool();

	/**
	 * @return
	 */
	boolean isRequired();

	/**
	 * @return
	 */
	boolean isOptional();

}