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
 * 1. `MqttTestClient` 是 ThingsBoard Application 中访问 MQTT 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 它直接协作于网络客户端、认证模型和请求响应对象。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
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
