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
package dremel.dataset.impl;

import dremel.dataset.Stream;
import dremel.dataset.ReaderTree;
import dremel.dataset.Stream.Codec;
import dremel.tableton.Schema;

/**
 * Stores and retrieves dataset to/from individual files within predefined directory. Locator is 
 * relative path to the file.
 *
 */
public class LocalDirectory implements Stream {

	/* (non-Javadoc)
	 * @see dremel.dataset.Stream#openForRead(java.lang.String, java.lang.String, dremel.dataset.Stream.Codec)
	 */
	@Override
	public ReaderTree openForRead(String dataLocator, String schemaLocator,
			Codec codec) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see dremel.dataset.Stream#write(dremel.dataset.ReaderTree, java.lang.String, dremel.dataset.Stream.Codec)
	 */
	@Override
	public void write(ReaderTree source, String dataLocator, Codec codec) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see dremel.dataset.Stream#writeSchema(dremel.dataset.Schema, java.lang.String, dremel.dataset.Stream.Codec)
	 */
	@Override
	public void writeSchema(Schema schema, String schemaLocator, Codec codec) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see dremel.dataset.Stream#readSchema(java.lang.String, dremel.dataset.Stream.Codec)
	 */
	@Override
	public Schema readSchema(String schemaLocator, Codec codec) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// simple 1:1 matching for now, may alter the name later...
	public String getLocatorFromFileName(String fileName) {
		return fileName;
	}
}
