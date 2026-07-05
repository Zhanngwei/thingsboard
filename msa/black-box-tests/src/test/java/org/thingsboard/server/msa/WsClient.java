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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`WsClient` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
@Slf4j
public class WsClient extends WebSocketClient {
    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private WsTelemetryResponse message;

    /**
     * 是否满足`firstReplyReceived`条件。
     */
    private volatile boolean firstReplyReceived;
    private final CountDownLatch firstReply = new CountDownLatch(1);
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private final long timeoutMultiplier;

    /**
     * 功能：创建 `WsClient` 实例，并初始化必要字段。
     * 参数：
     * - `serverUri`：`serverUri` 参数。
     * - `timeoutMultiplier`：`timeoutMultiplier` 参数。
     * 返回：新创建的对象实例。
     */
    WsClient(URI serverUri, long timeoutMultiplier) {
        super(serverUri);
        this.timeoutMultiplier = timeoutMultiplier;
    }

    /**
     * 功能：处理`on Open`。
     * 参数：
     * - `serverHandshake`：`serverHandshake` 参数。
     * 返回：无。
     */
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `message`：待处理消息。
     * 返回：无。
     */
    @Override
    public synchronized void onMessage(String message) {
        log.error("WS onMessage: {}", message);
        if (!firstReplyReceived) {
            firstReplyReceived = true;
            firstReply.countDown();
        } else {
            try {
                WsTelemetryResponse response = mapper.readValue(message, WsTelemetryResponse.class);
                if (!response.getData().isEmpty()) {
                    this.message = response;
                    latch.countDown();
                }
            } catch (IOException e) {
                log.error("ws message can't be read");
            }
        }
    }

    /**
     * 功能：处理`on Close`。
     * 参数：
     * - `code`：`code` 参数。
     * - `reason`：`reason` 参数。
     * - `remote`：`remote` 参数。
     * 返回：无。
     */
    @Override
    public synchronized void onClose(int code, String reason, boolean remote) {
        log.error("WS onClose: [{}]", reason);
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `ex`：`ex` 参数。
     * 返回：无。
     */
    @Override
    public synchronized void onError(Exception ex) {
        log.error("WS onError: ", ex);
        ex.printStackTrace();
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    public WsTelemetryResponse getLastMessage() {
        try {
            boolean result = latch.await(10 * timeoutMultiplier, TimeUnit.SECONDS);
            if (result) {
                return this.message;
            } else {
                log.error("Timeout, ws message wasn't received");
                throw new RuntimeException("Timeout, ws message wasn't received");
            }
        } catch (InterruptedException e) {
            log.error("Timeout, ws message wasn't received");
        }
        return null;
    }

    /**
     * 功能：执行 `waitForFirstReply` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void waitForFirstReply() {
        try {
            boolean result = firstReply.await(10 * timeoutMultiplier, TimeUnit.SECONDS);
            if (!result) {
                log.error("Timeout, ws message wasn't received");
                throw new RuntimeException("Timeout, ws message wasn't received");
            }
        } catch (InterruptedException e) {
            log.error("Timeout, ws message wasn't received");
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：处理SSL。
     * 参数：
     * - `sslParameters`：`sslParameters` 参数。
     * 返回：无。
     */
    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`WsClient` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
