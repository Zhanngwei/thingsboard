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
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EXPIRED_NOTIFICATION_PROTO;

@ExtendWith(MockitoExtension.class)
/**
 * 中文说明：
 * 1. 类目的：`TransportActivityManagerTest` 是ThingsBoard Common 测试模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
public class TransportActivityManagerTest {

    private final UUID SESSION_ID = UUID.fromString("1306648a-9b26-11ee-b9d1-0242ac120002");

    @Mock
    /**
     * 字段说明：
     * 1. 保存 `transportServiceMock` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DefaultTransportService transportServiceMock;
    private ConcurrentMap<UUID, SessionMetaData> sessions;

    @BeforeEach
    /**
     * 方法说明：
     * 1. 职责：执行 `setup` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setup() {
        sessions = new ConcurrentHashMap<>();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        ReflectionTestUtils.setField(transportServiceMock, "sessions", sessions);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenFirstActivityForAlreadyRemovedSessionAndFirstEventReportingStrategy_whenOnActivity_thenShouldRecordActivityAndReport` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenFirstActivityForAlreadyRemovedSessionAndFirstEventReportingStrategy_whenOnActivity_thenShouldRecordActivityAndReport() {
        // GIVEN
        ConcurrentMap<UUID, Object> states = new ConcurrentHashMap<>();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        ReflectionTestUtils.setField(transportServiceMock, "states", states);

        var strategyMock = mock(ActivityStrategy.class);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        when(transportServiceMock.getStrategy()).thenReturn(strategyMock);
        when(strategyMock.onActivity()).thenReturn(true);

        long activityTime = 123L;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        var sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        doCallRealMethod().when(transportServiceMock).getLastRecordedTime(SESSION_ID);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        doCallRealMethod().when(transportServiceMock).onActivity(SESSION_ID, sessionInfo, activityTime);

        // WHEN
        transportServiceMock.onActivity(SESSION_ID, sessionInfo, activityTime);

        // THEN
        assertThat(states).containsKey(SESSION_ID);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        assertThat(transportServiceMock.getLastRecordedTime(SESSION_ID)).isEqualTo(activityTime);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        verify(transportServiceMock).reportActivity(eq(SESSION_ID), eq(sessionInfo), eq(activityTime), any(ActivityReportCallback.class));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenKeyAndTimeToReportAndSessionExists_whenReportingActivity_thenShouldReportActivityWithSubscriptionsAndSessionInfoFromSession` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenKeyAndTimeToReportAndSessionExists_whenReportingActivity_thenShouldReportActivityWithSubscriptionsAndSessionInfoFromSession() {
        // GIVEN
        long expectedTime = 123L;
        boolean expectedAttributesSubscription = true;
        boolean expectedRPCSubscription = true;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.SessionInfoProto expectedSessionInfo = TransportProtos.SessionInfoProto.getDefaultInstance();

        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        SessionMetaData session = new SessionMetaData(expectedSessionInfo, TransportProtos.SessionType.ASYNC, listenerMock);
        session.setSubscribedToAttributes(expectedAttributesSubscription);
        session.setSubscribedToRPC(expectedRPCSubscription);
        sessions.put(SESSION_ID, session);

        ActivityReportCallback<UUID> callbackMock = mock(ActivityReportCallback.class);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        doCallRealMethod().when(transportServiceMock).reportActivity(SESSION_ID, sessionInfo, expectedTime, callbackMock);

        // WHEN
        transportServiceMock.reportActivity(SESSION_ID, sessionInfo, expectedTime, callbackMock);

        // THEN
        ArgumentCaptor<TransportProtos.SessionInfoProto> sessionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SessionInfoProto.class);
        ArgumentCaptor<TransportProtos.SubscriptionInfoProto> subscriptionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SubscriptionInfoProto.class);
        ArgumentCaptor<TransportServiceCallback<Void>> callbackCaptor = ArgumentCaptor.forClass(TransportServiceCallback.class);

        verify(transportServiceMock).process(sessionInfoCaptor.capture(), subscriptionInfoCaptor.capture(), callbackCaptor.capture());

        assertThat(sessionInfoCaptor.getValue()).isEqualTo(expectedSessionInfo);

        TransportProtos.SubscriptionInfoProto expectedSubscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(expectedAttributesSubscription)
                .setRpcSubscription(expectedRPCSubscription)
                .setLastActivityTime(expectedTime)
                .build();
        assertThat(subscriptionInfoCaptor.getValue()).isEqualTo(expectedSubscriptionInfo);

        TransportServiceCallback<Void> queueCallback = callbackCaptor.getValue();

        queueCallback.onSuccess(null);
        verify(callbackMock).onSuccess(SESSION_ID, expectedTime);

        var throwable = new Throwable();
        queueCallback.onError(throwable);
        verify(callbackMock).onFailure(SESSION_ID, throwable);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenKeyAndTimeToReportAndSessionDoesNotExist_whenReportingActivity_thenShouldReportActivityWithNoSubscriptionsAndPreviousSessionInfo` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenKeyAndTimeToReportAndSessionDoesNotExist_whenReportingActivity_thenShouldReportActivityWithNoSubscriptionsAndPreviousSessionInfo() {
        // GIVEN
        long expectedTime = 123L;
        boolean expectedAttributesSubscription = false;
        boolean expectedRPCSubscription = false;
        TransportProtos.SessionInfoProto expectedSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        ActivityReportCallback<UUID> callbackMock = mock(ActivityReportCallback.class);

        doCallRealMethod().when(transportServiceMock).reportActivity(SESSION_ID, expectedSessionInfo, expectedTime, callbackMock);

        // WHEN
        transportServiceMock.reportActivity(SESSION_ID, expectedSessionInfo, expectedTime, callbackMock);

        // THEN
        ArgumentCaptor<TransportProtos.SessionInfoProto> sessionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SessionInfoProto.class);
        ArgumentCaptor<TransportProtos.SubscriptionInfoProto> subscriptionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SubscriptionInfoProto.class);
        ArgumentCaptor<TransportServiceCallback<Void>> callbackCaptor = ArgumentCaptor.forClass(TransportServiceCallback.class);

        verify(transportServiceMock).process(sessionInfoCaptor.capture(), subscriptionInfoCaptor.capture(), callbackCaptor.capture());

        assertThat(sessionInfoCaptor.getValue()).isEqualTo(expectedSessionInfo);

        TransportProtos.SubscriptionInfoProto expectedSubscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(expectedAttributesSubscription)
                .setRpcSubscription(expectedRPCSubscription)
                .setLastActivityTime(expectedTime)
                .build();
        assertThat(subscriptionInfoCaptor.getValue()).isEqualTo(expectedSubscriptionInfo);

        TransportServiceCallback<Void> queueCallback = callbackCaptor.getValue();

        queueCallback.onSuccess(null);
        verify(callbackMock).onSuccess(SESSION_ID, expectedTime);

        var throwable = new Throwable();
        queueCallback.onError(throwable);
        verify(callbackMock).onFailure(SESSION_ID, throwable);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenActivityHappened_whenRecordActivity_thenShouldDelegateToOnActivity` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenActivityHappened_whenRecordActivity_thenShouldDelegateToOnActivity() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        doCallRealMethod().when(transportServiceMock).recordActivity(sessionInfo);
        when(transportServiceMock.toSessionId(sessionInfo)).thenReturn(SESSION_ID);
        long expectedTime = 123L;
        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(expectedTime);

        // WHEN
        transportServiceMock.recordActivity(sessionInfo);

        // THEN
        verify(transportServiceMock).onActivity(SESSION_ID, sessionInfo, expectedTime);
    }

    @ParameterizedTest
    @EnumSource(ActivityStrategyType.class)
    /**
     * 方法说明：
     * 1. 职责：执行 `givenDifferentReportingStrategies_whenGettingStrategy_thenShouldReturnCorrectStrategy` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenDifferentReportingStrategies_whenGettingStrategy_thenShouldReturnCorrectStrategy(ActivityStrategyType reportingStrategyType) {
        // GIVEN
        doCallRealMethod().when(transportServiceMock).getStrategy();
        ReflectionTestUtils.setField(transportServiceMock, "reportingStrategyType", reportingStrategyType);

        // WHEN
        ActivityStrategy actualStrategy = transportServiceMock.getStrategy();

        // THEN
        assertThat(actualStrategy).isEqualTo(reportingStrategyType.toStrategy());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenSessionDoesNotExist_whenUpdatingActivityState_thenShouldReturnNull` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenSessionDoesNotExist_whenUpdatingActivityState_thenShouldReturnNull() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(123L);
        state.setMetadata(sessionInfo);

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isNull();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenNoGwSessionId_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenNoGwSessionId_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenHasGwSessionIdButGwSessionIsNotNull_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenHasGwSessionIdButGwSessionIsNotNull_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);

        verify(transportServiceMock, never()).getLastRecordedTime(gwSessionId);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenHasGwSessionWithoutOverwriteEnabled_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenHasGwSessionWithoutOverwriteEnabled_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        sessions.put(gwSessionId, new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock));

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);

        verify(transportServiceMock, never()).getLastRecordedTime(gwSessionId);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsGreater_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoAndLastRecordedTime` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsGreater_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoAndLastRecordedTime() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        SessionMetaData gwSession = new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock);
        gwSession.setOverwriteActivityTime(true);
        sessions.put(gwSessionId, gwSession);

        long gwLastRecordedTime = 500L;
        when(transportServiceMock.getLastRecordedTime(gwSessionId)).thenReturn(gwLastRecordedTime);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(gwLastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsLess_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoOnly` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsLess_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoOnly() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        SessionMetaData gwSession = new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock);
        gwSession.setOverwriteActivityTime(true);
        sessions.put(gwSessionId, gwSession);

        long gwLastRecordedTime = 100L;
        when(transportServiceMock.getLastRecordedTime(gwSessionId)).thenReturn(gwLastRecordedTime);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForHasExpiredTrue")
    /**
     * 方法说明：
     * 1. 职责：执行 `givenExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnTrue` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void givenExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnTrue(long currentTimeMillis, long lastRecordedTime, long sessionInactivityTimeout) {
        // GIVEN
        ReflectionTestUtils.setField(transportServiceMock, "sessionInactivityTimeout", sessionInactivityTimeout);

        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(transportServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = transportServiceMock.hasExpired(lastRecordedTime);

        // THEN
        assertThat(hasExpired).isTrue();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `provideTestParamsForHasExpiredTrue` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Stream<Arguments> provideTestParamsForHasExpiredTrue() {
        return Stream.of(
                Arguments.of(10L, 0L, 9L),
                Arguments.of(10L, 7L, 2L),
                Arguments.of(10L, 8L, 1L),
                Arguments.of(10000L, 5000L, 3000L)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForHasExpiredFalse")
    /**
     * 方法说明：
     * 1. 职责：执行 `givenNotExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnFalse` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void givenNotExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnFalse(long currentTimeMillis, long lastRecordedTime, long sessionInactivityTimeout) {
        // GIVEN
        ReflectionTestUtils.setField(transportServiceMock, "sessionInactivityTimeout", sessionInactivityTimeout);

        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(transportServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = transportServiceMock.hasExpired(lastRecordedTime);

        // THEN
        assertThat(hasExpired).isFalse();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `provideTestParamsForHasExpiredFalse` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Stream<Arguments> provideTestParamsForHasExpiredFalse() {
        return Stream.of(
                Arguments.of(10L, 9L, 2L),
                Arguments.of(10L, 0L, 11L),
                Arguments.of(10L, 8L, 3L),
                Arguments.of(10000L, 8000L, 3000L)
        );
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenSessionExists_whenOnStateExpiryCalled_thenShouldPerformExpirationActions` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenSessionExists_whenOnStateExpiryCalled_thenShouldPerformExpirationActions() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));
        doCallRealMethod().when(transportServiceMock).onStateExpiry(SESSION_ID, sessionInfo);

        // WHEN
        transportServiceMock.onStateExpiry(SESSION_ID, sessionInfo);

        // THEN
        assertThat(sessions.containsKey(SESSION_ID)).isFalse();
        verify(transportServiceMock).deregisterSession(sessionInfo);
        verify(transportServiceMock).process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
        verify(listenerMock).onRemoteSessionCloseCommand(SESSION_ID, SESSION_EXPIRED_NOTIFICATION_PROTO);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenSessionDoesNotExist_whenOnStateExpiryCalled_thenShouldNotPerformExpirationActions` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenSessionDoesNotExist_whenOnStateExpiryCalled_thenShouldNotPerformExpirationActions() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        doCallRealMethod().when(transportServiceMock).onStateExpiry(SESSION_ID, sessionInfo);

        // WHEN
        transportServiceMock.onStateExpiry(SESSION_ID, sessionInfo);

        // THEN
        assertThat(sessions.containsKey(SESSION_ID)).isFalse();
        verify(transportServiceMock, never()).deregisterSession(sessionInfo);
        verify(transportServiceMock, never()).process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TransportActivityManagerTest` 在 ThingsBoard Common 测试模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
