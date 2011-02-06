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
package dremel.planner;

import java.util.List;

import dremel.compiler.Compiler.Query;
import dremel.dataset.ReaderTree;
import dremel.dataset.Stream;

/**
 * Instantiates source streams and links them with cloned Query objects in a tree manner with single
 * root. 
 * <P> 
 * In future, the planner interface will include methods to plan parallel query, where same compiled 
 * query object is cloned and linked to many streams in tree-like fashion.
 *  
 */
public interface Planner {
	/**
	 * @param query to clone to link streams  
	 * <P>
	 * @param streams to query
	 * <P>
	 * @return Iterator over result dataset
	 */
	ReaderTree plan(Query query, List<Stream> streams);
}
