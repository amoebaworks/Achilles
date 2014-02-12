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

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;

public class SetTranscoder extends SimpleTranscoder {

	public SetTranscoder(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public Set<Object> encode(PropertyMeta pm, Set<?> entityValue) {
		Set<Object> encoded = new HashSet<Object>();
		for (Object value : entityValue) {
			encoded.add(super.encodeInternal(pm.getValueClass(), value));
		}
		return encoded;
	}

	@Override
	public Set<Object> decode(PropertyMeta pm, Set<?> cassandraValue) {
		Set<Object> decoded = new HashSet<Object>();
		for (Object value : cassandraValue) {
			decoded.add(super.decodeInternal(pm.getValueClass(), value));
		}
		return decoded;
	}

}
