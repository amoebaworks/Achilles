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
package info.archinnov.achilles.internal.statement;

import static info.archinnov.achilles.type.BoundingMode.EXCLUSIVE_BOUNDS;
import static info.archinnov.achilles.type.BoundingMode.INCLUSIVE_BOUNDS;
import static info.archinnov.achilles.type.BoundingMode.INCLUSIVE_END_BOUND_ONLY;
import static info.archinnov.achilles.type.BoundingMode.INCLUSIVE_START_BOUND_ONLY;
import static info.archinnov.achilles.type.OrderingMode.ASCENDING;
import static info.archinnov.achilles.type.OrderingMode.DESCENDING;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import info.archinnov.achilles.internal.statement.wrapper.RegularStatementWrapper;
import info.archinnov.achilles.query.slice.CQLSliceQuery;
import info.archinnov.achilles.test.mapping.entity.ClusteredEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

@RunWith(MockitoJUnitRunner.class)
public class SliceQueryStatementGeneratorTest {

	private SliceQueryStatementGenerator generator = new SliceQueryStatementGenerator();

	@Mock
	private CQLSliceQuery<ClusteredEntity> sliceQuery;

	private UUID uuid1 = new UUID(10, 11);

	private List<String> componentNames = Arrays.asList("id", "a", "b", "c");

	@Before
	public void setUp() {
		when(sliceQuery.getComponentNames()).thenReturn(componentNames);
		when(sliceQuery.getVaryingComponentName()).thenReturn("c");
	}

	// ///////////////////////////////////// ASCENDING
	@Test
	public void should_generate_where_clause_when_same_number_of_components_ascending() throws Exception {
		when(sliceQuery.getOrdering()).thenReturn(ASCENDING);
		when(sliceQuery.getFixedComponents()).thenReturn(Arrays.<Object> asList(11L, uuid1, "author"));
		when(sliceQuery.getLastStartComponent()).thenReturn(1);
		when(sliceQuery.getLastEndComponent()).thenReturn(2);
		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_BOUNDS);
		when(sliceQuery.getBatchSize()).thenReturn(99);

		RegularStatementWrapper statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery,
				buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>=1 AND c<=2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(EXCLUSIVE_BOUNDS);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>1 AND c<2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_START_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>=1 AND c<2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_END_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>1 AND c<=2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);
	}

	@Test
	public void should_generate_where_clause_when_more_components_for_start_ascending() throws Exception {
		when(sliceQuery.getOrdering()).thenReturn(ASCENDING);
		when(sliceQuery.getFixedComponents()).thenReturn(Arrays.<Object> asList(11L, uuid1, "author"));
		when(sliceQuery.getLastStartComponent()).thenReturn(1);
		when(sliceQuery.getLastEndComponent()).thenReturn(null);
		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_BOUNDS);
        when(sliceQuery.getBatchSize()).thenReturn(99);

		RegularStatementWrapper statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery,
				buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>=1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(EXCLUSIVE_BOUNDS);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_START_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>=1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_END_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);
	}

	@Test
	public void should_generate_where_clause_when_more_components_for_end_ascending() throws Exception {
		when(sliceQuery.getOrdering()).thenReturn(ASCENDING);
		when(sliceQuery.getFixedComponents()).thenReturn(Arrays.<Object> asList(11L, uuid1, "author"));
		when(sliceQuery.getLastStartComponent()).thenReturn(null);
		when(sliceQuery.getLastEndComponent()).thenReturn(2);
		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_BOUNDS);
        when(sliceQuery.getBatchSize()).thenReturn(99);

		RegularStatementWrapper statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery,
				buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<=2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(EXCLUSIVE_BOUNDS);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_START_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_END_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<=2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);
	}

	// ///////////////////////////////////// DESCENDING
	@Test
	public void should_generate_where_clause_when_same_number_of_components_descending() throws Exception {
		when(sliceQuery.getOrdering()).thenReturn(DESCENDING);
		when(sliceQuery.getFixedComponents()).thenReturn(Arrays.<Object> asList(11L, uuid1, "author"));
		when(sliceQuery.getLastStartComponent()).thenReturn(2);
		when(sliceQuery.getLastEndComponent()).thenReturn(1);
		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_BOUNDS);
        when(sliceQuery.getBatchSize()).thenReturn(99);

		RegularStatementWrapper statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery,
				buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<=2 AND c>=1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(EXCLUSIVE_BOUNDS);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<2 AND c>1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_START_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<=2 AND c>1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_END_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<2 AND c>=1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);
	}

	@Test
	public void should_generate_where_clause_when_more_components_for_start_descending() throws Exception {
		when(sliceQuery.getOrdering()).thenReturn(DESCENDING);
		when(sliceQuery.getFixedComponents()).thenReturn(Arrays.<Object> asList(11L, uuid1, "author"));
		when(sliceQuery.getLastStartComponent()).thenReturn(2);
		when(sliceQuery.getLastEndComponent()).thenReturn(null);
		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_BOUNDS);
        when(sliceQuery.getBatchSize()).thenReturn(99);

		RegularStatementWrapper statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery,
				buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<=2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(EXCLUSIVE_BOUNDS);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_START_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<=2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_END_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c<2;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);
	}

	@Test
	public void should_generate_where_clause_when_more_components_for_end_descending() throws Exception {
		when(sliceQuery.getOrdering()).thenReturn(DESCENDING);
		when(sliceQuery.getFixedComponents()).thenReturn(Arrays.<Object> asList(11L, uuid1, "author"));
		when(sliceQuery.getLastStartComponent()).thenReturn(null);
		when(sliceQuery.getLastEndComponent()).thenReturn(1);
		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_BOUNDS);
        when(sliceQuery.getBatchSize()).thenReturn(99);

		RegularStatementWrapper statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery,
				buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>=1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(EXCLUSIVE_BOUNDS);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_START_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);

		when(sliceQuery.getBounding()).thenReturn(INCLUSIVE_END_BOUND_ONLY);
		statement = generator.generateWhereClauseForSelectSliceQuery(sliceQuery, buildFakeSelect());
		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"SELECT test FROM table WHERE id=11 AND a=? AND b=? AND c>=1;");
		assertThat(statement.getValues()).contains(uuid1, "author");
        assertThat(statement.getStatement().getFetchSize()).isEqualTo(99);
	}

	@Test
	public void should_generate_where_clause() throws Exception {
		when(sliceQuery.getFixedComponents()).thenReturn(Arrays.<Object> asList(11L, uuid1, "author"));

		RegularStatementWrapper statement = generator.generateWhereClauseForDeleteSliceQuery(sliceQuery,
				buildFakeDelete());

		assertThat(statement.getStatement().getQueryString()).isEqualTo(
				"DELETE  FROM table WHERE id=11 AND a=? AND b=?;");
	}

	private Select buildFakeSelect() {
		Select select = QueryBuilder.select("test").from("table");
		return select;
	}

	private Delete buildFakeDelete() {
		return QueryBuilder.delete().from("table");
	}
}
