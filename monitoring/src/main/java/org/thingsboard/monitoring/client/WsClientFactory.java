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
package org.thingsboard.monitoring.client;

import lombok.RequiredArgsConstructor;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`WsClientFactory` 是 ThingsBoard Monitoring 模块 中的监控客户端适配类型，用于封装 REST、WebSocket 或 LwM2M 客户端连接、认证、订阅和消息收发边界。
 * 2. 所属模块：位于 monitoring 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 生命周期：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Adapter / Client。
 */
@Component
@RequiredArgsConstructor
public class WsClientFactory {

    /**
     * 监控报告器，表示当前对象的对应属性。
     */
    private final MonitoringReporter monitoringReporter;
    private final TbStopWatch stopWatch;
    /**
     * 基础访问地址，用于定位外部资源或本地资源。
     */
    @Value("${monitoring.ws.base_url}")
    private String baseUrl;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${monitoring.ws.request_timeout_ms}")
    private int requestTimeoutMs;

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    public WsClient createClient(String accessToken) throws Exception {
        URI uri = new URI(baseUrl + "/api/ws/plugins/telemetry?token=" + accessToken);
        stopWatch.start();
        WsClient wsClient = new WsClient(uri, requestTimeoutMs);
        if (baseUrl.startsWith("wss")) {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
        }
        boolean connected = wsClient.connectBlocking(requestTimeoutMs, TimeUnit.MILLISECONDS);
        if (!connected) {
            throw new IllegalStateException("Failed to establish WS session");
        }
        monitoringReporter.reportLatency(Latencies.WS_CONNECT, stopWatch.getTime());
        return wsClient;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`WsClientFactory` 在 ThingsBoard Monitoring 模块 中承担监控客户端适配类型职责，核心目的是封装 REST、WebSocket 或 LwM2M 客户端连接、认证、订阅和消息收发边界。
 * 2. 核心流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
 * 3. 关键依赖：主要依赖或协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
