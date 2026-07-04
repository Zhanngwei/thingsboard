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
package org.thingsboard.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.monitoring.service.BaseMonitoringService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`ThingsboardMonitoringApplication` 是 ThingsBoard Monitoring 模块 中的健康探测与通知类型，用于对 ThingsBoard 服务、传输协议和 WebSocket/REST 链路执行健康检查、延迟采集与告警通知。
 * 2. 所属模块：位于 monitoring 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 生命周期：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Template Method / Strategy / Observer。
 */
public class ThingsboardMonitoringApplication {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `monitoringServices` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private List<BaseMonitoringService<?, ?>> monitoringServices;

    @Value("${monitoring.monitoring_rate_ms}")
    /**
     * 字段说明：
     * 1. 保存 `monitoringRateMs` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private int monitoringRateMs;

    /**
     * 方法说明：
     * 1. 职责：执行 `main` 对应的健康探测与通知类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(ThingsboardMonitoringApplication.class)
                .properties(Map.of("spring.config.name", "tb-monitoring"))
                .run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    /**
     * 方法说明：
     * 1. 职责：执行 `startMonitoring` 对应的健康探测与通知类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void startMonitoring() {
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("monitoring-executor"));
        scheduler.scheduleWithFixedDelay(() -> {
            monitoringServices.forEach(monitoringService -> {
                monitoringService.runChecks();
            });
        }, 0, monitoringRateMs, TimeUnit.MILLISECONDS);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsboardMonitoringApplication` 在 ThingsBoard Monitoring 模块 中承担健康探测与通知类型职责，核心目的是对 ThingsBoard 服务、传输协议和 WebSocket/REST 链路执行健康检查、延迟采集与告警通知。
 * 2. 核心流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
 * 3. 关键依赖：主要依赖或协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
