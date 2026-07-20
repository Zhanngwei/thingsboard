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
package org.thingsboard.server.dao.sql.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.NotificationEntity;
import org.thingsboard.server.dao.notification.NotificationDao;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `JpaNotificationDao` 是 ThingsBoard DAO 中负责通知存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaPartitionedAbstractDao`、`NotificationDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationDao extends JpaPartitionedAbstractDao<NotificationEntity, Notification> implements NotificationDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final NotificationRepository notificationRepository;
    private final SqlPartitioningRepository partitioningRepository;

    /**
     * 分区，用于定位消息或数据所属分区。
     */
    @Value("${sql.notifications.partition_size:168}")
    private int partitionSizeInHours;

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Notification> findUnreadByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRepository.findByDeliveryMethodAndRecipientIdAndStatusNot(deliveryMethod,
                recipientId.getId(), NotificationStatus.READ, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Notification> findByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRepository.findByDeliveryMethodAndRecipientId(deliveryMethod, recipientId.getId(),
                pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * - `status`：`status` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean updateStatusByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId, NotificationStatus status) {
        return notificationRepository.updateStatusByIdAndRecipientId(notificationId.getId(), recipientId.getId(), status) != 0;
    }

    /**
     * For this hot method, the partial index `idx_notification_recipient_id_unread` was introduced since 3.6.0
     * */
    /**
     * 功能：统计`Unread By Delivery Method And Recipient Id`数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：数值结果。
     */
    @Override
    public int countUnreadByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId) {
        return notificationRepository.countByDeliveryMethodAndRecipientIdAndStatusNot(deliveryMethod, recipientId.getId(), NotificationStatus.READ);
    }

    /**
     * 功能：删除或清理`By Id And Recipient Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：判断结果。
     */
    @Override
    public boolean deleteByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        return notificationRepository.deleteByIdAndRecipientId(notificationId.getId(), recipientId.getId()) != 0;
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `status`：`status` 参数。
     * 返回：数值结果。
     */
    @Override
    public int updateStatusByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, NotificationStatus status) {
        return notificationRepository.updateStatusByDeliveryMethodAndRecipientIdAndStatusNot(deliveryMethod, recipientId.getId(), status);
    }

    /**
     * 功能：删除或清理请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestId`：请求ID。
     * 返回：无。
     */
    @Override
    public void deleteByRequestId(TenantId tenantId, NotificationRequestId requestId) {
        notificationRepository.deleteByRequestId(requestId.getId());
    }

    /**
     * 功能：删除或清理`By Recipient Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * 返回：无。
     */
    @Override
    public void deleteByRecipientId(TenantId tenantId, UserId recipientId) {
        notificationRepository.deleteByRecipientId(recipientId.getId());
    }

    /**
     * 功能：保存或创建分区。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    @Override
    public void createPartition(NotificationEntity entity) {
        partitioningRepository.createPartitionIfNotExists(ModelConstants.NOTIFICATION_TABLE_NAME,
                entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<NotificationEntity> getEntityClass() {
        return NotificationEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<NotificationEntity, UUID> getRepository() {
        return notificationRepository;
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
