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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;

/**
 * 中文说明：
 * 1. 类目的：`TestProperties` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
@Slf4j
public class TestProperties {

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    private static final String HTTPS_URL = "https://localhost";

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    private static final String WSS_URL = "wss://localhost";

    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();

    /**
     * 功能：获取基础访问地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getBaseUrl() {
        if (instance.isActive()) {
            return HTTPS_URL;
        }
        return System.getProperty("tb.baseUrl", "http://localhost:8080");
    }

    /**
     * 功能：获取基础访问地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getBaseUiUrl() {
        if (instance.isActive()) {
            //return "https://host.docker.internal"; // this alternative requires docker-selenium.yml extra_hosts: - "host.docker.internal:host-gateway"
            //return "https://" + DockerClientFactory.instance().dockerHostIpAddress(); //this alternative will get Docker IP from testcontainers
            return "https://haproxy"; //communicate inside current docker-compose network to the load balancer container
        }
        return System.getProperty("tb.baseUiUrl", "http://localhost:8080");
    }

    /**
     * 功能：获取URL 地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getWebSocketUrl() {
        if (instance.isActive()) {
            return WSS_URL;
        }
        return System.getProperty("tb.wsUrl", "ws://localhost:8080");
    }

    /**
     * 功能：获取URL 地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getMqttBrokerUrl() {
        if (instance.isActive()) {
            String host = instance.getTestContainer().getServiceHost("broker", 1883);
            Integer port = instance.getTestContainer().getServicePort("broker", 1883);
            return "tcp://" + host + ":" + port;
        }
        return System.getProperty("mqtt.broker", "tcp://localhost:1883");
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TestProperties` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
