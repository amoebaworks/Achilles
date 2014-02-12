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
package info.archinnov.achilles.internal.statement.prepared;

import static info.archinnov.achilles.counter.AchillesCounter.CQL_COUNTER_FQCN;
import static info.archinnov.achilles.counter.AchillesCounter.CQL_COUNTER_PRIMARY_KEY;
import static info.archinnov.achilles.counter.AchillesCounter.CQL_COUNTER_PROPERTY_NAME;
import static info.archinnov.achilles.counter.AchillesCounter.CQL_COUNTER_TABLE;
import static info.archinnov.achilles.counter.AchillesCounter.CQL_COUNTER_VALUE;
import static info.archinnov.achilles.counter.AchillesCounter.CQLQueryType.DECR;
import static info.archinnov.achilles.counter.AchillesCounter.CQLQueryType.DELETE;
import static info.archinnov.achilles.counter.AchillesCounter.CQLQueryType.INCR;
import static info.archinnov.achilles.counter.AchillesCounter.CQLQueryType.SELECT;
import static info.archinnov.achilles.counter.AchillesCounter.ClusteredCounterStatement.DELETE_ALL;
import static info.archinnov.achilles.internal.metadata.holder.PropertyType.COUNTER;
import static info.archinnov.achilles.internal.metadata.holder.PropertyType.ID;
import static info.archinnov.achilles.test.builders.PropertyMetaTestBuilder.completeBean;
import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import info.archinnov.achilles.counter.AchillesCounter.CQLQueryType;
import info.archinnov.achilles.internal.metadata.holder.EntityMeta;
import info.archinnov.achilles.internal.metadata.holder.PropertyMeta;
import info.archinnov.achilles.internal.metadata.holder.PropertyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class PreparedStatementGeneratorTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	private PreparedStatementGenerator generator = new PreparedStatementGenerator();

	@Mock
	private Session session;

	@Mock
	private PreparedStatement ps;

	@Mock
	private PreparedStatement ps2;

	@Captor
	ArgumentCaptor<String> queryCaptor;

	@Captor
	ArgumentCaptor<RegularStatement> regularStatementCaptor;

	@Test
	public void should_prepare_insert_ps() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(PropertyType.SIMPLE).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setIdMeta(idMeta);
		meta.setTableName("table");
		meta.setAllMetasExceptIdAndCounters(asList(nameMeta));
		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareInsertPS(session, meta);

		assertThat(actual).isSameAs(ps);
		assertThat(queryCaptor.getValue()).isEqualTo("INSERT INTO table(id,name) VALUES (:id,:name) USING TTL :ttl;");
	}

	@Test
	public void should_prepare_insert_ps_with_clustered_id() throws Exception {
		List<PropertyMeta> allMetas = new ArrayList<PropertyMeta>();

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").compNames("id", "a", "b")
				.type(PropertyType.EMBEDDED_ID).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		allMetas.add(nameMeta);
		EntityMeta meta = new EntityMeta();
		meta.setIdMeta(idMeta);
		meta.setTableName("table");
		meta.setAllMetasExceptIdAndCounters(asList(nameMeta));
		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareInsertPS(session, meta);

		assertThat(actual).isSameAs(ps);
		assertThat(queryCaptor.getValue()).isEqualTo(
				"INSERT INTO table(id,a,b,name) VALUES (:id,:a,:b,:name) USING TTL :ttl;");
	}

	@Test
	public void should_prepare_select_field_ps() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(PropertyType.SIMPLE).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);

		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareSelectFieldPS(session, meta, nameMeta);

		assertThat(actual).isSameAs(ps);

		assertThat(queryCaptor.getValue()).isEqualTo("SELECT name FROM table WHERE id=:id;");
	}

	@Test
	public void should_prepare_select_field_ps_for_clustered_id() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(PropertyType.EMBEDDED_ID)
				.compNames("id", "a", "b").build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);

		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareSelectFieldPS(session, meta, idMeta);

		assertThat(actual).isSameAs(ps);

		assertThat(queryCaptor.getValue()).isEqualTo("SELECT id,a,b FROM table WHERE id=:id AND a=:a AND b=:b;");
	}

	@Test
	public void should_prepare_update_fields_ps() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(PropertyType.SIMPLE).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		PropertyMeta ageMeta = completeBean(Void.class, String.class).field("age").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);

		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareUpdateFields(session, meta, asList(nameMeta, ageMeta));

		assertThat(actual).isSameAs(ps);

		assertThat(queryCaptor.getValue()).isEqualTo(
				"UPDATE table USING TTL :ttl SET name=:name,age=:age WHERE id=:id;");
	}

	@Test
	public void should_prepare_update_fields_with_clustered_id_ps() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").compNames("id", "a", "b")
				.type(PropertyType.EMBEDDED_ID).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		PropertyMeta ageMeta = completeBean(Void.class, String.class).field("age").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);

		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareUpdateFields(session, meta, asList(nameMeta, ageMeta));

		assertThat(actual).isSameAs(ps);

		assertThat(queryCaptor.getValue()).isEqualTo(
				"UPDATE table USING TTL :ttl SET name=:name,age=:age WHERE id=:id AND a=:a AND b=:b;");
	}

	@Test
	public void should_exception_when_preparing_select_for_counter_type() throws Exception {

		PropertyMeta nameMeta = completeBean(Void.class, Long.class).field("count").type(PropertyType.COUNTER).build();

		EntityMeta meta = new EntityMeta();
		meta.setClassName("entity");

		exception.expect(IllegalArgumentException.class);
		exception
				.expectMessage("Cannot prepare statement for property 'count' of entity 'entity' because it is a counter type");

		generator.prepareSelectFieldPS(session, meta, nameMeta);

	}

	@Test
	public void should_prepare_select_eager_ps_with_single_key() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(PropertyType.SIMPLE).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);
		meta.setAllMetasExceptIdAndCounters(asList(nameMeta));
		meta.setAllMetasExceptId(asList(nameMeta));
		meta.setAllMetasExceptCounters(asList(nameMeta));

		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareSelectPS(session, meta);

		assertThat(actual).isSameAs(ps);
		assertThat(queryCaptor.getValue()).isEqualTo("SELECT name FROM table WHERE id=:id;");
	}

	@Test
	public void should_prepare_select_eager_ps_with_clustered_key() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").compNames("id", "a", "b")
				.type(PropertyType.EMBEDDED_ID).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);
		meta.setAllMetasExceptCounters(asList(idMeta, nameMeta));
		meta.setClusteredCounter(false);

		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		PreparedStatement actual = generator.prepareSelectPS(session, meta);

		assertThat(actual).isSameAs(ps);
		assertThat(queryCaptor.getValue()).isEqualTo("SELECT id,a,b,name FROM table WHERE id=:id AND a=:a AND b=:b;");
	}

	@Test
	public void should_remove_entity_having_single_key() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(PropertyType.SIMPLE).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);
		meta.setPropertyMetas(ImmutableMap.of("id", idMeta, "name", nameMeta));

		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		Map<String, PreparedStatement> actual = generator.prepareRemovePSs(session, meta);

		assertThat(actual).hasSize(1);
		assertThat(actual).containsValue(ps);
		assertThat(queryCaptor.getValue()).isEqualTo("DELETE  FROM table WHERE id=:id;");
	}

	@Test
	public void should_remove_entity_having_clustered_key() throws Exception {

		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").compNames("id", "a", "b")
				.type(PropertyType.EMBEDDED_ID).build();

		PropertyMeta nameMeta = completeBean(Void.class, String.class).field("name").type(PropertyType.SIMPLE).build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);
		meta.setPropertyMetas(ImmutableMap.of("name", nameMeta));
		when(session.prepare(queryCaptor.capture())).thenReturn(ps);

		Map<String, PreparedStatement> actual = generator.prepareRemovePSs(session, meta);

		assertThat(actual).hasSize(1);
		assertThat(actual).containsValue(ps);
		assertThat(queryCaptor.getValue()).isEqualTo("DELETE  FROM table WHERE id=:id AND a=:a AND b=:b;");
	}

	@Test
	public void should_remove_entity_having_counter() throws Exception {
		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(PropertyType.SIMPLE).build();

		PropertyMeta nameMeta = completeBean(UUID.class, String.class).field("count").type(PropertyType.COUNTER)
				.build();

		EntityMeta meta = new EntityMeta();
		meta.setTableName("table");
		meta.setIdMeta(idMeta);
		meta.setPropertyMetas(ImmutableMap.of("name", nameMeta));

		when(session.prepare(queryCaptor.capture())).thenReturn(ps, ps2);

		Map<String, PreparedStatement> actual = generator.prepareRemovePSs(session, meta);

		assertThat(actual).hasSize(1);
		assertThat(actual).containsKey("table");
		assertThat(actual).containsValue(ps);
		assertThat(queryCaptor.getAllValues()).containsOnly("DELETE  FROM table WHERE id=:id;");
	}

	@Test
	public void should_prepare_simple_counter_queries() throws Exception {
		PreparedStatement incrPs = mock(PreparedStatement.class);
		PreparedStatement decrPs = mock(PreparedStatement.class);
		PreparedStatement selectPs = mock(PreparedStatement.class);
		PreparedStatement deletePs = mock(PreparedStatement.class);

		when(session.prepare(queryCaptor.capture())).thenReturn(incrPs, decrPs, selectPs, deletePs);

		Map<CQLQueryType, PreparedStatement> actual = generator.prepareSimpleCounterQueryMap(session);

		assertThat(actual.get(INCR)).isSameAs(incrPs);
		assertThat(actual.get(DECR)).isSameAs(decrPs);
		assertThat(actual.get(SELECT)).isSameAs(selectPs);
		assertThat(actual.get(DELETE)).isSameAs(deletePs);

		List<String> queries = queryCaptor.getAllValues();

		assertThat(queries).hasSize(4);
		assertThat(queries.get(0)).isEqualTo(
				"UPDATE " + CQL_COUNTER_TABLE + " SET " + CQL_COUNTER_VALUE + " = " + CQL_COUNTER_VALUE + " + ? WHERE "
						+ CQL_COUNTER_FQCN + " = ? AND " + CQL_COUNTER_PRIMARY_KEY + " = ? AND "
						+ CQL_COUNTER_PROPERTY_NAME + " = ?");
		assertThat(queries.get(1)).isEqualTo(
				"UPDATE " + CQL_COUNTER_TABLE + " SET " + CQL_COUNTER_VALUE + " = " + CQL_COUNTER_VALUE + " - ? WHERE "
						+ CQL_COUNTER_FQCN + " = ? AND " + CQL_COUNTER_PRIMARY_KEY + " = ? AND "
						+ CQL_COUNTER_PROPERTY_NAME + " = ?");
		assertThat(queries.get(2)).isEqualTo(
				"SELECT " + CQL_COUNTER_VALUE + " FROM " + CQL_COUNTER_TABLE + " WHERE " + CQL_COUNTER_FQCN
						+ " = ? AND " + CQL_COUNTER_PRIMARY_KEY + " = ? AND " + CQL_COUNTER_PROPERTY_NAME + " = ?");
		assertThat(queries.get(3)).isEqualTo(
				"DELETE FROM " + CQL_COUNTER_TABLE + " WHERE " + CQL_COUNTER_FQCN + " = ? AND "
						+ CQL_COUNTER_PRIMARY_KEY + " = ? AND " + CQL_COUNTER_PROPERTY_NAME + " = ?");

	}

	@Test
	public void should_prepare_clustered_counter_queries() throws Exception {
		PropertyMeta idMeta = completeBean(Void.class, Long.class).field("id").type(ID).build();

		PropertyMeta counterMeta = completeBean(Void.class, String.class).field("count").type(COUNTER).build();

		EntityMeta meta = new EntityMeta();
		meta.setIdMeta(idMeta);
		meta.setTableName("counterTable");
		meta.setPropertyMetas(ImmutableMap.of("id", idMeta, "counter", counterMeta));

		PreparedStatement incrPs = mock(PreparedStatement.class);
		PreparedStatement decrPs = mock(PreparedStatement.class);
		PreparedStatement selectPs = mock(PreparedStatement.class);
		PreparedStatement deletePs = mock(PreparedStatement.class);

		when(session.prepare(regularStatementCaptor.capture())).thenReturn(incrPs, decrPs, selectPs, deletePs);

		Map<CQLQueryType, Map<String, PreparedStatement>> actual = generator.prepareClusteredCounterQueryMap(session,
				meta);

		assertThat(actual.get(INCR).get("count")).isSameAs(incrPs);
		assertThat(actual.get(DECR).get("count")).isSameAs(decrPs);
		assertThat(actual.get(SELECT).get("count")).isSameAs(selectPs);
		assertThat(actual.get(DELETE).get(DELETE_ALL.name())).isSameAs(deletePs);

		List<RegularStatement> regularStatements = regularStatementCaptor.getAllValues();

		assertThat(regularStatements).hasSize(5);
		assertThat(regularStatements.get(0).getQueryString()).isEqualTo(
				"UPDATE counterTable SET count=count+:count WHERE " + "id=:id;");
		assertThat(regularStatements.get(1).getQueryString()).isEqualTo(
				"UPDATE counterTable SET count=count-:count WHERE id=:id;");
		assertThat(regularStatements.get(2).getQueryString()).isEqualTo("SELECT count FROM counterTable WHERE id=:id;");
		assertThat(regularStatements.get(3).getQueryString()).isEqualTo("SELECT * FROM counterTable WHERE id=:id;");
		assertThat(regularStatements.get(4).getQueryString()).isEqualTo("DELETE  FROM counterTable WHERE id=:id;");
	}
}
