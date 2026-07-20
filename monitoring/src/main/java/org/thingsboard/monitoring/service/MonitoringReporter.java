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
 * 1. `MonitoringReporter` 是 ThingsBoard Monitoring 中围绕 `Monitoring Reporter` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
