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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.util.TbStopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`BaseHealthChecker` 是 ThingsBoard Monitoring 模块 中的监控服务与健康检查类型，用于编排周期性探测、延迟统计、异常捕获、恢复判断和报告输出。
 * 2. 所属模块：位于 monitoring 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 生命周期：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Template Method / Strategy。
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseHealthChecker<C extends MonitoringConfig, T extends MonitoringTarget> {

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Getter
    protected final C config;
    /**
     * 目标对象，表示当前对象的对应属性。
     */
    @Getter
    protected final T target;

    /**
     * 信息对象，表示当前对象的对应属性。
     */
    private Object info;

    /**
     * `reporter` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private MonitoringReporter reporter;
    /**
     * 耗时统计器，表示当前对象的对应属性。
     */
    @Autowired
    private TbStopWatch stopWatch;
    /**
     * 结果检查超时时间，用于控制时间范围或等待时长。
     */
    @Value("${monitoring.check_timeout_ms}")
    private int resultCheckTimeoutMs;

    @Getter
    private final Map<String, BaseHealthChecker<C, T>> associates = new HashMap<>();

    /**
     * 遥测常量，用于统一引用固定值。
     */
    public static final String TEST_TELEMETRY_KEY = "testData";

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        info = getInfo();
    }

    /**
     * 功能：执行 `initialize` 对应的处理。
     * 参数：
     * - `tbClient`：客户端对象。
     * 返回：无。
     */
    protected abstract void initialize(TbClient tbClient);

    /**
     * 功能：执行 `check` 对应的处理。
     * 参数：
     * - `wsClient`：客户端对象。
     * 返回：无。
     */
    public final void check(WsClient wsClient) {
        log.debug("[{}] Checking", info);
        try {
            wsClient.registerWaitForUpdate();

            String testValue = UUID.randomUUID().toString();
            String testPayload = createTestPayload(testValue);
            try {
                initClient();
                stopWatch.start();
                sendTestPayload(testPayload);
                reporter.reportLatency(Latencies.request(getKey()), stopWatch.getTime());
                log.trace("[{}] Sent test payload ({})", info, testPayload);
            } catch (Throwable e) {
                throw new ServiceFailureException(e);
            }

            log.trace("[{}] Waiting for WS update", info);
            checkWsUpdate(wsClient, testValue);

            reporter.serviceIsOk(info);
            reporter.serviceIsOk(MonitoredServiceKey.GENERAL);
        } catch (ServiceFailureException serviceFailureException) {
            reporter.serviceFailure(info, serviceFailureException);
        } catch (Exception e) {
            reporter.serviceFailure(MonitoredServiceKey.GENERAL, e);
        }

        associates.values().forEach(healthChecker -> {
            healthChecker.check(wsClient);
        });
    }

    /**
     * 功能：校验`Ws Update`。
     * 参数：
     * - `wsClient`：客户端对象。
     * - `testValue`：值。
     * 返回：无。
     */
    private void checkWsUpdate(WsClient wsClient, String testValue) {
        stopWatch.start();
        wsClient.waitForUpdate(resultCheckTimeoutMs);
        log.trace("[{}] Waited for WS update. Last WS msg: {}", info, wsClient.lastMsg);
        Object update = wsClient.getTelemetryUpdate(target.getDeviceId(), TEST_TELEMETRY_KEY);
        if (update == null) {
            throw new ServiceFailureException("No WS update arrived within " + resultCheckTimeoutMs + " ms");
        } else if (!update.toString().equals(testValue)) {
            throw new ServiceFailureException("Was expecting value " + testValue + " but got " + update);
        }
        reporter.reportLatency(Latencies.wsUpdate(getKey()), stopWatch.getTime());
    }

    /**
     * 功能：初始化或启动客户端。
     * 参数：无。
     * 返回：无。
     */
    protected abstract void initClient() throws Exception;

    /**
     * 功能：保存或创建消息载荷。
     * 参数：
     * - `testValue`：值。
     * 返回：文本结果。
     */
    protected abstract String createTestPayload(String testValue);

    /**
     * 功能：发送或提交消息载荷。
     * 参数：
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    protected abstract void sendTestPayload(String payload) throws Exception;

    /**
     * 功能：停止或关闭客户端。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    protected abstract void destroyClient() throws Exception;

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract Object getInfo();
    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getKey();

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseHealthChecker` 在 ThingsBoard Monitoring 模块 中承担监控服务与健康检查类型职责，核心目的是编排周期性探测、延迟统计、异常捕获、恢复判断和报告输出。
 * 2. 核心流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
 * 3. 关键依赖：主要依赖或协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
