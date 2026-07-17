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
 * 1. `BaseHealthChecker` 是 ThingsBoard Monitoring 中围绕 `Health Checker` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `MonitoringConfig`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
