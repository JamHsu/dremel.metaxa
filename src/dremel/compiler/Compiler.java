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
package dremel.compiler;

import java.util.List;

import dremel.dataset.ReaderTree;
import dremel.dataset.Schema;
import dremel.parser.AstNode;


/**
 * Invokes compiler to compile parsed query to executable form.
 * <P>
 * @author camuelg
 */
public interface Compiler {


	/**
	 * Represents compiled query.
	 * <P>
	 * Recursively links queries with sub queries and return single reader tree.
	 */
	public interface Query {
		/**
		 * Recursively links queries with sub queries and return single reader tree.
		 * <P>
		 * @see dremel.dataset.ReaderTree
		 * <P>
		 * @param source is a reference to source data iterator
		 * @return iterator which when pulled executes the query, expression by expression
		 */
		ReaderTree link(List<ReaderTree> source);		
	}
	
	/**
	 * @param root parsed query
	 * @param schema schema the query is compiled against
	 * @return compiled query
	 */
	Query compile(AstNode root, Schema schema);
}
 

