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
package org.thingsboard.server.service.ws.notification.sub;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.service.subscription.TbSubscription;
import org.thingsboard.server.service.subscription.TbSubscriptionType;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `NotificationsSubscription` 是 ThingsBoard Application 中围绕 `Notifications Subscription` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractNotificationSubscription`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Getter
public class NotificationsSubscription extends AbstractNotificationSubscription<NotificationsSubscriptionUpdate> {

    private final Map<UUID, Notification> latestUnreadNotifications = new HashMap<>();
    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    private final int limit;

    /**
     * 功能：创建 `NotificationsSubscription` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `sessionId`：会话ID。
     * - `subscriptionId`：订阅ID。
     * - `tenantId`：租户IDID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    public NotificationsSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                     BiConsumer<TbSubscription<NotificationsSubscriptionUpdate>, NotificationsSubscriptionUpdate> updateProcessor,
                                     int limit) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.NOTIFICATIONS, updateProcessor);
        this.limit = limit;
    }

    /**
     * 功能：保存或创建`Full Update`。
     * 参数：无。
     * 返回：处理结果。
     */
    public UnreadNotificationsUpdate createFullUpdate() {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .notifications(getSortedNotifications())
                .totalUnreadCount(totalUnreadCounter.get())
                .sequenceNumber(sequence.incrementAndGet())
                .build();
    }

    /**
     * 功能：获取`Sorted Notifications`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<Notification> getSortedNotifications() {
        return latestUnreadNotifications.values().stream()
                .sorted(Comparator.comparing(BaseData::getCreatedTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * 功能：保存或创建`Partial Update`。
     * 参数：
     * - `notification`：`notification` 参数。
     * 返回：处理结果。
     */
    public UnreadNotificationsUpdate createPartialUpdate(Notification notification) {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .update(notification)
                .totalUnreadCount(totalUnreadCounter.get())
                .sequenceNumber(sequence.incrementAndGet())
                .build();
    }

    /**
     * 功能：保存或创建数量。
     * 参数：无。
     * 返回：处理结果。
     */
    public UnreadNotificationsUpdate createCountUpdate() {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .totalUnreadCount(totalUnreadCounter.get())
                .sequenceNumber(sequence.incrementAndGet())
                .build();
    }

}
