/*
 * Copyright (C) 2012-2014 DuyHai DOAN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package info.archinnov.achilles.internal.metadata.transcoding;

import info.archinnov.achilles.internal.metadata.holder.PropertyMeta;
import info.archinnov.achilles.internal.reflection.ReflectionInvoker;

import org.codehaus.jackson.map.ObjectMapper;

public class SimpleTranscoder extends AbstractTranscoder {

	protected ReflectionInvoker invoker = new ReflectionInvoker();

	public SimpleTranscoder(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public Object encode(PropertyMeta pm, Object entityValue) {
		return super.encodeInternal(pm.getValueClass(), entityValue);
	}

	@Override
	public Object decode(PropertyMeta pm, Object cassandraValue) {
		return super.decodeInternal(pm.getValueClass(), cassandraValue);
	}

}
