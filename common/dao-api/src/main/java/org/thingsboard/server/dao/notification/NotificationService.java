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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

/**
 * 中文说明：
 * 1. `NotificationService` 是 ThingsBoard Common 中定义通知能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface NotificationService {

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notification`：`notification` 参数。
     * 返回：处理结果。
     */
    Notification saveNotification(TenantId tenantId, Notification notification);

    /**
     * 功能：获取通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationId`：通知ID。
     * 返回：处理结果。
     */
    Notification findNotificationById(TenantId tenantId, NotificationId notificationId);

    /**
     * 功能：执行 `markNotificationAsRead` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：判断结果。
     */
    boolean markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    /**
     * 功能：执行 `markAllNotificationsAsRead` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：数值结果。
     */
    int markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

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
    PageData<Notification> findNotificationsByRecipientIdAndReadStatus(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, boolean unreadOnly, PageLink pageLink);

    /**
     * 功能：获取`Latest Unread Notifications By Recipient Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    PageData<Notification> findLatestUnreadNotificationsByRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, int limit);

    /**
     * 功能：统计`Unread Notifications By Recipient Id`数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：数值结果。
     */
    int countUnreadNotificationsByRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    /**
     * 功能：删除或清理通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationId`：通知ID。
     * 返回：判断结果。
     */
    boolean deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId);

}
