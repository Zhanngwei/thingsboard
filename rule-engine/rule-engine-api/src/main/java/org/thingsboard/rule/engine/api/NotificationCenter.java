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
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;

import java.util.Set;

/**
 * 中文说明：
 * 1. 职责：定义 Rule Engine 和系统通知流程处理通知请求、Web 通知状态和可用投递方式的入口。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的通知中心边界。
 * 3. 协作对象：与通知规则节点、通知模板、通知目标、用户过滤器、通知请求统计和 Web 通知存储协作。
 * 4. 生命周期：由 Spring 实现类长期存在，规则节点或系统流程在创建、更新和查询通知时调用。
 * 5. 设计原因：通知可能投递到 Web、邮件、Slack、Firebase 等多个渠道，集中接口避免规则节点直接编排各渠道。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；实现通常访问通知数据库、模板和外部渠道，直接服务 Rule Engine。
 */
public interface NotificationCenter {

    /**
     * 功能：处理请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationRequest`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：处理结果。
     */
    NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest, FutureCallback<NotificationRequestStats> callback);

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipients`：`recipients` 参数。
     * - `template`：`template` 参数。
     * 返回：无。
     */
    void sendGeneralWebNotification(TenantId tenantId, UsersFilter recipients, NotificationTemplate template);

    /**
     * 功能：删除或清理请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationRequestId`：请求ID。
     * 返回：无。
     */
    void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId);

    /**
     * 功能：执行 `markNotificationAsRead` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：无。
     */
    void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    /**
     * 功能：执行 `markAllNotificationsAsRead` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：无。
     */
    void markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    /**
     * 功能：删除或清理通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：无。
     */
    void deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    /**
     * 功能：获取`Available Delivery Methods`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    Set<NotificationDeliveryMethod> getAvailableDeliveryMethods(TenantId tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：集中管理通知请求处理、Web 通知状态和投递方式发现。
 * 2. 核心流程：规则节点或系统流程提交 NotificationRequest，通知中心持久化、投递并通过回调返回统计。
 * 3. 关键依赖：NotificationRequest、NotificationTemplate、UsersFilter、FutureCallback 和通知存储/渠道实现。
 * 4. 学习重点：通知中心把多渠道通知编排从规则节点中抽离出来，节点只表达发送意图。
 */
