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
package org.thingsboard.server.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsCountUpdate;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdateType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`NotificationApiWsClient` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Getter
public class NotificationApiWsClient extends TbTestWebSocketClient {

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private UnreadNotificationsUpdate lastDataUpdate;
    private UnreadNotificationsCountUpdate lastCountUpdate;

    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    private int limit;
    private int unreadCount;
    /**
     * `notifications`列表，用于保存一组待处理对象。
     */
    private List<Notification> notifications;

    /**
     * 功能：创建 `NotificationApiWsClient` 实例，并初始化必要字段。
     * 参数：
     * - `wsUrl`：`wsUrl` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationApiWsClient(String wsUrl) throws URISyntaxException {
        super(new URI(wsUrl + "/api/ws"));
    }

    /**
     * 功能：订阅`For Unread Notifications`。
     * 参数：
     * - `limit`：数量限制。
     * 返回：处理结果。
     */
    public NotificationApiWsClient subscribeForUnreadNotifications(int limit) {
        send(new NotificationsSubCmd(1, limit));
        this.limit = limit;
        return this;
    }

    /**
     * 功能：订阅数量。
     * 参数：无。
     * 返回：处理结果。
     */
    public NotificationApiWsClient subscribeForUnreadNotificationsCount() {
        send(new NotificationsCountSubCmd(2));
        return this;
    }

    /**
     * 功能：执行 `markNotificationAsRead` 对应的处理。
     * 参数：
     * - `notifications`：`notifications` 参数。
     * 返回：无。
     */
    public void markNotificationAsRead(UUID... notifications) {
        send(new MarkNotificationsAsReadCmd(newCmdId(), Arrays.asList(notifications)));
    }

    /**
     * 功能：执行 `markAllNotificationsAsRead` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void markAllNotificationsAsRead() {
        send(new MarkAllNotificationsAsReadCmd(newCmdId()));
    }

    /**
     * 功能：保存或创建`Wait For Update`。
     * 参数：
     * - `count`：`count` 参数。
     * 返回：无。
     */
    @Override
    public void registerWaitForUpdate(int count) {
        lastDataUpdate = null;
        lastCountUpdate = null;
        super.registerWaitForUpdate(count);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：无。
     */
    @Override
    public void onMessage(String s) {
        JsonNode update = JacksonUtil.toJsonNode(s);
        CmdUpdateType updateType = CmdUpdateType.valueOf(update.get("cmdUpdateType").asText());
        if (updateType == CmdUpdateType.NOTIFICATIONS) {
            lastDataUpdate = JacksonUtil.treeToValue(update, UnreadNotificationsUpdate.class);
            unreadCount = lastDataUpdate.getTotalUnreadCount();
            if (lastDataUpdate.getNotifications() != null) {
                notifications = new ArrayList<>(lastDataUpdate.getNotifications());
            } else {
                Notification notificationUpdate = lastDataUpdate.getUpdate();
                boolean updated = false;
                for (int i = 0; i < notifications.size(); i++) {
                    Notification existing = notifications.get(i);
                    if (existing.getId().equals(notificationUpdate.getId())) {
                        notifications.set(i, notificationUpdate);
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    notifications.add(0, notificationUpdate);
                    if (notifications.size() > limit) {
                        notifications = notifications.subList(0, limit);
                    }
                }
            }
        } else if (updateType == CmdUpdateType.NOTIFICATIONS_COUNT) {
            lastCountUpdate = JacksonUtil.treeToValue(update, UnreadNotificationsCountUpdate.class);
            unreadCount = lastCountUpdate.getTotalUnreadCount();
        }
        super.onMessage(s);
    }

    /**
     * 功能：执行 `newCmdId` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    private static int newCmdId() {
        return RandomUtils.nextInt(1, 1000);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationApiWsClient` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
