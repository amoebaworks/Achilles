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
import info.archinnov.achilles.internal.validation.Validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompoundTranscoder extends AbstractTranscoder {

    private static final Logger log  = LoggerFactory.getLogger(CompoundTranscoder.class);

    public CompoundTranscoder(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public List<Object> encodeToComponents(PropertyMeta idMeta, Object compoundKey) {
        log.trace("Encode {} to CQL components",compoundKey);
		List<Object> compoundComponents = new ArrayList<>();
		List<Field> componentFields = idMeta.getComponentFields();
		List<Class<?>> componentClasses = idMeta.getComponentClasses();
		if (compoundKey != null) {
			for (int i = 0; i < componentFields.size(); i++) {
				Object component = invoker.getValueFromField(compoundKey, componentFields.get(i));
				Object encoded = super.encodeInternal(componentClasses.get(i), component);
				compoundComponents.add(encoded);
			}
		}
		return compoundComponents;
	}

	@Override
	public List<Object> encodeToComponents(PropertyMeta pm, List<?> components) {
        log.trace("Encode {} to CQL components",components);
		List<Object> encodedComponents = new ArrayList<>();
		List<Class<?>> componentClasses = pm.getComponentClasses();
		for (Object component : components) {
			if (component != null) {
				Class<?> componentClass = component.getClass();
				Validator.validateTrue(componentClasses.contains(componentClass),
						"The component {} for embedded id {} has an unknown type. Valid types are {}", component, pm
								.getValueClass().getCanonicalName(), componentClasses);
				Object encoded = super.encodeInternal(componentClass, component);
				encodedComponents.add(encoded);
			}
		}
		return encodedComponents;
	}

	@Override
	public Object decodeFromComponents(PropertyMeta idMeta, List<?> components) {
        log.trace("Decode from CQL components",components);
		List<Field> componentFields = idMeta.getComponentFields();

		List<Object> decodedComponents = new ArrayList<>();
		List<Class<?>> componentClasses = idMeta.getComponentClasses();
		for (int i = 0; i < components.size(); i++) {
			Object decoded = super.decodeInternal(componentClasses.get(i), components.get(i));
			decodedComponents.add(decoded);
		}

		Object compoundKey;
		compoundKey = injectValues(idMeta, decodedComponents, componentFields);
		return compoundKey;
	}

	private Object injectValues(PropertyMeta pm, List<?> components, List<Field> componentFields) {
        log.trace("Instantiate primary compound key from CQL components {}",components);
		Object compoundKey = pm.instantiate();

		for (int i = 0; i < components.size(); i++) {
			Object compValue = components.get(i);
			invoker.setValueToField(compoundKey, componentFields.get(i), compValue);
		}
		return compoundKey;
	}
}
