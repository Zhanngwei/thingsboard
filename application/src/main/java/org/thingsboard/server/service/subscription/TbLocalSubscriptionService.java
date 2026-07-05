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
package org.thingsboard.server.service.subscription;

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`TbLocalSubscriptionService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public interface TbLocalSubscriptionService {

    /**
     * 功能：保存或创建订阅。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * 返回：无。
     */
    void addSubscription(TbSubscription<?> subscription);

    /**
     * 功能：处理事件。
     * 参数：
     * - `subEventCallback`：处理完成后的回调。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onSubEventCallback(TransportProtos.TbEntitySubEventCallbackProto subEventCallback, TbCallback callback);

    /**
     * 功能：处理事件。
     * 参数：
     * - `entityId`：实体IDID。
     * - `seqNumber`：`seqNumber` 参数。
     * - `entityUpdatesInfo`：实体对象。
     * - `empty`：`empty` 参数。
     * 返回：无。
     */
    void onSubEventCallback(EntityId entityId, int seqNumber, TbEntityUpdatesInfo entityUpdatesInfo, TbCallback empty);

    /**
     * 功能：执行 `cancelSubscription` 对应的处理。
     * 参数：
     * - `sessionId`：会话ID。
     * - `subscriptionId`：订阅ID。
     * 返回：无。
     */
    void cancelSubscription(String sessionId, int subscriptionId);

    /**
     * 功能：执行 `cancelAllSessionSubscriptions` 对应的处理。
     * 参数：
     * - `sessionId`：会话ID。
     * 返回：无。
     */
    void cancelAllSessionSubscriptions(String sessionId);

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tsUpdate`：`tsUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTimeSeriesUpdate(TransportProtos.TbSubUpdateProto tsUpdate, TbCallback callback);

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `update`：数据列表。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTimeSeriesUpdate(EntityId entityId, List<TsKvEntry> update, TbCallback callback);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `attrUpdate`：`attrUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAttributesUpdate(TransportProtos.TbSubUpdateProto attrUpdate, TbCallback callback);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `update`：数据列表。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAttributesUpdate(EntityId entityId, String scope, List<TsKvEntry> update, TbCallback callback);

    /**
     * 功能：处理告警。
     * 参数：
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `deleted`：`deleted` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAlarmUpdate(EntityId entityId, AlarmInfo alarm, boolean deleted, TbCallback callback);

    /**
     * 功能：处理告警。
     * 参数：
     * - `update`：`update` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAlarmUpdate(TransportProtos.TbAlarmSubUpdateProto update, TbCallback callback);

    /**
     * 功能：处理通知。
     * 参数：
     * - `entityId`：实体IDID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onNotificationUpdate(EntityId entityId, NotificationsSubscriptionUpdate subscriptionUpdate, TbCallback callback);

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    void onApplicationEvent(ClusterTopologyChangeEvent event);

    /**
     * 功能：处理消息。
     * 参数：
     * - `coreStartupMsg`：待处理消息。
     * 返回：无。
     */
    void onCoreStartupMsg(TransportProtos.CoreStartupMsg coreStartupMsg);

    /**
     * 功能：处理请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `update`：`update` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onNotificationRequestUpdate(TenantId tenantId, NotificationRequestUpdate update, TbCallback callback);

    /**
     * 功能：处理通知。
     * 参数：
     * - `notificationsUpdate`：`notificationsUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onNotificationUpdate(TransportProtos.NotificationsSubUpdateProto notificationsUpdate, TbCallback callback);

}

/*
 * 本类总结：
 * 1. 核心职责：`TbLocalSubscriptionService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
