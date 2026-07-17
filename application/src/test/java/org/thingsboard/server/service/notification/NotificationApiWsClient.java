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
 * 1. `NotificationApiWsClient` 是 ThingsBoard Application 中访问通知的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 直接依赖的类型边界包括 `TbTestWebSocketClient`。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
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
