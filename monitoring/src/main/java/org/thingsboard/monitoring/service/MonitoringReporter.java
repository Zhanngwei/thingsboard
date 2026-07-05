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
package org.thingsboard.monitoring.service;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.data.Latency;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.notification.HighLatencyNotification;
import org.thingsboard.monitoring.data.notification.ServiceFailureNotification;
import org.thingsboard.monitoring.data.notification.ServiceRecoveryNotification;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`MonitoringReporter` 是 ThingsBoard Monitoring 模块 中的监控服务与健康检查类型，用于编排周期性探测、延迟统计、异常捕获、恢复判断和报告输出。
 * 2. 所属模块：位于 monitoring 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 生命周期：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Template Method / Strategy。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringReporter {

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationService notificationService;

    private final Map<String, Latency> latencies = new ConcurrentHashMap<>();
    private final Map<Object, AtomicInteger> failuresCounters = new ConcurrentHashMap<>();

    /**
     * 失败次数阈值，用于判断是否达到处理条件。
     */
    @Value("${monitoring.failures_threshold}")
    private int failuresThreshold;
    /**
     * 通知，表示当前对象的对应属性。
     */
    @Value("${monitoring.repeated_failure_notification}")
    private int repeatedFailureNotification;

    /**
     * 是否启用延迟上报。
     */
    @Value("${monitoring.latency.enabled}")
    private boolean latencyReportingEnabled;
    /**
     * 延迟阈值，用于判断是否达到处理条件。
     */
    @Value("${monitoring.latency.threshold_ms}")
    private int latencyThresholdMs;
    /**
     * 资产ID，用于定位对应业务对象。
     */
    @Value("${monitoring.latency.reporting_asset_id}")
    private String reportingAssetId;

    /**
     * 功能：上报延迟数据。
     * 参数：
     * - `tbClient`：客户端对象。
     * 返回：无。
     */
    public void reportLatencies(TbClient tbClient) {
        if (latencies.isEmpty()) {
            return;
        }
        log.debug("Latencies:\n{}", latencies.values().stream().map(latency -> latency.getKey() + ": " + latency.getFormattedValue())
                .collect(Collectors.joining("\n")) + "\n");
        if (!latencyReportingEnabled) return;

        List<Latency> highLatencies = latencies.values().stream()
                .filter(latency -> latency.getValue() >= (double) latencyThresholdMs)
                .collect(Collectors.toList());
        if (!highLatencies.isEmpty()) {
            HighLatencyNotification highLatencyNotification = new HighLatencyNotification(highLatencies, latencyThresholdMs);
            notificationService.sendNotification(highLatencyNotification);
            log.warn("{}", highLatencyNotification.getText());
        }

        try {
            if (StringUtils.isBlank(reportingAssetId)) {
                String assetName = "[Monitoring] Latencies";
                Asset monitoringAsset = tbClient.findAsset(assetName).orElseGet(() -> {
                    Asset asset = new Asset();
                    asset.setType("Monitoring");
                    asset.setName(assetName);
                    asset = tbClient.saveAsset(asset);
                    log.info("Created monitoring asset {}", asset.getId());
                    return asset;
                });
                reportingAssetId = monitoringAsset.getId().toString();
            }

            ObjectNode msg = JacksonUtil.newObjectNode();
            latencies.values().forEach(latency -> {
                msg.set(latency.getKey(), new DoubleNode(latency.getValue()));
            });
            tbClient.saveEntityTelemetry(new AssetId(UUID.fromString(reportingAssetId)), "time", msg);
            latencies.clear();
        } catch (Exception e) {
            log.error("Failed to report latencies: {}", e.getMessage());
        }
    }

    /**
     * 功能：上报延迟。
     * 参数：
     * - `key`：键。
     * - `latencyInNanos`：`latencyInNanos` 参数。
     * 返回：无。
     */
    public void reportLatency(String key, long latencyInNanos) {
        String latencyKey = key + "Latency";
        double latencyInMs = (double) latencyInNanos / 1000_000;
        log.trace("Reporting latency [{}]: {} ms", key, latencyInMs);
        latencies.put(latencyKey, Latency.of(latencyKey, latencyInMs));
    }

    /**
     * 功能：执行 `serviceFailure` 对应的处理。
     * 参数：
     * - `serviceKey`：服务对象。
     * - `error`：错误信息。
     * 返回：无。
     */
    public void serviceFailure(Object serviceKey, Throwable error) {
        if (log.isDebugEnabled()) {
            log.error("Error occurred", error);
        }
        int failuresCount = failuresCounters.computeIfAbsent(serviceKey, k -> new AtomicInteger()).incrementAndGet();
        ServiceFailureNotification notification = new ServiceFailureNotification(serviceKey, error, failuresCount);
        log.error(notification.getText());
        if (failuresCount == failuresThreshold || (repeatedFailureNotification != 0 && failuresCount % repeatedFailureNotification == 0)) {
            notificationService.sendNotification(notification);
        }
    }

    /**
     * 功能：执行 `serviceIsOk` 对应的处理。
     * 参数：
     * - `serviceKey`：服务对象。
     * 返回：无。
     */
    public void serviceIsOk(Object serviceKey) {
        ServiceRecoveryNotification notification = new ServiceRecoveryNotification(serviceKey);
        if (!serviceKey.equals(MonitoredServiceKey.GENERAL)) {
            log.info(notification.getText());
        }
        AtomicInteger failuresCounter = failuresCounters.get(serviceKey);
        if (failuresCounter != null) {
            if (failuresCounter.get() >= failuresThreshold) {
                notificationService.sendNotification(notification);
            }
            failuresCounter.set(0);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`MonitoringReporter` 在 ThingsBoard Monitoring 模块 中承担监控服务与健康检查类型职责，核心目的是编排周期性探测、延迟统计、异常捕获、恢复判断和报告输出。
 * 2. 核心流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
 * 3. 关键依赖：主要依赖或协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
