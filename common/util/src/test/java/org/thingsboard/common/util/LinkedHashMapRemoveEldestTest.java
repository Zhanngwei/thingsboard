/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.common.util;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 中文说明：
 * 1. `LinkedHashMapRemoveEldestTest` 是 ThingsBoard Common 中验证 `LinkedHashMapRemoveEldest` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class LinkedHashMapRemoveEldestTest {

    /**
     * `MAX_ENTRIES`常量，用于统一引用固定值。
     */
    public static final long MAX_ENTRIES = 10L;
    long removeCount = 0;

    /**
     * 功能：执行 `removalConsumer` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `name`：名称。
     * 返回：无。
     */
    void removalConsumer(Long id, String name) {
        removeCount++;
        assertThat(id, is(Matchers.lessThan(MAX_ENTRIES)));
        assertThat(name, is(id.toString()));
    }

    /**
     * 功能：验证 `givenMap_whenOverSized_thenVerifyRemovedEldest` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMap_whenOverSized_thenVerifyRemovedEldest() {
        //given
        LinkedHashMapRemoveEldest<Long, String> map =
                new LinkedHashMapRemoveEldest<>(MAX_ENTRIES, this::removalConsumer);

        assertThat(map.getMaxEntries(), is(MAX_ENTRIES));
        assertThat(map.getRemovalConsumer(), notNullValue());
        assertThat(map, instanceOf(LinkedHashMap.class));
        assertThat(map, instanceOf(LinkedHashMapRemoveEldest.class));
        assertThat(map.size(), is(0));

        //when
        for (long i = 0; i < MAX_ENTRIES * 2; i++) {
            map.put(i, String.valueOf(i));
        }

        //then
        assertThat((long) map.size(), is(MAX_ENTRIES));
        assertThat(removeCount, is(MAX_ENTRIES));
        for (long i = MAX_ENTRIES; i < MAX_ENTRIES * 2; i++) {
            assertThat(map.get(i), is(String.valueOf(i)));
        }
    }
}