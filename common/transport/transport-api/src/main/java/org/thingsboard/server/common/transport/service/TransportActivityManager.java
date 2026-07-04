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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`TransportActivityManager` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
public abstract class TransportActivityManager extends AbstractActivityManager<UUID, TransportProtos.SessionInfoProto> implements TransportService {

    /**
     * 字段说明：
     * 1. 保存 `SESSION_EXPIRED_MESSAGE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String SESSION_EXPIRED_MESSAGE = "Session has expired due to last activity time!";

    public static final TransportProtos.SessionEventMsg SESSION_EVENT_MSG_CLOSED = TransportProtos.SessionEventMsg.newBuilder()
            .setSessionType(TransportProtos.SessionType.ASYNC)
            .setEvent(TransportProtos.SessionEvent.CLOSED).build();
    public static final TransportProtos.SessionCloseNotificationProto SESSION_EXPIRED_NOTIFICATION_PROTO = TransportProtos.SessionCloseNotificationProto.newBuilder()
            .setMessage(SESSION_EXPIRED_MESSAGE).build();

    public final ConcurrentMap<UUID, SessionMetaData> sessions = new ConcurrentHashMap<>();

    @Value("${transport.sessions.report_timeout}")
    /**
     * 字段说明：
     * 1. 保存 `sessionReportTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected long sessionReportTimeout;

    @Value("${transport.sessions.inactivity_timeout}")
    /**
     * 字段说明：
     * 1. 保存 `sessionInactivityTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected long sessionInactivityTimeout;

    @Value("${transport.activity.reporting_strategy:LAST}")
    /**
     * 字段说明：
     * 1. 保存 `reportingStrategyType` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ActivityStrategyType reportingStrategyType;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getReportingPeriodMillis` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected long getReportingPeriodMillis() {
        return sessionReportTimeout;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getStrategy` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected ActivityStrategy getStrategy() {
        return reportingStrategyType.toStrategy();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateState` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected ActivityState<TransportProtos.SessionInfoProto> updateState(UUID sessionId, ActivityState<TransportProtos.SessionInfoProto> state) {
        SessionMetaData session = sessions.get(sessionId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (session == null) {
            return null;
        }

        state.setMetadata(session.getSessionInfo());
        var sessionInfo = state.getMetadata();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (sessionInfo.getGwSessionIdMSB() == 0L || sessionInfo.getGwSessionIdLSB() == 0L) {
            return state;
        }

        var gwSessionId = new UUID(sessionInfo.getGwSessionIdMSB(), sessionInfo.getGwSessionIdLSB());
        SessionMetaData gwSession = sessions.get(gwSessionId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (gwSession == null || !gwSession.isOverwriteActivityTime()) {
            return state;
        }

        long lastRecordedTime = state.getLastRecordedTime();
        long gwLastRecordedTime = getLastRecordedTime(gwSessionId);
        log.debug("Session with id: [{}] has gateway session with id: [{}] with overwrite activity time enabled. " +
                        "Updating last activity time. Session last recorded time: [{}], gateway session last recorded time: [{}].",
                sessionId, gwSessionId, lastRecordedTime, gwLastRecordedTime);
        state.setLastRecordedTime(Math.max(lastRecordedTime, gwLastRecordedTime));
        return state;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `hasExpired` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean hasExpired(long lastRecordedTime) {
        return (getCurrentTimeMillis() - sessionInactivityTimeout) > lastRecordedTime;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onStateExpiry` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void onStateExpiry(UUID sessionId, TransportProtos.SessionInfoProto sessionInfo) {
        log.debug("Session with id: [{}] has expired due to last activity time.", sessionId);
        SessionMetaData expiredSession = sessions.remove(sessionId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (expiredSession != null) {
            deregisterSession(sessionInfo);
            process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
            expiredSession.getListener().onRemoteSessionCloseCommand(sessionId, SESSION_EXPIRED_NOTIFICATION_PROTO);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `reportActivity` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void reportActivity(UUID sessionId, TransportProtos.SessionInfoProto currentSessionInfo, long timeToReport, ActivityReportCallback<UUID> callback) {
        log.debug("Reporting activity state for session with id: [{}]. Time to report: [{}].", sessionId, timeToReport);
        SessionMetaData session = sessions.get(sessionId);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.SubscriptionInfoProto subscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(session != null && session.isSubscribedToAttributes())
                .setRpcSubscription(session != null && session.isSubscribedToRPC())
                .setLastActivityTime(timeToReport)
                .build();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.SessionInfoProto sessionInfo = session != null ? session.getSessionInfo() : currentSessionInfo;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        process(sessionInfo, subscriptionInfo, new TransportServiceCallback<>() {
            @Override
            public void onSuccess(Void msgAcknowledged) {
                callback.onSuccess(sessionId, timeToReport);

            }

            @Override
            public void onError(Throwable e) {
                callback.onFailure(sessionId, e);
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCurrentTimeMillis` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TransportActivityManager` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
