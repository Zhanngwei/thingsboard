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
 * 1. 类目的：`LinkedHashMapRemoveEldestTest` 是ThingsBoard Common 测试模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
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


/*
 * 本类总结：
 * 1. 核心职责：`LinkedHashMapRemoveEldestTest` 在 ThingsBoard Common 测试模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}