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
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

/**
 * 中文说明：
 * 1. 类目的：`NotificationDao` 是 ThingsBoard DAO 模块 中的通知持久化服务类型，用于管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
 * 4. 生命周期：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Scheduler Command。
 */
public interface NotificationDao extends Dao<Notification> {

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Notification> findUnreadByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink);

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Notification> findByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink);

    /**
     * 功能：更新状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * - `status`：`status` 参数。
     * 返回：判断结果。
     */
    boolean updateStatusByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId, NotificationStatus status);

    /**
     * 功能：统计`Unread By Delivery Method And Recipient Id`数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：数值结果。
     */
    int countUnreadByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    /**
     * 功能：删除或清理`By Id And Recipient Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：判断结果。
     */
    boolean deleteByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    /**
     * 功能：更新状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `status`：`status` 参数。
     * 返回：数值结果。
     */
    int updateStatusByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, NotificationStatus status);

    /**
     * 功能：删除或清理请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestId`：请求ID。
     * 返回：无。
     */
    void deleteByRequestId(TenantId tenantId, NotificationRequestId requestId);

    /**
     * 功能：删除或清理`By Recipient Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * 返回：无。
     */
    void deleteByRecipientId(TenantId tenantId, UserId recipientId);

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationDao` 在 ThingsBoard DAO 模块 中承担通知持久化服务类型职责，核心目的是管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
 * 2. 核心流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
 * 3. 关键依赖：主要依赖或协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
