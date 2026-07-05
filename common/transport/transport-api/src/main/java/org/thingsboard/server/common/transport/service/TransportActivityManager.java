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
@Slf4j
public abstract class TransportActivityManager extends AbstractActivityManager<UUID, TransportProtos.SessionInfoProto> implements TransportService {

    /**
     * 会话常量，用于统一引用固定值。
     */
    public static final String SESSION_EXPIRED_MESSAGE = "Session has expired due to last activity time!";

    public static final TransportProtos.SessionEventMsg SESSION_EVENT_MSG_CLOSED = TransportProtos.SessionEventMsg.newBuilder()
            .setSessionType(TransportProtos.SessionType.ASYNC)
            .setEvent(TransportProtos.SessionEvent.CLOSED).build();
    public static final TransportProtos.SessionCloseNotificationProto SESSION_EXPIRED_NOTIFICATION_PROTO = TransportProtos.SessionCloseNotificationProto.newBuilder()
            .setMessage(SESSION_EXPIRED_MESSAGE).build();

    public final ConcurrentMap<UUID, SessionMetaData> sessions = new ConcurrentHashMap<>();

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${transport.sessions.report_timeout}")
    protected long sessionReportTimeout;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${transport.sessions.inactivity_timeout}")
    protected long sessionInactivityTimeout;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Value("${transport.activity.reporting_strategy:LAST}")
    private ActivityStrategyType reportingStrategyType;

    /**
     * 功能：获取上报。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected long getReportingPeriodMillis() {
        return sessionReportTimeout;
    }

    /**
     * 功能：获取策略对象。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected ActivityStrategy getStrategy() {
        return reportingStrategyType.toStrategy();
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `sessionId`：会话ID。
     * - `state`：`state` 参数。
     * 返回：处理结果。
     */
    @Override
    protected ActivityState<TransportProtos.SessionInfoProto> updateState(UUID sessionId, ActivityState<TransportProtos.SessionInfoProto> state) {
        SessionMetaData session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }

        state.setMetadata(session.getSessionInfo());
        var sessionInfo = state.getMetadata();

        if (sessionInfo.getGwSessionIdMSB() == 0L || sessionInfo.getGwSessionIdLSB() == 0L) {
            return state;
        }

        var gwSessionId = new UUID(sessionInfo.getGwSessionIdMSB(), sessionInfo.getGwSessionIdLSB());
        SessionMetaData gwSession = sessions.get(gwSessionId);
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

    /**
     * 功能：判断`Expired`。
     * 参数：
     * - `lastRecordedTime`：`lastRecordedTime` 参数。
     * 返回：判断结果。
     */
    @Override
    protected boolean hasExpired(long lastRecordedTime) {
        return (getCurrentTimeMillis() - sessionInactivityTimeout) > lastRecordedTime;
    }

    /**
     * 功能：处理状态。
     * 参数：
     * - `sessionId`：会话ID。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    @Override
    protected void onStateExpiry(UUID sessionId, TransportProtos.SessionInfoProto sessionInfo) {
        log.debug("Session with id: [{}] has expired due to last activity time.", sessionId);
        SessionMetaData expiredSession = sessions.remove(sessionId);
        if (expiredSession != null) {
            deregisterSession(sessionInfo);
            process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
            expiredSession.getListener().onRemoteSessionCloseCommand(sessionId, SESSION_EXPIRED_NOTIFICATION_PROTO);
        }
    }

    /**
     * 功能：上报`Activity`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `currentSessionInfo`：会话对象。
     * - `timeToReport`：`timeToReport` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    protected void reportActivity(UUID sessionId, TransportProtos.SessionInfoProto currentSessionInfo, long timeToReport, ActivityReportCallback<UUID> callback) {
        log.debug("Reporting activity state for session with id: [{}]. Time to report: [{}].", sessionId, timeToReport);
        SessionMetaData session = sessions.get(sessionId);
        TransportProtos.SubscriptionInfoProto subscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(session != null && session.isSubscribedToAttributes())
                .setRpcSubscription(session != null && session.isSubscribedToRPC())
                .setLastActivityTime(timeToReport)
                .build();
        TransportProtos.SessionInfoProto sessionInfo = session != null ? session.getSessionInfo() : currentSessionInfo;
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
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
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
