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
package org.thingsboard.server.service.ws.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.subscription.TbSubscription;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsCountSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.WEB;

/**
 * 中文说明：
 * 1. 类目的：`DefaultNotificationCommandsHandler` 是ThingsBoard Application 模块中的WebSocket 服务类型，用于维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 生命周期：随 WebSocket 建连创建订阅，断连或取消订阅时释放。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / Session。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationCommandsHandler implements NotificationCommandsHandler {

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationService notificationService;
    private final TbLocalSubscriptionService localSubscriptionService;
    /**
     * 通知，表示当前对象的对应属性。
     */
    private final NotificationCenter notificationCenter;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired @Lazy
    private WebSocketService wsService;

    /**
     * 功能：处理`Unread Notifications Sub Cmd`。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `cmd`：`cmd` 参数。
     * 返回：无。
     */
    @Override
    public void handleUnreadNotificationsSubCmd(WebSocketSessionRef sessionRef, NotificationsSubCmd cmd) {
        log.debug("[{}] Handling unread notifications subscription cmd (cmdId: {})", sessionRef.getSessionId(), cmd.getCmdId());
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        NotificationsSubscription subscription = NotificationsSubscription.builder()
                .serviceId(serviceInfoProvider.getServiceId())
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(cmd.getCmdId())
                .tenantId(securityCtx.getTenantId())
                .entityId(securityCtx.getId())
                .updateProcessor(this::handleNotificationsSubscriptionUpdate)
                .limit(cmd.getLimit())
                .build();
        localSubscriptionService.addSubscription(subscription);

        fetchUnreadNotifications(subscription);
        sendUpdate(sessionRef.getSessionId(), subscription.createFullUpdate());
    }

    /**
     * 功能：处理数量。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `cmd`：`cmd` 参数。
     * 返回：无。
     */
    @Override
    public void handleUnreadNotificationsCountSubCmd(WebSocketSessionRef sessionRef, NotificationsCountSubCmd cmd) {
        log.debug("[{}] Handling unread notifications count subscription cmd (cmdId: {})", sessionRef.getSessionId(), cmd.getCmdId());
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        NotificationsCountSubscription subscription = NotificationsCountSubscription.builder()
                .serviceId(serviceInfoProvider.getServiceId())
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(cmd.getCmdId())
                .tenantId(securityCtx.getTenantId())
                .entityId(securityCtx.getId())
                .updateProcessor(this::handleNotificationsCountSubscriptionUpdate)
                .build();
        localSubscriptionService.addSubscription(subscription);

        fetchUnreadNotificationsCount(subscription);
        sendUpdate(sessionRef.getSessionId(), subscription.createUpdate());
    }

    /**
     * 功能：获取`Unread Notifications`。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * 返回：无。
     */
    private void fetchUnreadNotifications(NotificationsSubscription subscription) {
        log.trace("[{}, subId: {}] Fetching unread notifications from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        PageData<Notification> notifications = notificationService.findLatestUnreadNotificationsByRecipientId(subscription.getTenantId(),
                WEB, (UserId) subscription.getEntityId(), subscription.getLimit());
        subscription.getLatestUnreadNotifications().clear();
        notifications.getData().forEach(notification -> {
            subscription.getLatestUnreadNotifications().put(notification.getUuidId(), notification);
        });
        subscription.getTotalUnreadCounter().set((int) notifications.getTotalElements());
    }

    /**
     * 功能：获取数量。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * 返回：无。
     */
    private void fetchUnreadNotificationsCount(NotificationsCountSubscription subscription) {
        log.trace("[{}, subId: {}] Fetching unread notifications count from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        int unreadCount = notificationService.countUnreadNotificationsByRecipientId(subscription.getTenantId(), WEB, (UserId) subscription.getEntityId());
        subscription.getTotalUnreadCounter().set(unreadCount);
    }


    /* Notifications subscription update handling */
    /**
     * 功能：处理订阅。
     * 参数：
     * - `sub`：`sub` 参数。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * 返回：无。
     */
    private void handleNotificationsSubscriptionUpdate(TbSubscription<NotificationsSubscriptionUpdate> sub, NotificationsSubscriptionUpdate subscriptionUpdate) {
        NotificationsSubscription subscription = (NotificationsSubscription) sub;
        try {
            if (subscriptionUpdate.getNotificationUpdate() != null) {
                handleNotificationUpdate(subscription, subscriptionUpdate.getNotificationUpdate());
            } else if (subscriptionUpdate.getNotificationRequestUpdate() != null) {
                handleNotificationRequestUpdate(subscription, subscriptionUpdate.getNotificationRequestUpdate());
            }
        } catch (Exception e) {
            log.error("[{}, subId: {}] Failed to handle update for notifications subscription: {}", subscription.getSessionId(), subscription.getSubscriptionId(), subscriptionUpdate, e);
        }
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * - `update`：`update` 参数。
     * 返回：无。
     */
    private void handleNotificationUpdate(NotificationsSubscription subscription, NotificationUpdate update) {
        log.trace("[{}, subId: {}] Handling notification update: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        Notification notification = update.getNotification();
        UUID notificationId = notification != null ? notification.getUuidId() : update.getNotificationId();
        if (update.isCreated()) {
            subscription.getLatestUnreadNotifications().put(notificationId, notification);
            subscription.getTotalUnreadCounter().incrementAndGet();
            if (subscription.getLatestUnreadNotifications().size() > subscription.getLimit()) {
                Set<UUID> beyondLimit = subscription.getSortedNotifications().stream().skip(subscription.getLimit())
                        .map(IdBased::getUuidId).collect(Collectors.toSet());
                beyondLimit.forEach(id -> subscription.getLatestUnreadNotifications().remove(id));
            }
            sendUpdate(subscription.getSessionId(), subscription.createPartialUpdate(notification));
        } else if (update.isUpdated()) {
            if (update.getNewStatus() == NotificationStatus.READ) {
                if (update.isAllNotifications() || subscription.getLatestUnreadNotifications().containsKey(notificationId)) {
                    fetchUnreadNotifications(subscription);
                    sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
                } else {
                    subscription.getTotalUnreadCounter().decrementAndGet();
                    sendUpdate(subscription.getSessionId(), subscription.createCountUpdate());
                }
            } else if (notification.getStatus() != NotificationStatus.READ) {
                if (subscription.getLatestUnreadNotifications().containsKey(notificationId)) {
                    subscription.getLatestUnreadNotifications().put(notificationId, notification);
                    sendUpdate(subscription.getSessionId(), subscription.createPartialUpdate(notification));
                }
            }
        } else if (update.isDeleted()) {
            if (subscription.getLatestUnreadNotifications().containsKey(notificationId)) {
                fetchUnreadNotifications(subscription);
                sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
            } else if (notification.getStatus() != NotificationStatus.READ) {
                subscription.getTotalUnreadCounter().decrementAndGet();
                sendUpdate(subscription.getSessionId(), subscription.createCountUpdate());
            }
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * - `update`：`update` 参数。
     * 返回：无。
     */
    private void handleNotificationRequestUpdate(NotificationsSubscription subscription, NotificationRequestUpdate update) {
        log.trace("[{}, subId: {}] Handling notification request update: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        fetchUnreadNotifications(subscription);
        sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
    }


    /* Notifications count subscription update handling */
    /**
     * 功能：处理订阅。
     * 参数：
     * - `sub`：`sub` 参数。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * 返回：无。
     */
    private void handleNotificationsCountSubscriptionUpdate(TbSubscription<NotificationsSubscriptionUpdate> sub, NotificationsSubscriptionUpdate subscriptionUpdate) {
        NotificationsCountSubscription subscription = (NotificationsCountSubscription) sub;
        try {
            if (subscriptionUpdate.getNotificationUpdate() != null) {
                handleNotificationUpdate(subscription, subscriptionUpdate.getNotificationUpdate());
            } else if (subscriptionUpdate.getNotificationRequestUpdate() != null) {
                handleNotificationRequestUpdate(subscription, subscriptionUpdate.getNotificationRequestUpdate());
            }
        } catch (Exception e) {
            log.error("[{}, subId: {}] Failed to handle update for notifications count subscription: {}", subscription.getSessionId(), subscription.getSubscriptionId(), subscriptionUpdate, e);
        }
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * - `update`：`update` 参数。
     * 返回：无。
     */
    private void handleNotificationUpdate(NotificationsCountSubscription subscription, NotificationUpdate update) {
        log.trace("[{}, subId: {}] Handling notification update for count sub: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        if (update.isCreated()) {
            subscription.getTotalUnreadCounter().incrementAndGet();
            sendUpdate(subscription.getSessionId(), subscription.createUpdate());
        } else if (update.isUpdated()) {
            if (update.getNewStatus() == NotificationStatus.READ) {
                if (update.isAllNotifications()) {
                    fetchUnreadNotificationsCount(subscription);
                } else {
                    subscription.getTotalUnreadCounter().decrementAndGet();
                }
                sendUpdate(subscription.getSessionId(), subscription.createUpdate());
            }
        } else if (update.isDeleted()) {
            if (update.getNotification().getStatus() != NotificationStatus.READ) {
                subscription.getTotalUnreadCounter().decrementAndGet();
                sendUpdate(subscription.getSessionId(), subscription.createUpdate());
            }
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * - `update`：`update` 参数。
     * 返回：无。
     */
    private void handleNotificationRequestUpdate(NotificationsCountSubscription subscription, NotificationRequestUpdate update) {
        log.trace("[{}, subId: {}] Handling notification request update for count sub: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        fetchUnreadNotificationsCount(subscription);
        sendUpdate(subscription.getSessionId(), subscription.createUpdate());
    }


    /**
     * 功能：处理`Mark As Read Cmd`。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `cmd`：`cmd` 参数。
     * 返回：无。
     */
    @Override
    public void handleMarkAsReadCmd(WebSocketSessionRef sessionRef, MarkNotificationsAsReadCmd cmd) {
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        cmd.getNotifications().stream()
                .map(NotificationId::new)
                .forEach(notificationId -> {
                    notificationCenter.markNotificationAsRead(securityCtx.getTenantId(), securityCtx.getId(), notificationId);
                });
    }

    /**
     * 功能：处理`Mark All As Read Cmd`。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `cmd`：`cmd` 参数。
     * 返回：无。
     */
    @Override
    public void handleMarkAllAsReadCmd(WebSocketSessionRef sessionRef, MarkAllNotificationsAsReadCmd cmd) {
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        notificationCenter.markAllNotificationsAsRead(securityCtx.getTenantId(), WEB, securityCtx.getId());
    }

    /**
     * 功能：处理`Unsub Cmd`。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `cmd`：`cmd` 参数。
     * 返回：无。
     */
    @Override
    public void handleUnsubCmd(WebSocketSessionRef sessionRef, UnsubscribeCmd cmd) {
        localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), cmd.getCmdId());
    }

    /**
     * 功能：发送或提交`Update`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `update`：`update` 参数。
     * 返回：无。
     */
    private void sendUpdate(String sessionId, CmdUpdate update) {
        log.trace("[{}, cmdId: {}] Sending WS update: {}", sessionId, update.getCmdId(), update);
        wsService.sendUpdate(sessionId, update);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNotificationCommandsHandler` 在 ThingsBoard Application 模块 中承担WebSocket 服务类型职责，核心目的是维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 核心流程：接收订阅请求后注册监听，数据变化时推送到客户端。
 * 3. 关键依赖：主要依赖或协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
