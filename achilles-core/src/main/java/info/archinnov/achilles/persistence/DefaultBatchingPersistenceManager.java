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
package info.archinnov.achilles.persistence;

import info.archinnov.achilles.exception.AchillesException;
import info.archinnov.achilles.exception.AchillesStaleObjectStateException;
import info.archinnov.achilles.internal.context.BatchingFlushContext;
import info.archinnov.achilles.internal.context.ConfigurationContext;
import info.archinnov.achilles.internal.context.DaoContext;
import info.archinnov.achilles.internal.context.PersistenceContext;
import info.archinnov.achilles.internal.context.PersistenceContextFactory;
import info.archinnov.achilles.internal.metadata.holder.EntityMeta;
import info.archinnov.achilles.internal.utils.UUIDGen;
import info.archinnov.achilles.type.ConsistencyLevel;
import info.archinnov.achilles.type.Options;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBatchingPersistenceManager extends DefaultPersistenceManager implements BatchingPersistenceManager {

	private static final Logger log = LoggerFactory.getLogger(DefaultBatchingPersistenceManager.class);

	protected BatchingFlushContext flushContext;
	private ConsistencyLevel defaultConsistencyLevel;

    DefaultBatchingPersistenceManager(Map<Class<?>, EntityMeta> entityMetaMap, PersistenceContextFactory contextFactory,
			DaoContext daoContext, ConfigurationContext configContext) {
		super(entityMetaMap, contextFactory, daoContext, configContext);
		defaultConsistencyLevel = configContext.getDefaultWriteConsistencyLevel();
		this.flushContext = new BatchingFlushContext(daoContext, defaultConsistencyLevel);
	}

	@Override
   public void startBatch() {
		log.debug("Starting batch mode");
		flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
	}

	@Override
   public void startBatch(ConsistencyLevel consistencyLevel) {
		log.debug("Starting batch mode with consistency level {}", consistencyLevel.name());
        flushContext = flushContext.duplicateWithNoData(consistencyLevel);
	}

	@Override
   public void endBatch() {
		log.debug("Ending batch mode");
		try {
			flushContext.endBatch();
		} finally {
			flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
		}
	}

	@Override
   public void cleanBatch() {
		log.debug("Cleaning all pending statements");
        flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
	}

   @Override
	public <T> T persist(final T entity, Options options) {
		if (options.getConsistencyLevel().isPresent()) {
            flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
			throw new AchillesException(
					"Runtime custom Consistency Level cannot be set for batch mode. Please set the Consistency Levels at batch start with 'startBatch(consistencyLevel)'");
		} else {
            return super.persist(entity, options.duplicateWithNewTimestamp(UUIDGen.increasingMicroTimestamp()));
		}
	}

   @Override
	public void update(Object entity, Options options) {
		if (options.getConsistencyLevel().isPresent()) {
            flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
			throw new AchillesException(
					"Runtime custom Consistency Level cannot be set for batch mode. Please set the Consistency Levels at batch start with 'startBatch(consistencyLevel)'");
		} else {
            super.update(entity, options.duplicateWithNewTimestamp(UUIDGen.increasingMicroTimestamp()));
		}
	}

   @Override
	public void remove(final Object entity, Options options) {
		if (options.getConsistencyLevel().isPresent()) {
            flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
			throw new AchillesException(
					"Runtime custom Consistency Level cannot be set for batch mode. Please set the Consistency Levels at batch start with 'startBatch(consistencyLevel)'");
		} else {
			super.remove(entity, options.duplicateWithNewTimestamp(UUIDGen.increasingMicroTimestamp()));
		}
	}

   @Override
	public <T> T find(final Class<T> entityClass, final Object primaryKey, ConsistencyLevel readLevel) {
		if (readLevel != null) {
            flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
			throw new AchillesException(
					"Runtime custom Consistency Level cannot be set for batch mode. Please set the Consistency Levels at batch start with 'startBatch(consistencyLevel)'");
		} else {
			return super.find(entityClass, primaryKey, null);
		}
	}

   @Override
	public <T> T getProxy(final Class<T> entityClass, final Object primaryKey, ConsistencyLevel readLevel) {
		if (readLevel != null) {
			flushContext = flushContext.duplicateWithNoData(defaultConsistencyLevel);
			throw new AchillesException(
					"Runtime custom Consistency Level cannot be set for batch mode. Please set the Consistency Levels at batch start with 'startBatch(consistencyLevel)'");
		} else {
			return super.getProxy(entityClass, primaryKey, null);
		}
	}

   @Override
	public void refresh(final Object entity, ConsistencyLevel readLevel) throws AchillesStaleObjectStateException {
		if (readLevel != null) {
			throw new AchillesException(
					"Runtime custom Consistency Level cannot be set for batch mode. Please set the Consistency Levels at batch start with 'startBatch(consistencyLevel)'");
		} else {
			super.refresh(entity, null);
		}
	}

	@Override
	protected PersistenceContext initPersistenceContext(Class<?> entityClass, Object primaryKey, Options options) {
		log.trace("Initializing new persistence context for entity class {} and primary key {}",
				entityClass.getCanonicalName(), primaryKey);
		return contextFactory.newContextWithFlushContext(entityClass, primaryKey, options, flushContext);
	}

	@Override
	protected PersistenceContext initPersistenceContext(Object entity, Options options) {
		log.trace("Initializing new persistence context for entity {}", entity);
		return contextFactory.newContextWithFlushContext(entity, options, flushContext);
	}
}
