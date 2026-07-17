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
package org.thingsboard.monitoring.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.notification.Notification;
import org.thingsboard.monitoring.notification.channels.NotificationChannel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. `NotificationService` 是 ThingsBoard Monitoring 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    /**
     * 通知列表，用于保存一组待处理对象。
     */
    private final List<NotificationChannel> notificationChannels;
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Value("${monitoring.notifications.message_prefix}")
    private String messagePrefix;

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `notification`：`notification` 参数。
     * 返回：无。
     */
    public void sendNotification(Notification notification) {
        String message;
        if (StringUtils.isEmpty(messagePrefix)) {
            message = notification.getText();
        } else {
            message = messagePrefix + System.lineSeparator() + notification.getText();
        }
        notificationChannels.forEach(notificationChannel -> {
            notificationExecutor.submit(() -> {
                try {
                    notificationChannel.sendNotification(message);
                } catch (Exception e) {
                    log.error("Failed to send notification to {}", notificationChannel.getClass().getSimpleName(), e);
                }
            });
        });
    }

}
