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
package org.thingsboard.monitoring.notification.channels.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.monitoring.notification.channels.NotificationChannel;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;

/**
 * 中文说明：
 * 1. `SlackNotificationChannel` 是 ThingsBoard Monitoring 中处理通知的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `NotificationChannel`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@ConditionalOnProperty(value = "monitoring.notifications.slack.enabled", havingValue = "true")
@Slf4j
public class SlackNotificationChannel implements NotificationChannel {

    /**
     * Webhook 地址，用于定位外部资源或本地资源。
     */
    @Value("${monitoring.notifications.slack.webhook_url}")
    private String webhookUrl;

    /**
     * HTTP 客户端，用于支撑当前网络或外部服务交互。
     */
    private RestTemplate restTemplate;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
    }

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `message`：待处理消息。
     * 返回：无。
     */
    @Override
    public void sendNotification(String message) {
        restTemplate.postForObject(webhookUrl, Map.of("text", message), String.class);
    }

}
