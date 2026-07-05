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

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.OtherServiceShutdownEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`SubscriptionManagerService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public interface SubscriptionManagerService extends ApplicationListener<PartitionChangeEvent> {

    /**
     * 功能：处理事件。
     * 参数：
     * - `serviceId`：服务ID。
     * - `event`：`event` 参数。
     * - `empty`：`empty` 参数。
     * 返回：无。
     */
    void onSubEvent(String serviceId, TbEntitySubEvent event, TbCallback empty);

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    void onApplicationEvent(OtherServiceShutdownEvent event);

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, TbCallback callback);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, TbCallback callback);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, TbCallback callback);

    /**
     * 功能：处理`on Attributes Delete`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, TbCallback empty);

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, TbCallback callback);

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAlarmUpdate(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback);

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAlarmDeleted(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback);

    /**
     * 功能：处理通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationUpdate`：`notificationUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onNotificationUpdate(TenantId tenantId, UserId recipientId, NotificationUpdate notificationUpdate, TbCallback callback);

}

/*
 * 本类总结：
 * 1. 核心职责：`SubscriptionManagerService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
