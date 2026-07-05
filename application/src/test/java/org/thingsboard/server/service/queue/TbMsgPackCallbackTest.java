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
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. 类目的：`TbMsgPackCallbackTest` 是ThingsBoard Application 测试模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
class TbMsgPackCallbackTest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    TenantId tenantId;
    UUID msgId;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    TbMsgPackProcessingContext ctx;
    TbMsgPackCallback callback;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        msgId = UUID.randomUUID();
        ctx = mock(TbMsgPackProcessingContext.class);
        callback = spy(new TbMsgPackCallback(msgId, tenantId, ctx));
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> testOnFailure_NotRateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("rule engine no cause")),
                Arguments.of(new RuleEngineException("rule engine caused 1 lvl", new RuntimeException())),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl", new RuntimeException(new Exception()))),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl Throwable", new RuntimeException(new Throwable()))),
                Arguments.of(new RuleNodeException("rule node no cause", "RuleChain", new RuleNode()))
        );
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：
     * - `ree`：`ree` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource
    void testOnFailure_NotRateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback, never()).onRateLimit(any());
        verify(callback, never()).onSuccess();
        verify(ctx, never()).onSuccess(any());
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> testOnFailure_RateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("caused lvl 1", new TbRateLimitsException(EntityType.ASSET))),
                Arguments.of(new RuleEngineException("caused lvl 2", new RuntimeException(new TbRateLimitsException(EntityType.ASSET)))),
                Arguments.of(
                        new RuleEngineException("caused lvl 3",
                                new RuntimeException(
                                        new Exception(
                                                new TbRateLimitsException(EntityType.ASSET)))))
        );
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：
     * - `ree`：`ree` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource
    void testOnFailure_RateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback).onRateLimit(any());
        verify(callback).onFailure(any());
        verify(callback, never()).onSuccess();
        verify(ctx).onSuccess(msgId);
        verify(ctx).onSuccess(any());
        verify(ctx, never()).onFailure(any(), any(), any());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgPackCallbackTest` 在 ThingsBoard Application 测试模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
