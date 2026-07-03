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
     * 中文说明：
     * 1. 方法职责：处理一条通知请求并异步返回统计结果。
     * 2. 输入参数：tenantId 是租户边界，notificationRequest 是通知请求，callback 接收发送统计。
     * 3. 返回值：保存或处理后的 NotificationRequest。
     * 4. 调用时机：通知规则节点或系统服务需要发起通知时调用。
     * 5. 调用方：通知规则节点、系统通知流程。
     * 6. 使用流程：属于 Rule Engine 通知请求处理流程。
     * 7. 线程安全：接口无状态，具体实现需保证请求持久化和异步回调并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor；实现可能涉及数据库、缓存和外部通知渠道，服务 Rule Engine。
     */
    NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest, FutureCallback<NotificationRequestStats> callback);

    /**
     * 中文说明：
     * 1. 方法职责：发送通用 Web 平台通知。
     * 2. 输入参数：tenantId 是租户，recipients 是用户过滤条件，template 是通知模板。
     * 3. 返回值：无。
     * 4. 调用时机：系统流程需要给一组用户发送平台内通知时调用。
     * 5. 调用方：通知中心、系统通知服务或规则节点间接调用。
     * 6. 使用流程：属于 Web 通知投递流程。
     * 7. 线程安全：实现需保证模板渲染、收件人解析和通知持久化并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库和模板缓存，可由 Rule Engine 触发。
     */
    void sendGeneralWebNotification(TenantId tenantId, UsersFilter recipients, NotificationTemplate template);

    /**
     * 中文说明：
     * 1. 方法职责：删除指定通知请求。
     * 2. 输入参数：tenantId 是租户边界，notificationRequestId 是通知请求标识。
     * 3. 返回值：无。
     * 4. 调用时机：清理通知请求或管理员删除通知记录时调用。
     * 5. 调用方：通知管理流程。
     * 6. 使用流程：属于通知请求生命周期管理流程。
     * 7. 线程安全：实现需保证并发删除和状态读取一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库，可能清理缓存。
     */
    void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId);

    /**
     * 中文说明：
     * 1. 方法职责：标记单条通知为已读。
     * 2. 输入参数：tenantId 是租户，recipientId 是接收用户，notificationId 是通知标识。
     * 3. 返回值：无。
     * 4. 调用时机：用户阅读通知后调用。
     * 5. 调用方：Web 通知 API 或 UI 操作流程。
     * 6. 使用流程：属于平台通知状态更新流程。
     * 7. 线程安全：实现需保证并发状态更新一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor/Rule Engine 消息；实现通常访问数据库。
     */
    void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    /**
     * 中文说明：
     * 1. 方法职责：按投递方式将接收人的所有通知标记为已读。
     * 2. 输入参数：tenantId 是租户，deliveryMethod 是投递方式，recipientId 是接收用户。
     * 3. 返回值：无。
     * 4. 调用时机：用户执行全部已读操作时调用。
     * 5. 调用方：Web 通知 API 或 UI 操作流程。
     * 6. 使用流程：属于平台通知状态批量更新流程。
     * 7. 线程安全：实现需保证批量更新与并发新增通知的一致性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor；实现通常访问数据库并可能使用事务。
     */
    void markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    /**
     * 中文说明：
     * 1. 方法职责：删除接收人的单条通知。
     * 2. 输入参数：tenantId 是租户，recipientId 是接收用户，notificationId 是通知标识。
     * 3. 返回值：无。
     * 4. 调用时机：用户或管理流程删除通知时调用。
     * 5. 调用方：Web 通知 API 或通知管理流程。
     * 6. 使用流程：属于平台通知生命周期管理流程。
     * 7. 线程安全：实现需保证并发删除和读取一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库，可能清理缓存。
     */
    void deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    /**
     * 中文说明：
     * 1. 方法职责：获取租户当前可用的通知投递方式。
     * 2. 输入参数：tenantId 是租户边界。
     * 3. 返回值：可用 NotificationDeliveryMethod 集合。
     * 4. 调用时机：创建通知请求、渲染配置或校验目标渠道时调用。
     * 5. 调用方：通知规则节点、通知管理 API。
     * 6. 使用流程：属于通知发送前置能力发现流程。
     * 7. 线程安全：实现需保证配置读取并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现可能读取配置缓存或数据库，直接服务 Rule Engine。
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
