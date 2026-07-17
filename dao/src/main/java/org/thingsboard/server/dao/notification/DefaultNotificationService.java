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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `DefaultNotificationService` 是 ThingsBoard DAO 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `NotificationService`、`EntityDaoService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService, EntityDaoService {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final NotificationDao notificationDao;

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notification`：`notification` 参数。
     * 返回：处理结果。
     */
    @Override
    public Notification saveNotification(TenantId tenantId, Notification notification) {
        return notificationDao.save(tenantId, notification);
    }

    /**
     * 功能：获取通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationId`：通知ID。
     * 返回：处理结果。
     */
    @Override
    public Notification findNotificationById(TenantId tenantId, NotificationId notificationId) {
        return notificationDao.findById(tenantId, notificationId.getId());
    }

    /**
     * 功能：执行 `markNotificationAsRead` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：判断结果。
     */
    @Override
    public boolean markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        return notificationDao.updateStatusByIdAndRecipientId(tenantId, recipientId, notificationId, NotificationStatus.READ);
    }

    /**
     * 功能：执行 `markAllNotificationsAsRead` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：数值结果。
     */
    @Override
    public int markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId) {
        return notificationDao.updateStatusByDeliveryMethodAndRecipientId(tenantId, deliveryMethod, recipientId, NotificationStatus.READ);
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `unreadOnly`：`unreadOnly` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Notification> findNotificationsByRecipientIdAndReadStatus(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, boolean unreadOnly, PageLink pageLink) {
        if (unreadOnly) {
            return notificationDao.findUnreadByDeliveryMethodAndRecipientIdAndPageLink(tenantId, deliveryMethod, recipientId, pageLink);
        } else {
            return notificationDao.findByDeliveryMethodAndRecipientIdAndPageLink(tenantId, deliveryMethod, recipientId, pageLink);
        }
    }

    /**
     * 功能：获取`Latest Unread Notifications By Recipient Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Notification> findLatestUnreadNotificationsByRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, int limit) {
        SortOrder sortOrder = new SortOrder(EntityKeyMapping.CREATED_TIME, SortOrder.Direction.DESC);
        PageLink pageLink = new PageLink(limit, 0, null, sortOrder);
        return findNotificationsByRecipientIdAndReadStatus(tenantId, deliveryMethod, recipientId, true, pageLink);
    }

    /**
     * 功能：统计`Unread Notifications By Recipient Id`数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：数值结果。
     */
    @Override
    public int countUnreadNotificationsByRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId) {
        return notificationDao.countUnreadByDeliveryMethodAndRecipientId(tenantId, deliveryMethod, recipientId);
    }

    /**
     * 功能：删除或清理通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：判断结果。
     */
    @Override
    public boolean deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        return notificationDao.deleteByIdAndRecipientId(tenantId, recipientId, notificationId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findNotificationById(tenantId, new NotificationId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION;
    }

}
