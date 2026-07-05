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
package org.thingsboard.server.transport.mqtt.mqttv3;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`MqttTestClient` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class MqttTestClient {

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final int TIMEOUT = 30; // seconds
    public static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(TIMEOUT);

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final MqttAsyncClient client;

    /**
     * 功能：更新回调。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    public void setCallback(MqttTestCallback callback) {
        client.setCallback(callback);
    }

    /**
     * 功能：创建 `MqttTestClient` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public MqttTestClient() throws MqttException {
        this.client = createClient();
    }

    /**
     * 功能：创建 `MqttTestClient` 实例，并初始化必要字段。
     * 参数：
     * - `clientId`：客户端ID。
     * 返回：新创建的对象实例。
     */
    public MqttTestClient(String clientId) throws MqttException {
        this.client = createClient(clientId);
    }

    /**
     * 功能：执行 `connectAndWait` 对应的处理。
     * 参数：
     * - `userName`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    public void connectAndWait(String userName, String password) throws MqttException {
        IMqttToken connect = connect(userName, password);
        connect.waitForCompletion(TIMEOUT_MS);
    }

    /**
     * 功能：执行 `connectAndWait` 对应的处理。
     * 参数：
     * - `userName`：名称。
     * 返回：无。
     */
    public void connectAndWait(String userName) throws MqttException {
        connectAndWait(userName, null);
    }

    /**
     * 功能：执行 `connectAndWait` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void connectAndWait() throws MqttException {
        connectAndWait(null, null);
    }

    /**
     * 功能：执行 `connect` 对应的处理。
     * 参数：
     * - `userName`：名称。
     * - `password`：`password` 参数。
     * 返回：处理结果。
     */
    private IMqttToken connect(String userName, String password) throws MqttException {
        if (client == null) {
            throw new RuntimeException("Failed to connect! MqttAsyncClient is not initialized!");
        }
        MqttConnectOptions options = new MqttConnectOptions();
        if (StringUtils.isNotEmpty(userName)) {
            options.setUserName(userName);
        }
        if (StringUtils.isNotEmpty(password)) {
            options.setPassword(password.toCharArray());
        }
        return client.connect(options);
    }

    /**
     * 功能：执行 `disconnectAndWait` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void disconnectAndWait() throws MqttException {
        disconnect().waitForCompletion(TIMEOUT_MS);
    }

    /**
     * 功能：执行 `disconnect` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public IMqttToken disconnect() throws MqttException {
        return client.disconnect();
    }

    /**
     * 功能：执行 `disconnectForcibly` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void disconnectForcibly() throws MqttException {
        client.disconnectForcibly(TIMEOUT_MS);
    }

    /**
     * 功能：发送或提交`And Wait`。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    public void publishAndWait(String topic, byte[] payload) throws MqttException {
        publish(topic, payload).waitForCompletion(TIMEOUT_MS);
    }

    /**
     * 功能：执行 `publish` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `payload`：`payload` 参数。
     * 返回：处理结果。
     */
    public IMqttDeliveryToken publish(String topic, byte[] payload) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        return client.publish(topic, message);
    }

    /**
     * 功能：订阅`And Wait`。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `qoS`：`qoS` 参数。
     * 返回：无。
     */
    public void subscribeAndWait(String topic, MqttQoS qoS) throws MqttException {
        subscribe(topic, qoS).waitForCompletion(TIMEOUT_MS);
    }

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `qoS`：`qoS` 参数。
     * 返回：处理结果。
     */
    public IMqttToken subscribe(String topic, MqttQoS qoS) throws MqttException {
        return client.subscribe(topic, qoS.value());
    }

    /**
     * 功能：判断`Connected`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * 功能：执行 `enableManualAcks` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void enableManualAcks() {
        client.setManualAcks(true);
    }

    /**
     * 功能：执行 `messageArrivedComplete` 对应的处理。
     * 参数：
     * - `mqttMessage`：待处理消息。
     * 返回：无。
     */
    public void messageArrivedComplete(MqttMessage mqttMessage) throws MqttException {
        client.messageArrivedComplete(mqttMessage.getId(), mqttMessage.getQos());
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    private MqttAsyncClient createClient() throws MqttException {
        return createClient(null);
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `clientId`：客户端ID。
     * 返回：处理结果。
     */
    private MqttAsyncClient createClient(String clientId) throws MqttException {
        if (StringUtils.isEmpty(clientId)) {
            clientId = MqttAsyncClient.generateClientId();
        }
        return new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`MqttTestClient` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
