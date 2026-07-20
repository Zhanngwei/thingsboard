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
 * 1. `NotificationCenter` 是 ThingsBoard Rule Engine API 中定义通知能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
