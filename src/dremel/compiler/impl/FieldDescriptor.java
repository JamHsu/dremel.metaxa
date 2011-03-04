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
package dremel.compiler.impl;

import java.util.List;

import dremel.compiler.impl.Descriptor;
import dremel.compiler.impl.SchemaTree.NodeType;

/**
 * @author Constantine Peresypkin
 *
 */
public class FieldDescriptor {

	/**
	 * @author Constantine Peresypkin
	 *
	 */
	SchemaTree schema;
	
//	public enum Type {
//		GROUP
//	}

	/**
	 * @return
	 */
	public boolean isRepeated() {
		if (schema.getType() == NodeType.ARRAY)
			return true;
		return false;
	}

	/**
	 * @return
	 */
	public Object getType() {
		return schema.getType();
	}

	/**
	 * @return
	 */
	public Descriptor getMessageType() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * @return
	 */
	public String getName() {
		return schema.getName();
	}

//	/**
//	 * @return
//	 */
//	public String getFullName() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	/**
	 * @return
	 */
	public List<FieldDescriptor> getFields() {
		// TODO Auto-generated method stub
		return null;
	}
	
//	/**
//	 * @return
//	 */
//	public FieldDescriptor getRecordType() {
//		// TODO Auto-generated method stub
//		return null;
//	}
}
