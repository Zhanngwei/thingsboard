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
package org.thingsboard.server.transport.lwm2m.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mTransportServerHelperTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
class LwM2mTransportServerHelperTest {

    /**
     * 字段说明：
     * 1. 保存 `KEY_SW_STATE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String KEY_SW_STATE = "sw_state";
    public static final String DOWNLOADING = "DOWNLOADING";

    /**
     * 字段说明：
     * 1. 保存 `now` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    long now;
    List<TransportProtos.KeyValueProto> kvList;
    /**
     * 字段说明：
     * 1. 保存 `keyTsLatestMap` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    ConcurrentMap<String, AtomicLong> keyTsLatestMap;
    LwM2mTransportServerHelper helper;
    /**
     * 字段说明：
     * 1. 保存 `context` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    LwM2mTransportContext context;


    @BeforeEach
    /**
     * 方法说明：
     * 1. 职责：执行 `setUp` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void setUp() {
        now = System.currentTimeMillis();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        context = mock(LwM2mTransportContext.class);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        helper = spy(new LwM2mTransportServerHelper(context));
        willReturn(now).given(helper).getCurrentTimeMillis();
        kvList = List.of(
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                TransportProtos.KeyValueProto.newBuilder().setKey(KEY_SW_STATE).setStringV(DOWNLOADING).build(),
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                TransportProtos.KeyValueProto.newBuilder().setKey(LOG_LWM2M_TELEMETRY).setStringV("Transport log example").build()
        );
        keyTsLatestMap = new ConcurrentHashMap<>();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyNoGetTsByKeyCall` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyNoGetTsByKeyCall() {
        assertThat(helper.getTs(null, null)).isEqualTo(now);
        assertThat(helper.getTs(null, keyTsLatestMap)).isEqualTo(now);
        assertThat(helper.getTs(emptyList(), null)).isEqualTo(now);
        assertThat(helper.getTs(emptyList(), keyTsLatestMap)).isEqualTo(now);
        assertThat(helper.getTs(kvList, null)).isEqualTo(now);

        verify(helper, never()).getTsByKey(anyString(), anyMap(), anyLong());
        verify(helper, times(5)).getCurrentTimeMillis();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyGetTsByKeyCallByFirstKey` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyGetTsByKeyCallByFirstKey() {
        assertThat(helper.getTs(kvList, keyTsLatestMap)).isEqualTo(now);

        verify(helper, times(1)).getTsByKey(kvList.get(0).getKey(), keyTsLatestMap, now);
        verify(helper, times(1)).getTsByKey(anyString(), anyMap(), anyLong());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenKeyAndEmptyLatestTsMap_whenGetTsByKey_thenAddToMapAndReturnNow` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenKeyAndEmptyLatestTsMap_whenGetTsByKey_thenAddToMapAndReturnNow() {
        assertThat(keyTsLatestMap).as("ts latest map before").isEmpty();

        assertThat(helper.getTsByKey(KEY_SW_STATE, keyTsLatestMap, now)).as("getTsByKey").isEqualTo(now);

        assertThat(keyTsLatestMap).as("ts latest map after").hasSize(1);
        assertThat(keyTsLatestMap.get(KEY_SW_STATE)).as("key present").isNotNull();
        assertThat(keyTsLatestMap.get(KEY_SW_STATE).get()).as("ts in map by key").isEqualTo(now);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenKeyAndLatestTsMapWithExistedKey_whenGetTsByKey_thenCallSwapOrIncrementMethod` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenKeyAndLatestTsMapWithExistedKey_whenGetTsByKey_thenCallSwapOrIncrementMethod() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong());
        keyTsLatestMap.put("other", new AtomicLong());
        assertThat(keyTsLatestMap).as("ts latest map").hasSize(2);
        willReturn(now).given(helper).compareAndSwapOrIncrementTsAtomically(any(AtomicLong.class), anyLong());

        assertThat(helper.getTsByKey(KEY_SW_STATE, keyTsLatestMap, now)).as("getTsByKey").isEqualTo(now);

        verify(helper, times(1)).compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now);
        verify(helper, times(1)).compareAndSwapOrIncrementTsAtomically(any(AtomicLong.class), anyLong());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenMapWithTsValueLessThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNow` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenMapWithTsValueLessThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNow() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(now - 1));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(now);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenMapWithTsValueEqualsNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNowIncremented` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenMapWithTsValueEqualsNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNowIncremented() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(now));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(now + 1);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenMapWithTsValueGreaterThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnGreaterThanNowIncremented` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenMapWithTsValueGreaterThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnGreaterThanNowIncremented() {
        final long nextHourTs = now + TimeUnit.HOURS.toMillis(1);
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(nextHourTs));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(nextHourTs + 1);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mTransportServerHelperTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
