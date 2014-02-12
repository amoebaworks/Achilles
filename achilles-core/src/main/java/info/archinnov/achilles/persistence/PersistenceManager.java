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

import info.archinnov.achilles.exception.AchillesStaleObjectStateException;
import info.archinnov.achilles.query.cql.NativeQueryBuilder;
import info.archinnov.achilles.query.slice.SliceQueryBuilder;
import info.archinnov.achilles.query.typed.TypedQueryBuilder;
import info.archinnov.achilles.type.ConsistencyLevel;
import info.archinnov.achilles.type.IndexCondition;
import info.archinnov.achilles.type.Options;

import java.util.List;
import java.util.Set;

import com.datastax.driver.core.Session;

public interface PersistenceManager {

   /**
    * Persist an entity.
    * 
    * @param entity
    *            Entity to be persisted
    * @return proxified entity
    */
   public <T> T persist(T entity);

   /**
    * Persist an entity with the given options.
    * 
    * @param entity
    *            Entity to be persisted
    * @param options
    *            options for consistency level, ttl and timestamp
    * @return proxified entity
    */
   public <T> T persist(T entity, Options options);

   /**
    * Update a "managed" entity
    * 
    * @param entity
    *            Managed entity to be updated
    */
   public void update(Object entity);

   /**
    * Update a "managed" entity
    * 
    * @param entity
    *            Managed entity to be updated
    * @param options
    *            options for consistency level, ttl and timestamp
    */
   public void update(Object entity, Options options);

   /**
    * Remove an entity.
    * 
    * @param entity
    *            Entity to be removed
    */
   public void remove(Object entity);

   /**
    * Remove an entity by its id.
    * 
    * @param entityClass
    *            Entity class
    * 
    * @param primaryKey
    *            Primary key
    */
   public void removeById(Class<?> entityClass, Object primaryKey);

   /**
    * Remove an entity with the given Consistency Level for write.
    * 
    * @param entity
    *            Entity to be removed
    * @param options
    *            options for consistency level and timestamp
    */
   public void remove(Object entity, Options options);

   /**
    * Remove an entity by its id with the given Consistency Level for write.
    * 
    * @param entityClass
    *            Entity class
    * 
    * @param primaryKey
    *            Primary key
    */
   public void removeById(Class<?> entityClass, Object primaryKey, ConsistencyLevel writeLevel);

   /**
    * Find an entity.
    * 
    * @param entityClass
    *            Entity type
    * @param primaryKey
    *            Primary key (Cassandra row key) of the entity to load
    */
   public <T> T find(Class<T> entityClass, Object primaryKey);

   /**
    * Find an entity with the given Consistency Level for read
    * 
    * @param entityClass
    *            Entity type
    * @param primaryKey
    *            Primary key (Cassandra row key) of the entity to load
    * @param readLevel
    *            Consistency Level for read
    */
   public <T> T find(Class<T> entityClass, Object primaryKey, ConsistencyLevel readLevel);

   /**
    * Create a proxy for the entity. An new empty entity will be created,
    * populated with the provided primary key and then proxified. This method
    * never returns null Use this method to perform direct update without
    * read-before-write
    * 
    * @param entityClass
    *            Entity type
    * @param primaryKey
    *            Primary key (Cassandra row key) of the entity to initialize
    */
   public <T> T getProxy(Class<T> entityClass, Object primaryKey);

   /**
    * Create a proxy for the entity. An new empty entity will be created,
    * populated with the provided primary key and then proxified. This method
    * never returns null Use this method to perform direct update without
    * read-before-write
    * 
    * @param entityClass
    *            Entity type
    * @param primaryKey
    *            Primary key (Cassandra row key) of the entity to initialize
    * @param readLevel
    *            Consistency Level for read
    */
   public <T> T getProxy(Class<T> entityClass, Object primaryKey, ConsistencyLevel readLevel);

   /**
    * Refresh an entity.
    * 
    * @param entity
    *            Entity to be refreshed
    */
   public void refresh(Object entity) throws AchillesStaleObjectStateException;

   /**
    * Refresh an entity with the given Consistency Level for read.
    * 
    * @param entity
    *            Entity to be refreshed
    * @param readLevel
    *            Consistency Level for read
    */
   public void refresh(Object entity, ConsistencyLevel readLevel) throws AchillesStaleObjectStateException;

   /**
    * Initialize all lazy fields of a 'managed' entity, except WideMap/Counter
    * fields.
    * 
    * Raise an <strong>IllegalStateException</strong> if the entity is not
    * 'managed'
    * 
    */
   public <T> T initialize(T entity);

   /**
    * Initialize all lazy fields of a set of 'managed' entities, except
    * WideMap/Counter fields.
    * 
    * Raise an IllegalStateException if an entity is not 'managed'
    * 
    */
   public <T> Set<T> initialize(Set<T> entities);

   /**
    * Initialize all lazy fields of a list of 'managed' entities, except
    * WideMap/Counter fields.
    * 
    * Raise an IllegalStateException if an entity is not 'managed'
    * 
    */
   public <T> List<T> initialize(List<T> entities);

   /**
    * Shorthand for manager.removeProxy(manager.initialize(T entity))
    * 
    */
   public <T> T initAndRemoveProxy(T entity);

   /**
    * Shorthand for manager.removeProxy(manager.initialize(Set<T> entities))
    * 
    */
   public <T> Set<T> initAndRemoveProxy(Set<T> entities);

   /**
    * Shorthand for manager.removeProxy(manager.initialize(List<T> entities))
    * 
    */
   public <T> List<T> initAndRemoveProxy(List<T> entities);

   /**
    * Remove the proxy of a 'managed' entity and return the underlying "raw"
    * entity
    * 
    * If the argument is not a proxy objet, return itself <br/>
    * Else, return the target object behind the proxy
    * 
    * @param proxy
    * @return real object
    */
   public <T> T removeProxy(T proxy);

   /**
    * Remove the proxy of a list of 'managed' entities and return the
    * underlying "raw" entities
    * 
    * See {@link #removeProxy}
    * 
    * @param proxies
    *            list of proxified entity
    * @return real object list
    */
   public <T> List<T> removeProxy(List<T> proxies);

   /**
    * Remove the proxy of a set of 'managed' entities return the underlying
    * "raw" entities
    * 
    * See {@link #removeProxy}
    * 
    * @param proxies
    *            set of proxified entities
    * @return real object set
    */
   public <T> Set<T> removeProxy(Set<T> proxies);

   public <T> SliceQueryBuilder<T> sliceQuery(Class<T> entityClass);

   /**
    * Return a CQL native query builder
    * 
    * @param queryString
    *            native CQL query string, including limit, ttl and consistency
    *            options
    * 
    * @param boundValues
    *            values to be bind to the parameterized query, if any
    * 
    * @return NativeQueryBuilder
    */
   public NativeQueryBuilder nativeQuery(String queryString, Object... boundValues);

   /**
    * Return a CQL typed query builder
    * 
    * All found entities will be in 'managed' state
    * 
    * @param entityClass
    *            type of entity to be returned
    * 
    * @param queryString
    *            native CQL query string, including limit, ttl and consistency
    *            options
    * 
    * @param boundValues
    *            values to be bind to the parameterized query, if any
    * 
    * @return TypedQueryBuilder<T>
    */
   public <T> TypedQueryBuilder<T> typedQuery(Class<T> entityClass, String queryString, Object... boundValues);

   /**
    * Return a CQL typed query builder
    * 
    * All found entities will be in 'managed' state
    * 
    * @param entityClass
    *            type of entity to be returned
    * 
    * @param indexCondition
    *            index condition
    * 
    * @return TypedQueryBuilder<T>
    */
   public <T> TypedQueryBuilder<T> indexedQuery(Class<T> entityClass, IndexCondition indexCondition);

   /**
    * Return a CQL typed query builder
    * 
    * All found entities will be returned as raw entities and not 'managed' by
    * Achilles
    * 
    * @param entityClass
    *            type of entity to be returned
    * 
    * @param queryString
    *            native CQL query string, including limit, ttl and consistency
    *            options
    * 
    * @param boundValues
    *            values to be bind to the parameterized query, if any
    * 
    * @return TypedQueryBuilder<T>
    */
   public <T> TypedQueryBuilder<T> rawTypedQuery(Class<T> entityClass, String queryString, Object... boundValues);

   public Session getNativeSession();

}
