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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.util.TbStopWatch;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `BaseMonitoringService` 是 ThingsBoard Monitoring 中负责 `Monitoring` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `MonitoringConfig`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
public abstract class BaseMonitoringService<C extends MonitoringConfig<T>, T extends MonitoringTarget> {

    /**
     * `configs`列表，用于保存一组待处理对象。
     */
    @Autowired(required = false)
    private List<C> configs;
    private final List<BaseHealthChecker<C, T>> healthCheckers = new LinkedList<>();
    private final List<UUID> devices = new LinkedList<>();

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Autowired
    private TbClient tbClient;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Autowired
    private WsClientFactory wsClientFactory;
    /**
     * 耗时统计器，表示当前对象的对应属性。
     */
    @Autowired
    private TbStopWatch stopWatch;
    /**
     * `reporter` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private MonitoringReporter reporter;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        tbClient.logIn();
        configs.forEach(config -> {
            config.getTargets().forEach(target -> {
                BaseHealthChecker<C, T> healthChecker = initHealthChecker(target, config);
                healthCheckers.add(healthChecker);

                if (target.isCheckDomainIps()) {
                    getAssociatedUrls(target.getBaseUrl()).forEach(url -> {
                        healthChecker.getAssociates().put(url, initHealthChecker(createTarget(url), config));
                    });
                }
            });
        });
    }

    /**
     * 功能：初始化或启动`Health Checker`。
     * 参数：
     * - `target`：`target` 参数。
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    private BaseHealthChecker<C, T> initHealthChecker(T target, C config) {
        BaseHealthChecker<C, T> healthChecker = (BaseHealthChecker<C, T>) createHealthChecker(config, target);
        log.info("Initializing {} for {}", healthChecker.getClass().getSimpleName(), target.getBaseUrl());
        healthChecker.initialize(tbClient);
        devices.add(target.getDeviceId());
        return healthChecker;
    }

    /**
     * 功能：执行`Checks`。
     * 参数：无。
     * 返回：无。
     */
    public final void runChecks() {
        if (healthCheckers.isEmpty()) {
            return;
        }
        try {
            log.info("Starting {}", getName());
            stopWatch.start();
            String accessToken = tbClient.logIn();
            reporter.reportLatency(Latencies.LOG_IN, stopWatch.getTime());

            try (WsClient wsClient = wsClientFactory.createClient(accessToken)) {
                wsClient.subscribeForTelemetry(devices, TransportHealthChecker.TEST_TELEMETRY_KEY).waitForReply();
                for (BaseHealthChecker<C, T> healthChecker : healthCheckers) {
                    check(healthChecker, wsClient);
                }
            }
            reporter.reportLatencies(tbClient);
            log.debug("Finished {}", getName());
        } catch (Throwable error) {
            try {
                reporter.serviceFailure(MonitoredServiceKey.GENERAL, error);
            } catch (Throwable reportError) {
                log.error("Error occurred during service failure reporting", reportError);
            }
        }
    }

    /**
     * 功能：执行 `check` 对应的处理。
     * 参数：
     * - `healthChecker`：`healthChecker` 参数。
     * - `wsClient`：客户端对象。
     * 返回：无。
     */
    private void check(BaseHealthChecker<C, T> healthChecker, WsClient wsClient) throws Exception {
        healthChecker.check(wsClient);

        T target = healthChecker.getTarget();
        if (target.isCheckDomainIps()) {
            Set<String> associatedUrls = getAssociatedUrls(target.getBaseUrl());
            Map<String, BaseHealthChecker<C, T>> associates = healthChecker.getAssociates();
            Set<String> prevAssociatedUrls = new HashSet<>(associates.keySet());

            boolean changed = false;
            for (String url : associatedUrls) {
                if (!prevAssociatedUrls.contains(url)) {
                    BaseHealthChecker<C, T> associate = initHealthChecker(createTarget(url), healthChecker.getConfig());
                    associates.put(url, associate);
                    changed = true;
                }
            }
            for (String url : prevAssociatedUrls) {
                if (!associatedUrls.contains(url)) {
                    stopHealthChecker(healthChecker);
                    associates.remove(url);
                    changed = true;
                }
            }
            if (changed) {
                log.info("Updated IPs for {}: {} (old list: {})", target.getBaseUrl(), associatedUrls, prevAssociatedUrls);
            }
        }
    }

    /**
     * 功能：获取`Associated Urls`。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * 返回：匹配的数据集合。
     */
    @SneakyThrows
    private Set<String> getAssociatedUrls(String baseUrl) {
        URI url = new URI(baseUrl);
        return Arrays.stream(InetAddress.getAllByName(url.getHost()))
                .map(InetAddress::getHostAddress)
                .map(ip -> {
                    try {
                        return new URI(url.getScheme(), null, ip, url.getPort(), "", null, null).toString();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }

    /**
     * 功能：停止或关闭`Health Checker`。
     * 参数：
     * - `healthChecker`：`healthChecker` 参数。
     * 返回：无。
     */
    private void stopHealthChecker(BaseHealthChecker<C, T> healthChecker) throws Exception {
        healthChecker.destroyClient();
        devices.remove(healthChecker.getTarget().getDeviceId());
        log.info("Stopped {} for {}", healthChecker.getClass().getSimpleName(), healthChecker.getTarget().getBaseUrl());
    }

    /**
     * 功能：保存或创建`Health Checker`。
     * 参数：
     * - `config`：配置对象。
     * - `target`：`target` 参数。
     * 返回：处理结果。
     */
    protected abstract BaseHealthChecker<?, ?> createHealthChecker(C config, T target);

    /**
     * 功能：保存或创建目标对象。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * 返回：处理结果。
     */
    protected abstract T createTarget(String baseUrl);

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getName();

}
