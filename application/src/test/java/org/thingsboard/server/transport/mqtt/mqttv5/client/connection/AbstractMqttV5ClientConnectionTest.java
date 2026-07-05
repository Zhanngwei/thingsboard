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
package org.thingsboard.server.transport.mqtt.mqttv5.client.connection;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.concurrent.TimeUnit;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttV5ClientConnectionTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractMqttV5ClientConnectionTest extends AbstractMqttIntegrationTest {

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        IMqttToken connectionResult = client.connectAndWait(accessToken);
        MqttWireMessage response = connectionResult.getResponse();

        Assert.assertEquals(MESSAGE_TYPE_CONNACK, response.getType());

        MqttConnAck connAckMsg = (MqttConnAck) response;

        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());
        client.disconnect();
    }

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithWrongAccessTokenTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        try {
            client.connectAndWait("wrongAccessToken");
        } catch (MqttException e) {
            Assert.assertEquals(MqttReturnCode.RETURN_CODE_BAD_USERNAME_OR_PASSWORD, e.getReasonCode());
        }
    }

    /**
     * 功能：处理用户名。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithWrongClientIdAndEmptyUsernamePasswordTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient("unknownClientId");
        try {
            client.connectAndWait();
        } catch (MqttException e) {
            Assert.assertEquals(MqttReturnCode.RETURN_CODE_IDENTIFIER_NOT_VALID, e.getReasonCode());
        }
    }

    /**
     * 功能：处理凭据。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithNoCredentialsTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient(false);
        try {
            client.connectAndWait();
        } catch (MqttException e) {
            Assert.assertEquals(MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED, e.getReasonCode());
        }
    }

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithPacketSizeLimitationTest() throws Exception {
        int packetSizeLimit = 99;
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setMaximumPacketSize((long) packetSizeLimit);
        options.setUserName(accessToken);

        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(options);

        MqttV5TestCallback possibleSizeCallback = updateAttributeWithStringValue(client, packetSizeLimit / 2);

        Assert.assertTrue("Server should send messages if size less then limitation.", possibleSizeCallback.getPayloadBytes().length < packetSizeLimit);

        MqttV5TestCallback bigMessageCallback = updateAttributeWithStringValue(client, packetSizeLimit * 2);

        Assert.assertNull("Server should not send a message if the message size bigger then set limit.", bigMessageCallback.getLastReceivedMessage());

        client.disconnect();

    }

    /**
     * 功能：更新属性。
     * 参数：
     * - `client`：客户端对象。
     * - `valueLen`：值。
     * 返回：处理结果。
     */
    private MqttV5TestCallback updateAttributeWithStringValue(MqttV5TestClient client, int valueLen) throws Exception {
        MqttV5TestCallback onUpdateCallback = new MqttV5TestCallback();
        client.setCallback(onUpdateCallback);
        client.subscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.ATTRIBUTES, 1);

        String payload = "{\"sharedStr\":\"" + StringUtils.repeat("*", valueLen) + "\"}";

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", payload, String.class, status().isOk());
        onUpdateCallback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        return onUpdateCallback;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttV5ClientConnectionTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
