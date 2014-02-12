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

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static info.archinnov.achilles.type.OptionsBuilder.noOptions;
import info.archinnov.achilles.exception.AchillesStaleObjectStateException;
import info.archinnov.achilles.internal.context.ConfigurationContext;
import info.archinnov.achilles.internal.context.DaoContext;
import info.archinnov.achilles.internal.context.PersistenceContext;
import info.archinnov.achilles.internal.context.PersistenceContextFactory;
import info.archinnov.achilles.internal.metadata.holder.EntityMeta;
import info.archinnov.achilles.internal.persistence.operations.EntityProxifier;
import info.archinnov.achilles.internal.persistence.operations.EntityValidator;
import info.archinnov.achilles.internal.persistence.operations.SliceQueryExecutor;
import info.archinnov.achilles.internal.validation.Validator;
import info.archinnov.achilles.query.cql.NativeQueryBuilder;
import info.archinnov.achilles.query.slice.SliceQueryBuilder;
import info.archinnov.achilles.query.typed.TypedQueryBuilder;
import info.archinnov.achilles.query.typed.TypedQueryValidator;
import info.archinnov.achilles.type.ConsistencyLevel;
import info.archinnov.achilles.type.IndexCondition;
import info.archinnov.achilles.type.Options;
import info.archinnov.achilles.type.OptionsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

public class DefaultPersistenceManager implements PersistenceManager {
	private static final Logger log = LoggerFactory.getLogger(DefaultPersistenceManager.class);

	protected Map<Class<?>, EntityMeta> entityMetaMap;
	protected ConfigurationContext configContext;
	protected PersistenceContextFactory contextFactory;

	protected EntityProxifier proxifier = new EntityProxifier();
	private EntityValidator entityValidator = new EntityValidator();
	private TypedQueryValidator typedQueryValidator = new TypedQueryValidator();

	private SliceQueryExecutor sliceQueryExecutor;

	protected DaoContext daoContext;

	protected DefaultPersistenceManager(Map<Class<?>, EntityMeta> entityMetaMap, //
			PersistenceContextFactory contextFactory, DaoContext daoContext, ConfigurationContext configContext) {
		this.entityMetaMap = entityMetaMap;
		this.configContext = configContext;
		this.daoContext = daoContext;
		this.contextFactory = contextFactory;
		this.sliceQueryExecutor = new SliceQueryExecutor(contextFactory, configContext, daoContext);
	}

	@Override
   public <T> T persist(T entity) {
		log.debug("Persisting entity '{}'", entity);
		return persist(entity, noOptions());
	}

	@Override
   public <T> T persist(final T entity, Options options) {
		if (log.isDebugEnabled())
			log.debug("Persisting entity '{}' with options {} ", entity, options);

		entityValidator.validateEntity(entity, entityMetaMap);

		if (options.getTtl().isPresent()) {
			entityValidator.validateNotClusteredCounter(entity, entityMetaMap);
		}
		proxifier.ensureNotProxy(entity);
		PersistenceContext context = initPersistenceContext(entity, options);
		return context.persist(entity);
	}

	@Override
   public void update(Object entity) {
		if (log.isDebugEnabled())
			log.debug("Updating entity '{}'", proxifier.getRealObject(entity));
		update(entity, noOptions());
	}

	@Override
   public void update(Object entity, Options options) {
		proxifier.ensureProxy(entity);
		Object realObject = proxifier.getRealObject(entity);
		if (log.isDebugEnabled()) {
			log.debug("Updating entity '{}' with options {} ", realObject, options);
		}
		entityValidator.validateEntity(realObject, entityMetaMap);
		if (options.getTtl().isPresent()) {
			entityValidator.validateNotClusteredCounter(realObject, entityMetaMap);
		}
		PersistenceContext context = initPersistenceContext(realObject, options);
		context.update(entity);
	}

	@Override
   public void remove(Object entity) {
		if (log.isDebugEnabled())
			log.debug("Removing entity '{}'", proxifier.getRealObject(entity));
		remove(entity, noOptions());
	}

	@Override
   public void removeById(Class<?> entityClass, Object primaryKey) {
		Validator.validateNotNull(entityClass, "The entity class should not be null for removal by id");
		Validator.validateNotNull(primaryKey, "The primary key should not be null for removal by id");
		if (log.isDebugEnabled()) {
			log.debug("Removing entity of type '{}' by its id '{}'", entityClass, primaryKey);
		}
		PersistenceContext context = initPersistenceContext(entityClass, primaryKey, noOptions());
		entityValidator.validatePrimaryKey(context.getIdMeta(), primaryKey);
		context.remove();
	}

	@Override
   public void remove(final Object entity, Options options) {
		Object realObject = proxifier.getRealObject(entity);
		if (log.isDebugEnabled()) {
			log.debug("Removing entity '{}' with options {}", realObject, options);
		}

		entityValidator.validateEntity(realObject, entityMetaMap);
		PersistenceContext context = initPersistenceContext(realObject, options);
		context.remove();
	}

	@Override
   public void removeById(Class<?> entityClass, Object primaryKey, ConsistencyLevel writeLevel) {
		Validator.validateNotNull(entityClass, "The entity class should not be null for removal by id");
		Validator.validateNotNull(primaryKey, "The primary key should not be null for removal by id");
		if (log.isDebugEnabled())
			log.debug("Removing entity of type '{}' by its id '{}'", entityClass, primaryKey);

		PersistenceContext context = initPersistenceContext(entityClass, primaryKey,
				OptionsBuilder.withConsistency(writeLevel));
		entityValidator.validatePrimaryKey(context.getIdMeta(), primaryKey);
		context.remove();
	}

	@Override
   public <T> T find(Class<T> entityClass, Object primaryKey) {
		log.debug("Find entity class '{}' with primary key {}", entityClass, primaryKey);
		T entity = find(entityClass, primaryKey, null);
		return entity;
	}

	@Override
   public <T> T find(final Class<T> entityClass, final Object primaryKey, ConsistencyLevel readLevel) {
		log.debug("Find entity class '{}' with primary key {} and read consistency level {}", entityClass, primaryKey,
				readLevel);
		Validator.validateNotNull(entityClass, "Entity class should not be null for find by id");
		Validator.validateNotNull(primaryKey, "Entity primaryKey should not be null for find by id");
		Validator.validateTrue(entityMetaMap.containsKey(entityClass),
				"The entity class '%s' is not managed by Achilles", entityClass.getCanonicalName());
		Validator.validateTrue(entityMetaMap.containsKey(entityClass),
				"The entity class '%s' is not managed by Achilles", entityClass.getCanonicalName());
		PersistenceContext context = initPersistenceContext(entityClass, primaryKey,
				OptionsBuilder.withConsistency(readLevel));
		entityValidator.validatePrimaryKey(context.getIdMeta(), primaryKey);
		return context.find(entityClass);
	}

	@Override
   public <T> T getProxy(Class<T> entityClass, Object primaryKey) {
		if (log.isDebugEnabled())
			log.debug("Get reference for entity class '{}' with primary key {}", entityClass, primaryKey);

		return getProxy(entityClass, primaryKey, null);
	}

	@Override
   public <T> T getProxy(final Class<T> entityClass, final Object primaryKey, ConsistencyLevel readLevel) {
		if (log.isDebugEnabled())
			log.debug("Get reference for entity class '{}' with primary key {} and read consistency level {}",
					entityClass, primaryKey, readLevel);

		Validator.validateNotNull(entityClass, "Entity class should not be null for get reference");
		Validator.validateNotNull(primaryKey, "Entity primaryKey should not be null for get reference");
		Validator.validateTrue(entityMetaMap.containsKey(entityClass),
				"The entity class '%s' is not managed by Achilles", entityClass.getCanonicalName());

		Validator.validateTrue(entityMetaMap.containsKey(entityClass),
				"The entity class '%s' is not managed by Achilles", entityClass.getCanonicalName());

		PersistenceContext context = initPersistenceContext(entityClass, primaryKey,
				OptionsBuilder.withConsistency(readLevel));
		entityValidator.validatePrimaryKey(context.getIdMeta(), primaryKey);
		T entity = context.getProxy(entityClass);
		return entity;
	}

	@Override
   public void refresh(Object entity) throws AchillesStaleObjectStateException {
		if (log.isDebugEnabled())
			log.debug("Refreshing entity '{}'", proxifier.removeProxy(entity));
		refresh(entity, null);
	}

	@Override
   public void refresh(final Object entity, ConsistencyLevel readLevel) throws AchillesStaleObjectStateException {
		if (log.isDebugEnabled())
			log.debug("Refreshing entity '{}' with read consistency level {}", proxifier.removeProxy(entity), readLevel);

		proxifier.ensureProxy(entity);
		Object realObject = proxifier.getRealObject(entity);
		entityValidator.validateEntity(realObject, entityMetaMap);
		PersistenceContext context = initPersistenceContext(realObject, OptionsBuilder.withConsistency(readLevel));
		context.refresh(entity);
	}

	@Override
   public <T> T initialize(final T entity) {
		log.debug("Force lazy fields initialization for entity {}", entity);
		if (log.isDebugEnabled()) {
			log.debug("Force lazy fields initialization for entity {}", proxifier.removeProxy(entity));
		}
		proxifier.ensureProxy(entity);
		T realObject = proxifier.getRealObject(entity);
		PersistenceContext context = initPersistenceContext(realObject, noOptions());
		return context.initialize(entity);
	}

	@Override
   public <T> Set<T> initialize(final Set<T> entities) {
		log.debug("Force lazy fields initialization for entity set {}", entities);
		for (T entity : entities) {
			initialize(entity);
		}
		return entities;
	}

	@Override
   public <T> List<T> initialize(final List<T> entities) {
		log.debug("Force lazy fields initialization for entity set {}", entities);
		for (T entity : entities) {
			initialize(entity);
		}
		return entities;
	}

	@Override
   public <T> T initAndRemoveProxy(T entity) {
		return removeProxy(initialize(entity));
	}

	@Override
   public <T> Set<T> initAndRemoveProxy(Set<T> entities) {
		return removeProxy(initialize(entities));
	}

	@Override
   public <T> List<T> initAndRemoveProxy(List<T> entities) {
		return removeProxy(initialize(entities));
	}

	@Override
   public <T> T removeProxy(T proxy) {
		log.debug("Removing proxy for entity {}", proxy);

		T realObject = proxifier.removeProxy(proxy);

		return realObject;
	}

	@Override
   public <T> List<T> removeProxy(List<T> proxies) {
		log.debug("Removing proxy for a list of entities {}", proxies);

		return proxifier.removeProxy(proxies);
	}

	@Override
   public <T> Set<T> removeProxy(Set<T> proxies) {
		log.debug("Removing proxy for a set of entities {}", proxies);

		return proxifier.removeProxy(proxies);
	}

	@Override
   public <T> SliceQueryBuilder<T> sliceQuery(Class<T> entityClass) {
		log.debug("Execute slice query for entity class {}", entityClass);
		EntityMeta meta = entityMetaMap.get(entityClass);
		Validator.validateTrue(meta.isClusteredEntity(),
				"Cannot perform slice query on entity type '%s' because it is " + "not a clustered entity",
				meta.getClassName());
		return new SliceQueryBuilder<>(sliceQueryExecutor, entityClass, meta);
	}

	@Override
   public NativeQueryBuilder nativeQuery(String queryString, Object... boundValues) {
		log.debug("Execute native query {}", queryString);
		Validator.validateNotBlank(queryString, "The query string for native query should not be blank");
		return new NativeQueryBuilder(daoContext, queryString, boundValues);
	}

	@Override
   public <T> TypedQueryBuilder<T> typedQuery(Class<T> entityClass, String queryString, Object... boundValues) {
		return typedQueryInternal(entityClass, queryString, true, boundValues);
	}

	private <T> TypedQueryBuilder<T> typedQueryInternal(Class<T> entityClass, String queryString,
			boolean normalizeQuery, Object... boundValues) {
		log.debug("Execute typed query for entity class {}", entityClass);
		Validator.validateNotNull(entityClass, "The entityClass for typed query should not be null");
		Validator.validateNotBlank(queryString, "The query string for typed query should not be blank");
		Validator.validateTrue(entityMetaMap.containsKey(entityClass),
				"Cannot perform typed query because the entityClass '%s' is not managed by Achilles",
				entityClass.getCanonicalName());

		EntityMeta meta = entityMetaMap.get(entityClass);
		typedQueryValidator.validateTypedQuery(entityClass, queryString, meta);
		return new TypedQueryBuilder<>(entityClass, daoContext, queryString, meta, contextFactory, true,
				normalizeQuery, boundValues);
	}

	@Override
   public <T> TypedQueryBuilder<T> indexedQuery(Class<T> entityClass, IndexCondition indexCondition) {
		log.debug("Execute indexed query for entity class {}", entityClass);

		EntityMeta entityMeta = entityMetaMap.get(entityClass);

		Validator.validateFalse(entityMeta.isClusteredEntity(),
				"Index query is not supported for clustered entity. Please use typed query/native query");
		Validator.validateNotNull(indexCondition, "Index condition should not be null");
		Validator.validateNotBlank(indexCondition.getColumnName(),
				"Column name for index condition '%s' should be provided", indexCondition);
		Validator.validateNotNull(indexCondition.getColumnValue(),
				"Column value for index condition '%s' should be provided", indexCondition);
		Validator.validateNotNull(indexCondition.getIndexRelation(),
				"Index relation for index condition '%s' should be provided", indexCondition);

		String indexColumnName = indexCondition.getColumnName();
		final Select.Where query = QueryBuilder.select().from(entityMeta.getTableName())
				.where(QueryBuilder.eq(indexColumnName, bindMarker(indexColumnName)));
		return typedQueryInternal(entityClass, query.getQueryString(), false, indexCondition.getColumnValue());
	}

	@Override
   public <T> TypedQueryBuilder<T> rawTypedQuery(Class<T> entityClass, String queryString, Object... boundValues) {
		log.debug("Execute raw typed query for entity class {}", entityClass);
		Validator.validateNotNull(entityClass, "The entityClass for typed query should not be null");
		Validator.validateNotBlank(queryString, "The query string for typed query should not be blank");
		Validator.validateTrue(entityMetaMap.containsKey(entityClass),
				"Cannot perform typed query because the entityClass '%s' is not managed by Achilles",
				entityClass.getCanonicalName());

		EntityMeta meta = entityMetaMap.get(entityClass);
		typedQueryValidator.validateRawTypedQuery(entityClass, queryString, meta);
		return new TypedQueryBuilder<>(entityClass, daoContext, queryString, meta, contextFactory, false, true,
				boundValues);
	}

	protected PersistenceContext initPersistenceContext(Class<?> entityClass, Object primaryKey, Options options) {
		return contextFactory.newContext(entityClass, primaryKey, options);
	}

	protected PersistenceContext initPersistenceContext(Object entity, Options options) {
		return contextFactory.newContext(entity, options);
	}

	@Override
   public Session getNativeSession() {
		return daoContext.getSession();
	}

	protected Map<Class<?>, EntityMeta> getEntityMetaMap() {
		return entityMetaMap;
	}

	protected ConfigurationContext getConfigContext() {
		return configContext;
	}

	protected void setEntityMetaMap(Map<Class<?>, EntityMeta> entityMetaMap) {
		this.entityMetaMap = entityMetaMap;
	}

	protected void setConfigContext(ConfigurationContext configContext) {
		this.configContext = configContext;
	}
}