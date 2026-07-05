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
 * 1. 类目的：`SlackNotificationChannel` 是 ThingsBoard Monitoring 模块 中的通知通道类型，用于把服务失败、恢复或高延迟事件转换为外部通知消息。
 * 2. 所属模块：位于 monitoring 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 生命周期：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Observer / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`SlackNotificationChannel` 在 ThingsBoard Monitoring 模块 中承担通知通道类型职责，核心目的是把服务失败、恢复或高延迟事件转换为外部通知消息。
 * 2. 核心流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
 * 3. 关键依赖：主要依赖或协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
