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
package org.thingsboard.server.transport.mqtt.mqttv3.telemetry.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttTimeseriesJsonIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public abstract class AbstractMqttTimeseriesJsonIntegrationTest extends AbstractMqttTimeseriesIntegrationTest {

    /**
     * 主题常量，用于统一引用固定值。
     */
    private static final String POST_DATA_TELEMETRY_TOPIC = "data/telemetry";

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    @Override
    public void beforeTest() throws Exception {
        //do nothing, processBeforeTest will be invoked in particular test methods with different parameters
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetry() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithTs() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}";
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, payloadStr.getBytes(), true);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryOnShortTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        super.testPushTelemetryOnShortTopic();
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryOnShortJsonTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        super.testPushTelemetryOnShortJsonTopic();
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryGateway() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .gatewayName("Test Post Telemetry gateway json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        super.testPushTelemetryGateway();
    }

    /**
     * 功能：验证`Gateway Connect`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGatewayConnect() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .gatewayName("Test Post Telemetry gateway json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        super.testGatewayConnect();
    }

    /**
     * 功能：验证消息载荷相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithMalformedPayloadAndSendAckOnErrorEnabled() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .sendAckOnValidationException(true)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_JSON_PAYLOAD.getBytes());
        callback.getDeliveryLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(callback.isPubAckReceived());
        client.disconnect();
    }

    /**
     * 功能：验证消息载荷相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithMalformedPayloadAndSendAckOnErrorDisabled() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_JSON_PAYLOAD.getBytes());
        callback.getDeliveryLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(callback.isPubAckReceived());
    }

    /**
     * 功能：验证消息载荷相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryGatewayWithMalformedPayloadAndSendAckOnErrorEnabled() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .gatewayName("Test Post Telemetry gateway json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .sendAckOnValidationException(true)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_JSON_PAYLOAD.getBytes());
        callback.getDeliveryLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(callback.isPubAckReceived());
        client.disconnect();
    }

    /**
     * 功能：验证消息载荷相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryGatewayWithMalformedPayloadAndSendAckOnErrorDisabled() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .gatewayName("Test Post Telemetry gateway json payload")
                .transportPayloadType(TransportPayloadType.JSON)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_JSON_PAYLOAD.getBytes());
        callback.getDeliveryLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(callback.isPubAckReceived());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttTimeseriesJsonIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
