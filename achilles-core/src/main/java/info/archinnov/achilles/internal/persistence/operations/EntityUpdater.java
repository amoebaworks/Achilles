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
package info.archinnov.achilles.internal.persistence.operations;

import static com.google.common.collect.FluentIterable.from;
import static info.archinnov.achilles.internal.metadata.holder.PropertyType.excludeCounterType;
import info.archinnov.achilles.internal.context.PersistenceContext;
import info.archinnov.achilles.internal.metadata.holder.EntityMeta;
import info.archinnov.achilles.internal.metadata.holder.PropertyMeta;
import info.archinnov.achilles.internal.proxy.EntityInterceptor;
import info.archinnov.achilles.internal.validation.Validator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityUpdater {

	private static final Logger log = LoggerFactory.getLogger(EntityUpdater.class);

	private PropertyMetaComparator comparator = new PropertyMetaComparator();

	private CounterPersister counterPersister = new CounterPersister();
	private EntityProxifier proxifier = new EntityProxifier();

	public void update(PersistenceContext context, Object entity) {
		log.debug("Merging entity of class {} with primary key {}", context.getEntityClass().getCanonicalName(),
				context.getPrimaryKey());

		EntityMeta entityMeta = context.getEntityMeta();

		Validator.validateNotNull(entity, "Proxy object should not be null for update");
		Validator.validateNotNull(entityMeta, "entityMeta should not be null for update");

		log.debug("Checking for dirty fields before merging");

		Object realObject = proxifier.getRealObject(entity);
		context.setEntity(realObject);

		EntityInterceptor<Object> interceptor = proxifier.getInterceptor(entity);
		Map<Method, PropertyMeta> dirtyMap = interceptor.getDirtyMap();
		List<PropertyMeta> sortedDirtyNonCounterMetas = new ArrayList<>(from(dirtyMap.values()).filter(
				excludeCounterType).toList());
		if (sortedDirtyNonCounterMetas.size() > 0) {
			Collections.sort(sortedDirtyNonCounterMetas, comparator);
			context.pushUpdateStatement(sortedDirtyNonCounterMetas);
			dirtyMap.clear();
		}

		if (context.isClusteredCounter()) {
			counterPersister.persistClusteredCounters(context);
		} else {
			counterPersister.persistCounters(context, entityMeta.getAllCounterMetas());
		}
		interceptor.setContext(context);
		interceptor.setTarget(realObject);
	}

	public static class PropertyMetaComparator implements Comparator<PropertyMeta> {
		@Override
		public int compare(PropertyMeta arg0, PropertyMeta arg1) {
			return arg0.getPropertyName().compareTo(arg1.getPropertyName());
		}

	}
}
