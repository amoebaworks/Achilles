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
package info.archinnov.achilles.internal.proxy.wrapper;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import info.archinnov.achilles.internal.metadata.holder.PropertyMeta;
import info.archinnov.achilles.internal.persistence.operations.EntityProxifier;
import info.archinnov.achilles.test.mapping.entity.CompleteBean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListWrapperTest {

	@Mock
	private Map<Method, PropertyMeta> dirtyMap;

	private Method setter;

	@Mock
	private PropertyMeta propertyMeta;

	@Mock
	private EntityProxifier proxifier;

	@Before
	public void setUp() throws Exception {
		setter = CompleteBean.class.getDeclaredMethod("setFriends", List.class);
	}

	@Test
	public void should_mark_dirty_on_element_add_at_index() throws Exception {

		ArrayList<String> target = new ArrayList<String>();
		ListWrapper listWrapper = prepareListWrapper(target);
		when(proxifier.removeProxy("a")).thenReturn("a");
		listWrapper.add(0, "a");

		assertThat(target).hasSize(1);
		assertThat(target.get(0)).isEqualTo("a");

		verify(dirtyMap).put(setter, propertyMeta);
	}

	@Test
	public void should_mark_dirty_on_add_all_at_index() throws Exception {

		ArrayList<String> target = new ArrayList<String>();
		target.add("a");
		ListWrapper listWrapper = prepareListWrapper(target);
		listWrapper.setProxifier(proxifier);

		Collection<String> list = Arrays.asList("b", "c");
		when(proxifier.removeProxy(list)).thenReturn(list);

		listWrapper.addAll(1, list);

		assertThat(target).hasSize(3);
		assertThat(target.get(1)).isEqualTo("b");
		assertThat(target.get(2)).isEqualTo("c");

		verify(dirtyMap).put(setter, propertyMeta);
	}

	@Test
	public void should_mark_dirty_on_remove_at_index() throws Exception {

		ArrayList<String> target = new ArrayList<String>();
		target.add("a");
		target.add("b");
		ListWrapper listWrapper = prepareListWrapper(target);
		listWrapper.remove(1);

		assertThat(target).hasSize(1);
		assertThat(target.get(0)).isEqualTo("a");

		verify(dirtyMap).put(setter, propertyMeta);
	}

	@Test
	public void should_mark_dirty_on_set() throws Exception {

		ArrayList<String> target = new ArrayList<String>();
		target.add("a");
		target.add("b");
		target.add("c");
		ListWrapper listWrapper = prepareListWrapper(target);
		when(proxifier.removeProxy("d")).thenReturn("d");
		listWrapper.set(1, "d");

		assertThat(target).hasSize(3);
		assertThat(target.get(1)).isEqualTo("d");

		verify(dirtyMap).put(setter, propertyMeta);
	}

	@Test
	public void should_mark_dirty_on_list_iterator_add() throws Exception {
		ArrayList<String> target = new ArrayList<String>();
		target.add("a");
		target.add("b");
		ListIterator<Object> listIteratorWrapper = prepareListWrapper(target).listIterator();

		assertThat(listIteratorWrapper).isInstanceOf(ListIteratorWrapper.class);
		when(proxifier.removeProxy("c")).thenReturn("c");
		listIteratorWrapper.add("c");

		verify(dirtyMap).put(setter, propertyMeta);
	}

	@Test
	public void should_mark_dirty_on_sub_list_add() throws Exception {
		ArrayList<String> target = new ArrayList<String>();
		target.add("a");
		target.add("b");
		target.add("c");
		List<Object> subListWrapper = prepareListWrapper(target).subList(0, 1);

		assertThat(subListWrapper).isInstanceOf(ListWrapper.class);
		when(proxifier.removeProxy("d")).thenReturn("d");
		subListWrapper.add("d");

		verify(dirtyMap).put(setter, propertyMeta);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void should_get_target() throws Exception {
		ArrayList<String> target = new ArrayList<String>();
		ListWrapper listWrapper = prepareListWrapper(target);

		assertThat((List) listWrapper.getTarget()).isSameAs(target);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ListWrapper prepareListWrapper(List<String> target) {
		ListWrapper listWrapper = new ListWrapper((List) target);
		listWrapper.setDirtyMap(dirtyMap);
		listWrapper.setSetter(setter);
		listWrapper.setPropertyMeta(propertyMeta);
		listWrapper.setProxifier(proxifier);
		return listWrapper;
	}
}
