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

import org.antlr.runtime.RecognitionException;
import org.codehaus.commons.compiler.IScriptEvaluator;

import dremel.dataset.ReaderTree;
import dremel.dataset.Stream.Codec;
import dremel.parser.Parser;
import dremel.server.Server;
import dremel.compiler.Compiler;
import dremel.compiler.Query;
import dremel.compiler.impl.MetaxaQuery;
import dremel.dataset.Stream;
import dremel.executor.Executor;

/**
 * @author Constantine Peresypkin
 *
 */
public class ServerImpl implements Server, INeedsStreamImpl, INeedsCompilerImpl {
	/* (non-Javadoc)
	 * @see dremel.server.Server#queryImmediate(java.lang.String, java.lang.String, java.lang.String)
	 */
	Compiler compiler;
	Stream stream;
	List<String> inputDataLocator;
	String inputSchemaLocator;
	Codec inputCodec;
	Query compiledQuery;
	Executor executor;
	
	// Construction is handled through aspects of Stream and Compiler wiring
	public ServerImpl() {
		
	}
	
	/* (non-Javadoc)
	 * @see dremel.server.impl.ICreateServerImpl#setCompiler(dremel.compiler.Compiler)
	 */
	@Override
	public void setCompiler(Compiler compiler) {
		this.compiler = compiler;
	}

	/* (non-Javadoc)
	 * @see dremel.server.impl.INeedsStreamImpl#setStream(dremel.dataset.Stream)
	 */
	@Override
	public void setStream(Stream stream) {
		this.stream = stream;
	}
	
	@Override
	public ReaderTree queryImmediate(String query, String schemaFilename,
			List<String> dataFilename) {

		List<ReaderTree> rtreeList = new ArrayList<ReaderTree>();
		for (String dataLocator : inputDataLocator) {
			ReaderTree rtree = stream.openForRead(dataLocator, inputSchemaLocator, inputCodec);
			rtreeList.add(rtree);
		}
		
		ReaderTree result = null;
		//Query compiledQuery = new MetaxaQuery(Parser.parseBql(query));
		compiler.analyse(compiledQuery);
		executor.setEvaluator(compiler.compileToScript(compiledQuery));
		executor.execute();
		//result = compiledQuery.link(rtreeList);
		return result;
	}

	/* (non-Javadoc)
	 * @see dremel.server.Server#toFile(dremel.dataset.ReaderTree, java.util.List, dremel.dataset.Stream.Codec, java.lang.String)
	 */
	@Override
	public void toFile(ReaderTree readerTree,
			List<String> destinationDataFileList, Codec destinationCodec,
			String destinationSchemaFilename) {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see dremel.server.Server#toFile(dremel.dataset.ReaderTree, java.util.List, dremel.dataset.Stream.Codec)
	 */
	@Override
	public void toFile(ReaderTree readerTree,
			List<String> destinationDataFileList, Codec destinationCodec) {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see dremel.server.Server#fromFile(dremel.dataset.ReaderTree, java.util.List, dremel.dataset.Stream.Codec, java.lang.String)
	 */
	@Override
	public ReaderTree fromFile(ReaderTree readerTree,
			List<String> sourceDataFileList, Codec sourceCodec,
			String sourceSchemaFilename) {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see dremel.server.Server#convertFile(dremel.dataset.Stream.Codec, java.lang.String, java.util.List, dremel.dataset.Stream.Codec, java.lang.String, java.util.List)
	 */
	@Override
	public void convertFile(Codec sourceCodec, String sourceSchemaFilename,
			List<String> sourceDataFileList, Codec destinationCodec,
			String destinationSchemaFilename,
			List<String> destinationDataFileList) {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see dremel.server.impl.INeedsStreamImpl#setInputSchemaLocator(java.lang.String)
	 */
	@Override
	public void setInputSchemaLocator(String schemaLocator) {
		this.inputSchemaLocator = schemaLocator;
	}
	/* (non-Javadoc)
	 * @see dremel.server.impl.INeedsStreamImpl#setInputDataLocator(java.util.List)
	 */
	@Override
	public void setInputDataLocator(List<String> dataLocator) {
		this.inputDataLocator = dataLocator;
	}
	/* (non-Javadoc)
	 * @see dremel.server.impl.INeedsStreamImpl#setInputCodec(dremel.dataset.Stream.Codec)
	 */
	@Override
	public void setInputCodec(Codec codec) {
		this.inputCodec = codec;
	}

}
