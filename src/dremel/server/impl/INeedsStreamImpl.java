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
package dremel.server.impl;

import java.util.List;

import dremel.dataset.Stream;
import dremel.dataset.Stream.Codec;

/**
 * @author Constantine Peresypkin
 *
 */

// interface for setting correct stream implementation inside Server object
// now has only Input* setters, should get Output* setters soon...
public interface INeedsStreamImpl {

	/* (non-Javadoc)
	 * @see dremel.server.impl.ICreateServerImpl#setStream(dremel.dataset.Stream)
	 */
	public abstract void setStream(Stream stream);
	
	public abstract void setInputSchemaLocator(String schemaLocator);
	public abstract void setInputDataLocator(List<String> dataLocator);
	public abstract void setInputCodec(Codec codec);
}