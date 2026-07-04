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

@RequiredArgsConstructor
@Slf4j
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
public abstract class BaseHealthChecker<C extends MonitoringConfig, T extends MonitoringTarget> {

    @Getter
    /**
     * 字段说明：
     * 1. 保存 `config` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    protected final C config;
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `target` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    protected final T target;

    /**
     * 字段说明：
     * 1. 保存 `info` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private Object info;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `reporter` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private MonitoringReporter reporter;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `stopWatch` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private TbStopWatch stopWatch;
    @Value("${monitoring.check_timeout_ms}")
    /**
     * 字段说明：
     * 1. 保存 `resultCheckTimeoutMs` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private int resultCheckTimeoutMs;

    @Getter
    private final Map<String, BaseHealthChecker<C, T>> associates = new HashMap<>();

    /**
     * 字段说明：
     * 1. 保存 `TEST_TELEMETRY_KEY` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    public static final String TEST_TELEMETRY_KEY = "testData";

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    private void init() {
        info = getInfo();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initialize` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    protected abstract void initialize(TbClient tbClient);

    /**
     * 方法说明：
     * 1. 职责：执行 `check` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public final void check(WsClient wsClient) {
        log.debug("[{}] Checking", info);
        try {
            // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
            wsClient.registerWaitForUpdate();

            String testValue = UUID.randomUUID().toString();
            String testPayload = createTestPayload(testValue);
            try {
                initClient();
                stopWatch.start();
                sendTestPayload(testPayload);
                // 延迟和通知逻辑用于把运行时状态转化为可观测事件，避免探测结果只停留在日志中。
                reporter.reportLatency(Latencies.request(getKey()), stopWatch.getTime());
                log.trace("[{}] Sent test payload ({})", info, testPayload);
            // 异常在这里转换为统一失败路径，便于监控告警、测试断言或客户端回调处理。
            } catch (Throwable e) {
                throw new ServiceFailureException(e);
            }

            log.trace("[{}] Waiting for WS update", info);
            // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
            checkWsUpdate(wsClient, testValue);

            reporter.serviceIsOk(info);
            reporter.serviceIsOk(MonitoredServiceKey.GENERAL);
        // 异常在这里转换为统一失败路径，便于监控告警、测试断言或客户端回调处理。
        } catch (ServiceFailureException serviceFailureException) {
            reporter.serviceFailure(info, serviceFailureException);
        // 异常在这里转换为统一失败路径，便于监控告警、测试断言或客户端回调处理。
        } catch (Exception e) {
            reporter.serviceFailure(MonitoredServiceKey.GENERAL, e);
        }

        associates.values().forEach(healthChecker -> {
            // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
            healthChecker.check(wsClient);
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkWsUpdate` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    private void checkWsUpdate(WsClient wsClient, String testValue) {
        stopWatch.start();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        wsClient.waitForUpdate(resultCheckTimeoutMs);
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        log.trace("[{}] Waited for WS update. Last WS msg: {}", info, wsClient.lastMsg);
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        Object update = wsClient.getTelemetryUpdate(target.getDeviceId(), TEST_TELEMETRY_KEY);
        // 条件分支用于保护配置、连接状态、测试前置条件或协议状态机边界。
        if (update == null) {
            throw new ServiceFailureException("No WS update arrived within " + resultCheckTimeoutMs + " ms");
        // 条件分支用于保护配置、连接状态、测试前置条件或协议状态机边界。
        } else if (!update.toString().equals(testValue)) {
            throw new ServiceFailureException("Was expecting value " + testValue + " but got " + update);
        }
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        reporter.reportLatency(Latencies.wsUpdate(getKey()), stopWatch.getTime());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initClient` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    protected abstract void initClient() throws Exception;

    /**
     * 方法说明：
     * 1. 职责：执行 `createTestPayload` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    protected abstract String createTestPayload(String testValue);

    /**
     * 方法说明：
     * 1. 职责：执行 `sendTestPayload` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    protected abstract void sendTestPayload(String payload) throws Exception;

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `destroyClient` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    protected abstract void destroyClient() throws Exception;

    /**
     * 方法说明：
     * 1. 职责：执行 `getInfo` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    protected abstract Object getInfo();
    /**
     * 方法说明：
     * 1. 职责：执行 `getKey` 对应的监控服务与健康检查类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
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
