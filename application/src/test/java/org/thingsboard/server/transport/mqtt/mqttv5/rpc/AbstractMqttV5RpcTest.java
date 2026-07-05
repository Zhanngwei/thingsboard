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
package org.thingsboard.server.transport.mqtt.mqttv5.rpc;

import com.nimbusds.jose.util.StandardCharset;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.mqttv5.AbstractMqttV5Test;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC;

/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttV5RpcTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public abstract class AbstractMqttV5RpcTest extends AbstractMqttV5Test {

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";


    /**
     * 功能：处理RPC。
     * 参数：无。
     * 返回：无。
     */
    protected void processOneWayRpcTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        MqttV5TestCallback callback = new MqttV5TestCallback(DEVICE_RPC_REQUESTS_SUB_TOPIC.replace("+", "0"));
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.RPC, 1);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String result = doPostAsync("/api/rpc/oneway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        assertTrue(StringUtils.isEmpty(result));
        callback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：无。
     * 返回：无。
     */
    protected void processJsonTwoWayRpcTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        client.subscribeAndWait(DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_LEAST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.RPC, 1);
        MqttV5TestRpcCallback callback = new MqttV5TestRpcCallback(client, DEVICE_RPC_REQUESTS_SUB_TOPIC.replace("+", "0"));
        client.setCallback(callback);
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", actualRpcResponse);
        client.disconnect();
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttV5TestRpcCallback` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    protected class MqttV5TestRpcCallback extends MqttV5TestCallback {

        /**
         * 客户端，用于发起外部调用或协议交互。
         */
        private final MqttV5TestClient client;

        /**
         * 功能：创建 `AbstractMqttV5RpcTest` 实例，并初始化必要字段。
         * 参数：
         * - `client`：客户端对象。
         * - `awaitSubTopic`：主题名称或主题对象。
         * 返回：新创建的对象实例。
         */
        public MqttV5TestRpcCallback(MqttV5TestClient client, String awaitSubTopic) {
            super(awaitSubTopic);
            this.client = client;
        }

        /**
         * 功能：执行 `messageArrivedOnAwaitSubTopic` 对应的处理。
         * 参数：
         * - `requestTopic`：请求对象。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        @Override
        protected void messageArrivedOnAwaitSubTopic(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            if (awaitSubTopic.equals(requestTopic)) {
                qoS = mqttMessage.getQos();
                payloadBytes = mqttMessage.getPayload();
                String responseTopic = requestTopic.replace("request", "response");
                try {
                    client.publish(responseTopic, DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8));
                } catch (MqttException e) {
                    log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
                }
                subscribeLatch.countDown();
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttV5RpcTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
