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
package org.thingsboard.server.transport.mqtt.mqttv3.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nimbusds.jose.util.StandardCharset;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestSubscribeOnTopicCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.BASE_DEVICE_API_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.BASE_DEVICE_API_TOPIC_V2;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_RPC_TOPIC;

/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttServerSideRpcIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public abstract class AbstractMqttServerSideRpcIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  optional string method = 1;\n" +
            "  optional int32 requestId = 2;\n" +
            "  Params params = 3;\n" +
            "\n" +
            "  message Params {\n" +
            "      optional string pin = 1;\n" +
            "      optional int32 value = 2;\n" +
            "   }\n" +
            "}";

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    /**
     * RPC常量，用于统一引用固定值。
     */
    protected static final Long asyncContextTimeoutToUseRpcPlugin = 10000L;

    /**
     * 功能：处理RPC。
     * 参数：
     * - `rpcSubTopic`：主题名称或主题对象。
     * 返回：无。
     */
    protected void processOneWayRpcTest(String rpcSubTopic) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(rpcSubTopic.replace("+", "0"));
        client.setCallback(callback);
        subscribeAndWait(client, rpcSubTopic, savedDevice.getId(), FeatureType.RPC);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String result = doPostAsync("/api/rpc/oneway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        assertTrue(StringUtils.isEmpty(result));
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        DeviceTransportType deviceTransportType = deviceProfile.getTransportType();
        if (deviceTransportType.equals(DeviceTransportType.MQTT)) {
            DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
            assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
            MqttDeviceProfileTransportConfiguration configuration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            TransportPayloadType transportPayloadType = configuration.getTransportPayloadTypeConfiguration().getTransportPayloadType();
            if (transportPayloadType.equals(TransportPayloadType.PROTOBUF)) {
                // TODO: add correct validation of proto requests to device
                assertTrue(callback.getPayloadBytes().length > 0);
            } else {
                assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
            }
        } else {
            assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        }
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processJsonOneWayRpcTestGateway(String deviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        String payload = "{\"device\":\"" + deviceName + "\"}";
        byte[] payloadBytes = payload.getBytes();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `rpcSubTopic`：主题名称或主题对象。
     * 返回：无。
     */
    protected void processJsonTwoWayRpcTest(String rpcSubTopic) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        subscribeAndWait(client, rpcSubTopic, savedDevice.getId(), FeatureType.RPC);
        MqttTestRpcJsonCallback callback = new MqttTestRpcJsonCallback(client, rpcSubTopic.replace("+", "0"));
        client.setCallback(callback);
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", actualRpcResponse);
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `rpcSubTopic`：主题名称或主题对象。
     * 返回：无。
     */
    protected void processProtoTwoWayRpcTest(String rpcSubTopic) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        subscribeAndWait(client, rpcSubTopic, savedDevice.getId(), FeatureType.RPC);

        MqttTestRpcProtoCallback callback = new MqttTestRpcProtoCallback(client, rpcSubTopic.replace("+", "0"));
        client.setCallback(callback);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // TODO: add correct validation of proto requests to device
        assertTrue(callback.getPayloadBytes().length > 0);
        assertEquals("{\"payload\":\"{\\\"value1\\\":\\\"A\\\",\\\"value2\\\":\\\"B\\\"}\"}", actualRpcResponse);
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processProtoTwoWayRpcTestGateway(String deviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        byte[] payloadBytes = connectMsgProto.toByteArray();
        validateProtoTwoWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processProtoOneWayRpcTestGateway(String deviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        byte[] payloadBytes = connectMsgProto.toByteArray();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    /**
     * 功能：获取Protobuf。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private TransportApiProtos.ConnectMsg getConnectProto(String deviceName) {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setDeviceType(TransportPayloadType.PROTOBUF.name());
        return builder.build();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `mqttQoS`：`mqttQoS` 参数。
     * 返回：无。
     */
    protected void processSequenceOneWayRpcTest(MqttQoS mqttQoS) throws Exception {
        List<String> expectedRequest = new ArrayList<>();
        List<String> actualRequests = new ArrayList<>();

        String deviceId = savedDevice.getId().getId().toString();

        for (int i = 0; i < 10; i++) {
            ObjectNode request = JacksonUtil.newObjectNode();
            request.put("method", "test");
            request.put("params", i);
            expectedRequest.add(JacksonUtil.toString(request));
            request.put("persistent", true);
            doPostAsync("/api/rpc/oneway/" + deviceId, JacksonUtil.toString(request), String.class, status().isOk());
        }

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestOneWaySequenceCallback callback = new MqttTestOneWaySequenceCallback(client, 10, actualRequests);
        client.setCallback(callback);
        subscribeAndWait(client, DEVICE_RPC_REQUESTS_SUB_TOPIC, savedDevice.getId(), FeatureType.RPC, mqttQoS);

        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(expectedRequest, actualRequests);
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `mqttQoS`：`mqttQoS` 参数。
     * 返回：无。
     */
    protected void processSequenceTwoWayRpcTest(MqttQoS mqttQoS) throws Exception {
        processSequenceTwoWayRpcTest(mqttQoS, false);
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `mqttQoS`：`mqttQoS` 参数。
     * - `manualAcksEnabled`：`manualAcksEnabled` 参数。
     * 返回：无。
     */
    protected void processSequenceTwoWayRpcTest(MqttQoS mqttQoS, boolean manualAcksEnabled) throws Exception {
        List<String> expectedRequest = new ArrayList<>();
        List<String> actualRequests = new ArrayList<>();

        List<String> rpcIds = new ArrayList<>();

        List<String> expectedResponses = new ArrayList<>();
        List<String> actualResponses = new ArrayList<>();

        String deviceId = savedDevice.getId().getId().toString();

        for (int i = 0; i < 10; i++) {
            ObjectNode request = JacksonUtil.newObjectNode();
            request.put("method", "test");
            request.put("params", i);
            expectedRequest.add(JacksonUtil.toString(request));
            request.put("persistent", true);
            String response = doPostAsync("/api/rpc/twoway/" + deviceId, JacksonUtil.toString(request), String.class, status().isOk());
            var responseNode = JacksonUtil.toJsonNode(response);
            rpcIds.add(responseNode.get("rpcId").asText());
        }

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        if (manualAcksEnabled) {
            client.enableManualAcks();
        }
        MqttTestTwoWaySequenceCallback callback = new MqttTestTwoWaySequenceCallback(
                client, 10, actualRequests, expectedResponses, manualAcksEnabled);
        client.setCallback(callback);
        subscribeAndWait(client, DEVICE_RPC_REQUESTS_SUB_TOPIC, savedDevice.getId(), FeatureType.RPC, mqttQoS);

        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(expectedRequest, actualRequests);
        awaitForDeviceActorToProcessAllRpcResponses(savedDevice.getId());
        for (String rpcId : rpcIds) {
            Rpc rpc = doGet("/api/rpc/persistent/" + rpcId, Rpc.class);
            actualResponses.add(JacksonUtil.toString(rpc.getResponse()));
        }
        assertEquals(expectedResponses, actualResponses);
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processJsonTwoWayRpcTestGateway(String deviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);

        String payload = "{\"device\":\"" + deviceName + "\"}";
        byte[] payloadBytes = payload.getBytes();

        validateJsonTwoWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    /**
     * 功能：校验RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `client`：客户端对象。
     * - `connectPayloadBytes`：`connectPayloadBytes` 参数。
     * 返回：无。
     */
    protected void validateOneWayRpcGatewayResponse(String deviceName, MqttTestClient client, byte[] connectPayloadBytes) throws Exception {
        client.publish(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);
        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(GATEWAY_RPC_TOPIC);
        client.setCallback(callback);
        subscribeAndCheckSubscription(client, GATEWAY_RPC_TOPIC, savedDevice.getId(), FeatureType.RPC);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        assertTrue(StringUtils.isEmpty(result));
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        DeviceTransportType deviceTransportType = deviceProfile.getTransportType();
        if (deviceTransportType.equals(DeviceTransportType.MQTT)) {
            DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
            assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
            MqttDeviceProfileTransportConfiguration configuration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            TransportPayloadType transportPayloadType = configuration.getTransportPayloadTypeConfiguration().getTransportPayloadType();
            if (transportPayloadType.equals(TransportPayloadType.PROTOBUF)) {
                // TODO: add correct validation of proto requests to device
                assertTrue(callback.getPayloadBytes().length > 0);
            } else {
                JsonNode expectedJsonRequestData = getExpectedGatewayJsonRequestData(deviceName, setGpioRequest);
                assertEquals(expectedJsonRequestData, JacksonUtil.fromBytes(callback.getPayloadBytes()));
            }
        } else {
            JsonNode expectedJsonRequestData = getExpectedGatewayJsonRequestData(deviceName, setGpioRequest);
            assertEquals(expectedJsonRequestData, JacksonUtil.fromBytes(callback.getPayloadBytes()));
        }
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `requestStr`：请求对象。
     * 返回：处理结果。
     */
    private JsonNode getExpectedGatewayJsonRequestData(String deviceName, String requestStr) {
        ObjectNode deviceData = (ObjectNode) JacksonUtil.toJsonNode(requestStr);
        deviceData.put("id", 0);
        ObjectNode expectedRequest = JacksonUtil.newObjectNode();
        expectedRequest.put("device", deviceName);
        expectedRequest.set("data", deviceData);
        return expectedRequest;
    }

    /**
     * 功能：校验RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `client`：客户端对象。
     * - `connectPayloadBytes`：`connectPayloadBytes` 参数。
     * 返回：无。
     */
    protected void validateJsonTwoWayRpcGatewayResponse(String deviceName, MqttTestClient client, byte[] connectPayloadBytes) throws Exception {
        client.publish(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        MqttTestRpcJsonCallback callback = new MqttTestRpcJsonCallback(client, GATEWAY_RPC_TOPIC);
        client.setCallback(callback);
        subscribeAndCheckSubscription(client, GATEWAY_RPC_TOPIC, savedDevice.getId(), FeatureType.RPC);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.warn("request payload: {}", JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals("{\"success\":true}", actualRpcResponse);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
    }

    /**
     * 功能：校验RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `client`：客户端对象。
     * - `connectPayloadBytes`：`connectPayloadBytes` 参数。
     * 返回：无。
     */
    protected void validateProtoTwoWayRpcGatewayResponse(String deviceName, MqttTestClient client, byte[] connectPayloadBytes) throws Exception {
        client.publish(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        MqttTestRpcProtoCallback callback = new MqttTestRpcProtoCallback(client, GATEWAY_RPC_TOPIC);
        client.setCallback(callback);
        subscribeAndCheckSubscription(client, GATEWAY_RPC_TOPIC, savedDevice.getId(), FeatureType.RPC);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals("{\"success\":true}", actualRpcResponse);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private Device getDeviceByName(String deviceName) throws Exception {
        return doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `requestTopic`：请求对象。
     * - `mqttMessage`：待处理消息。
     * 返回：处理结果。
     */
    protected byte[] processJsonMessageArrived(String requestTopic, MqttMessage mqttMessage) {
        if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC) || requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
            return DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8);
        } else {
            JsonNode requestMsgNode = JacksonUtil.toJsonNode(new String(mqttMessage.getPayload(), StandardCharset.UTF_8));
            String deviceName = requestMsgNode.get("device").asText();
            int requestId = requestMsgNode.get("data").get("id").asInt();
            String response = "{\"device\": \"" + deviceName + "\", \"id\": " + requestId + ", \"data\": {\"success\": true}}";
            return response.getBytes(StandardCharset.UTF_8);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttTestRpcJsonCallback` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    protected class MqttTestRpcJsonCallback extends MqttTestSubscribeOnTopicCallback {

        /**
         * 客户端，用于发起外部调用或协议交互。
         */
        private final MqttTestClient client;

        /**
         * 功能：创建 `AbstractMqttServerSideRpcIntegrationTest` 实例，并初始化必要字段。
         * 参数：
         * - `client`：客户端对象。
         * - `awaitSubTopic`：主题名称或主题对象。
         * 返回：新创建的对象实例。
         */
        public MqttTestRpcJsonCallback(MqttTestClient client, String awaitSubTopic) {
            super(awaitSubTopic);
            this.client = client;
        }

        /**
         * 功能：执行 `messageArrived` 对应的处理。
         * 参数：
         * - `requestTopic`：请求对象。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            if (awaitSubTopic.equals(requestTopic)) {
                messageArrivedQoS = mqttMessage.getQos();
                payloadBytes = mqttMessage.getPayload();
                String responseTopic;
                if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
                    responseTopic = requestTopic.replace("req", "res");
                } else {
                    responseTopic = requestTopic.replace("request", "response");
                }
                try {
                    client.publish(responseTopic, processJsonMessageArrived(requestTopic, mqttMessage));
                } catch (MqttException e) {
                    log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
                }
                subscribeLatch.countDown();
            }
        }

    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttTestRpcProtoCallback` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    protected class MqttTestRpcProtoCallback extends MqttTestSubscribeOnTopicCallback {

        /**
         * 客户端，用于发起外部调用或协议交互。
         */
        private final MqttTestClient client;

        /**
         * 功能：创建 `AbstractMqttServerSideRpcIntegrationTest` 实例，并初始化必要字段。
         * 参数：
         * - `client`：客户端对象。
         * - `awaitSubTopic`：主题名称或主题对象。
         * 返回：新创建的对象实例。
         */
        public MqttTestRpcProtoCallback(MqttTestClient client, String awaitSubTopic) {
            super(awaitSubTopic);
            this.client = client;
        }

        /**
         * 功能：执行 `messageArrived` 对应的处理。
         * 参数：
         * - `requestTopic`：请求对象。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            if (awaitSubTopic.equals(requestTopic)) {
                messageArrivedQoS = mqttMessage.getQos();
                payloadBytes = mqttMessage.getPayload();
                String responseTopic;
                if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
                    responseTopic = requestTopic.replace("req", "res");
                } else {
                    responseTopic = requestTopic.replace("request", "response");
                }
                try {
                    client.publish(responseTopic, processProtoMessageArrived(requestTopic, mqttMessage));
                } catch (Exception e) {
                    log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
                }
                subscribeLatch.countDown();
            }
        }

    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `requestTopic`：请求对象。
     * - `mqttMessage`：待处理消息。
     * 返回：处理结果。
     */
    protected byte[] processProtoMessageArrived(String requestTopic, MqttMessage mqttMessage) throws MqttException, InvalidProtocolBufferException {
        if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC) || requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
            ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = getProtoTransportPayloadConfiguration();
            ProtoFileElement rpcRequestProtoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema());
            DynamicSchema rpcRequestProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcRequestProtoFileElement, ProtoTransportPayloadConfiguration.RPC_REQUEST_PROTO_SCHEMA);

            byte[] requestPayload = mqttMessage.getPayload();
            DynamicMessage.Builder rpcRequestMsg = rpcRequestProtoSchema.newMessageBuilder("RpcRequestMsg");
            Descriptors.Descriptor rpcRequestMsgDescriptor = rpcRequestMsg.getDescriptorForType();
            assertNotNull(rpcRequestMsgDescriptor);
            try {
                DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rpcRequestMsgDescriptor, requestPayload);
                List<Descriptors.FieldDescriptor> fields = rpcRequestMsgDescriptor.getFields();
                for (Descriptors.FieldDescriptor fieldDescriptor : fields) {
                    assertTrue(dynamicMessage.hasField(fieldDescriptor));
                }
                ProtoFileElement rpcResponseProtoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema());
                DynamicSchema rpcResponseProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcResponseProtoFileElement, ProtoTransportPayloadConfiguration.RPC_RESPONSE_PROTO_SCHEMA);

                DynamicMessage.Builder rpcResponseBuilder = rpcResponseProtoSchema.newMessageBuilder("RpcResponseMsg");
                Descriptors.Descriptor rpcResponseMsgDescriptor = rpcResponseBuilder.getDescriptorForType();
                assertNotNull(rpcResponseMsgDescriptor);
                DynamicMessage rpcResponseMsg = rpcResponseBuilder
                        .setField(rpcResponseMsgDescriptor.findFieldByName("payload"), DEVICE_RESPONSE)
                        .build();
                return rpcResponseMsg.toByteArray();
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException("Command Response Ack Error, Invalid response received: ", e);
            }
        } else {
            TransportApiProtos.GatewayDeviceRpcRequestMsg msg = TransportApiProtos.GatewayDeviceRpcRequestMsg.parseFrom(mqttMessage.getPayload());
            String deviceName = msg.getDeviceName();
            int requestId = msg.getRpcRequestMsg().getRequestId();
            TransportApiProtos.GatewayRpcResponseMsg gatewayRpcResponseMsg = TransportApiProtos.GatewayRpcResponseMsg.newBuilder()
                    .setDeviceName(deviceName)
                    .setId(requestId)
                    .setData("{\"success\": true}")
                    .build();
            return gatewayRpcResponseMsg.toByteArray();
        }
    }

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    private ProtoTransportPayloadConfiguration getProtoTransportPayloadConfiguration() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        return (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttTestOneWaySequenceCallback` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    protected static class MqttTestOneWaySequenceCallback extends MqttTestCallback {

        /**
         * `requests`列表，用于保存一组待处理对象。
         */
        private final List<String> requests;

        MqttTestOneWaySequenceCallback(MqttTestClient client, int subscribeCount, List<String> requests) {
            super(subscribeCount);
            this.requests = requests;
        }

        /**
         * 功能：执行 `messageArrived` 对应的处理。
         * 参数：
         * - `requestTopic`：请求对象。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}", requestTopic);
            requests.add(new String(mqttMessage.getPayload()));
            messageArrivedQoS = mqttMessage.getQos();
            subscribeLatch.countDown();
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttTestTwoWaySequenceCallback` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    protected class MqttTestTwoWaySequenceCallback extends MqttTestCallback {

        /**
         * 客户端，用于发起外部调用或协议交互。
         */
        private final MqttTestClient client;
        private final List<String> requests;
        /**
         * `responses`列表，用于保存一组待处理对象。
         */
        private final List<String> responses;
        private final boolean manualAcksEnabled;

        MqttTestTwoWaySequenceCallback(MqttTestClient client, int subscribeCount, List<String> requests, List<String> responses, boolean manualAcksEnabled) {
            super(subscribeCount);
            this.client = client;
            this.requests = requests;
            this.responses = responses;
            this.manualAcksEnabled = manualAcksEnabled;
        }

        /**
         * 功能：执行 `messageArrived` 对应的处理。
         * 参数：
         * - `requestTopic`：请求对象。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}", requestTopic);
            requests.add(new String(mqttMessage.getPayload()));
            messageArrivedQoS = mqttMessage.getQos();
            if (manualAcksEnabled) {
                try {
                    client.messageArrivedComplete(mqttMessage);
                } catch (MqttException e) {
                    log.warn("Failed to ack message delivery on topic: {} due to: ", requestTopic, e);
                } finally {
                    subscribeLatch.countDown();
                    processResponse(requestTopic, mqttMessage);
                }
                return;
            }
            subscribeLatch.countDown();
            processResponse(requestTopic, mqttMessage);
        }

        /**
         * 功能：处理响应。
         * 参数：
         * - `requestTopic`：请求对象。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        private void processResponse(String requestTopic, MqttMessage mqttMessage) {
            String responseTopic = requestTopic.replace("request", "response");
            byte[] responsePayload = processJsonMessageArrived(requestTopic, mqttMessage);
            responses.add(new String(responsePayload));
            try {
                client.publish(responseTopic, responsePayload);
            } catch (MqttException e) {
                log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
            }
        }

    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttServerSideRpcIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
