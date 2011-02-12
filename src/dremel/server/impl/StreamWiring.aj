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

import java.util.ArrayList;
import java.util.List;

import dremel.dataset.Stream.Codec;
import dremel.dataset.impl.LocalDirectory;
import dremel.dataset.ReaderTree;

/**
 * @author Constantine Peresypkin
 *
 * Aspect based wiring for the Stream interface implementation
 * 
 */
public aspect StreamWiring {

	// point cut that catches construction of new object that needs its Stream implementation set
	// now it's only ServerImpl object
	pointcut streamCreation(INeedsStreamImpl object) :
		execution(INeedsStreamImpl+.new(..))
		&& this(object);
	
	// sets the default implementation to LocalDirectory
	// works by using the point cut "streamCreation"
	after(INeedsStreamImpl object) 
	returning : streamCreation(object) {
		LocalDirectory stream = new LocalDirectory();
		object.setStream(stream);
	}

	// point cut that catches queryImmediate() execution
	pointcut streamFromSchemaFile(String query, String schemaFile, List<String> dataList, INeedsStreamImpl object) :
		execution(ReaderTree INeedsStreamImpl+.queryImmediate(String,String,List<String>))
		&& args(query, schemaFile, dataList) && target(object);
	
	// this advice sets correct Stream implementation before queryImmediate() runs
	// now it is limited to LocalDirectory only, but should include openstack.swift and others later
	before(String query, String schemaFile, List<String> dataList, INeedsStreamImpl object) : 
		streamFromSchemaFile(query,schemaFile,dataList,object)
	{
		if (schemaFile.matches("^local:")) {
			useLocalDirectoryStream(schemaFile, dataList, object);
		}
		else {
			useLocalDirectoryStream(schemaFile, dataList, object);
		}
	}

	// just to prevent copy/paste.
	private void useLocalDirectoryStream(String schemaFile,
			List<String> dataList, INeedsStreamImpl object) {
		LocalDirectory localDirStream = new LocalDirectory();
		List<String> dataLocatorList = new ArrayList<String>();
		for (String dataFile : dataList) {
			dataLocatorList.add(
					localDirStream.getLocatorFromFileName(
							dataFile.replaceFirst("^local:", "")));
		}
		object.setInputDataLocator(dataLocatorList);
		object.setInputSchemaLocator(localDirStream.getLocatorFromFileName(
				schemaFile.replaceFirst("^local:", "")));
		object.setInputCodec(guessCodecFromFile(schemaFile));
		object.setStream(localDirStream);
	}
	
	/**
	 * @param schemaFile
	 * @return
	 */
	// I think we should implement it in the Stream also, but I don't see how exactly yet
	private Codec guessCodecFromFile(String schemaFile) {
		// TODO Auto-generated method stub
		return null;
	}
}
