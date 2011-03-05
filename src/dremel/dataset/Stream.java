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

import dremel.tableton.Schema;

/**
 * Stores and retrieves datasets. Implementation can be for example localdirectory or some other 
 * form of storage like HDFS or openstack.swift
 * 
 * @see dremel.dataset.impl.LocalDirctory
 *
 */
public interface Stream {
	enum Codec {AVRO_JSON, AVRO_BIN};
	ReaderTree openForRead(String dataLocator, String schemaLocator, Codec codec);
	void write(ReaderTree source, String dataLocator, Codec codec);
	void writeSchema(Schema schema, String schemaLocator, Codec codec);
	Schema readSchema(String schemaLocator, Codec codec);
}
