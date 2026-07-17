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

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_PROTO_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_TELEMETRY_TOPIC;

/**
 * 中文说明：
 * 1. `AbstractMqttTimeseriesProtoIntegrationTest` 是 ThingsBoard Application 中验证 `AbstractMqttTimeseriesProtoIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttTimeseriesIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractMqttTimeseriesProtoIntegrationTest extends AbstractMqttTimeseriesIntegrationTest {

    /**
     * 主题常量，用于统一引用固定值。
     */
    private static final String POST_DATA_TELEMETRY_TOPIC = "proto/telemetry";
    private static final String MALFORMED_PROTO_PAYLOAD = "invalid proto payload str";

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
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicMessage postTelemetryMsg = getDefaultDynamicMessage();
        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postTelemetryMsg.toByteArray(), false, false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithEnabledJsonBackwardCompatibility() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .build();
        processBeforeTest(configProperties);
        processJsonPayloadTelemetryTest(POST_DATA_TELEMETRY_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), PAYLOAD_VALUES_STR.getBytes(), false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithTs() throws Exception {
        String schemaStr = "syntax =\"proto3\";\n" +
                "\n" +
                "package test;\n" +
                "\n" +
                "message PostTelemetry {\n" +
                "  optional int64 ts = 1;\n" +
                "  Values values = 2;\n" +
                "  \n" +
                "  message Values {\n" +
                "    optional string key1 = 3;\n" +
                "    optional bool key2 = 4;\n" +
                "    optional double key3 = 5;\n" +
                "    optional int32 key4 = 6;\n" +
                "    JsonObject key5 = 7;\n" +
                "  }\n" +
                "  \n" +
                "  message JsonObject {\n" +
                "    optional int32 someNumber = 8;\n" +
                "    repeated int32 someArray = 9;\n" +
                "    NestedJsonObject someNestedObject = 10;\n" +
                "    message NestedJsonObject {\n" +
                "       optional string key = 11;\n" +
                "    }\n" +
                "  }\n" +
                "}";
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .telemetryProtoSchema(schemaStr)
                .build();
        processBeforeTest(configProperties);
        DynamicSchema telemetrySchema = getDynamicSchema();

        DynamicMessage.Builder nestedJsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();


        DynamicMessage.Builder valuesBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.Values");
        Descriptors.Descriptor valuesDescriptor = valuesBuilder.getDescriptorForType();
        assertNotNull(valuesDescriptor);

        DynamicMessage valuesMsg = valuesBuilder
                .setField(valuesDescriptor.findFieldByName("key1"), "value1")
                .setField(valuesDescriptor.findFieldByName("key2"), true)
                .setField(valuesDescriptor.findFieldByName("key3"), 3.0)
                .setField(valuesDescriptor.findFieldByName("key4"), 4)
                .setField(valuesDescriptor.findFieldByName("key5"), jsonObject)
                .build();

        DynamicMessage.Builder postTelemetryBuilder = telemetrySchema.newMessageBuilder("PostTelemetry");
        Descriptors.Descriptor postTelemetryMsgDescriptor = postTelemetryBuilder.getDescriptorForType();
        assertNotNull(postTelemetryMsgDescriptor);
        DynamicMessage postTelemetryMsg = postTelemetryBuilder
                .setField(postTelemetryMsgDescriptor.findFieldByName("ts"), 10000L)
                .setField(postTelemetryMsgDescriptor.findFieldByName("values"), valuesMsg)
                .build();

        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postTelemetryMsg.toByteArray(), true, false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithExplicitPresenceProtoKeys() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicSchema telemetrySchema = getDynamicSchema();

        DynamicMessage.Builder nestedJsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postTelemetryBuilder = telemetrySchema.newMessageBuilder("PostTelemetry");
        Descriptors.Descriptor postTelemetryMsgDescriptor = postTelemetryBuilder.getDescriptorForType();
        assertNotNull(postTelemetryMsgDescriptor);
        DynamicMessage postTelemetryMsg = postTelemetryBuilder
                .setField(postTelemetryMsgDescriptor.findFieldByName("key1"), "")
                .setField(postTelemetryMsgDescriptor.findFieldByName("key2"), false)
                .setField(postTelemetryMsgDescriptor.findFieldByName("key3"), 0.0)
                .setField(postTelemetryMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, Arrays.asList("key1", "key2", "key3", "key5"), postTelemetryMsg.toByteArray(), false, true);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithTsAndNoPresenceFields() throws Exception {
        String schemaStr = "syntax =\"proto3\";\n" +
                "\n" +
                "package test;\n" +
                "\n" +
                "message PostTelemetry {\n" +
                "  optional int64 ts = 1;\n" +
                "  Values values = 2;\n" +
                "  \n" +
                "  message Values {\n" +
                "    string key1 = 3;\n" +
                "    bool key2 = 4;\n" +
                "    double key3 = 5;\n" +
                "    int32 key4 = 6;\n" +
                "    JsonObject key5 = 7;\n" +
                "  }\n" +
                "  \n" +
                "  message JsonObject {\n" +
                "    optional int32 someNumber = 8;\n" +
                "    repeated int32 someArray = 9;\n" +
                "    NestedJsonObject someNestedObject = 10;\n" +
                "    message NestedJsonObject {\n" +
                "       optional string key = 11;\n" +
                "    }\n" +
                "  }\n" +
                "}";
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .telemetryProtoSchema(schemaStr)
                .build();
        processBeforeTest(configProperties);
        DynamicSchema telemetrySchema = getDynamicSchema();

        DynamicMessage.Builder nestedJsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();


        DynamicMessage.Builder valuesBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.Values");
        Descriptors.Descriptor valuesDescriptor = valuesBuilder.getDescriptorForType();
        assertNotNull(valuesDescriptor);

        DynamicMessage valuesMsg = valuesBuilder
                .setField(valuesDescriptor.findFieldByName("key5"), jsonObject)
                .build();

        DynamicMessage.Builder postTelemetryBuilder = telemetrySchema.newMessageBuilder("PostTelemetry");
        Descriptors.Descriptor postTelemetryMsgDescriptor = postTelemetryBuilder.getDescriptorForType();
        assertNotNull(postTelemetryMsgDescriptor);
        DynamicMessage postTelemetryMsg = postTelemetryBuilder
                .setField(postTelemetryMsgDescriptor.findFieldByName("ts"), 10000L)
                .setField(postTelemetryMsgDescriptor.findFieldByName("values"), valuesMsg)
                .build();

        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postTelemetryMsg.toByteArray(), true, true);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryOnShortTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicMessage postTelemetryMsg = getDefaultDynamicMessage();
        processTelemetryTest(DEVICE_TELEMETRY_SHORT_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postTelemetryMsg.toByteArray(), false, false);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryOnShortJsonTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_SHORT_JSON_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), PAYLOAD_VALUES_STR.getBytes(), false);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryOnShortProtoTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicMessage postTelemetryMsg = getDefaultDynamicMessage();
        processTelemetryTest(DEVICE_TELEMETRY_SHORT_PROTO_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postTelemetryMsg.toByteArray(), false, false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryGateway() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .gatewayName("Test Post Telemetry gateway proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        TransportApiProtos.GatewayTelemetryMsg.Builder gatewayTelemetryMsgProtoBuilder = TransportApiProtos.GatewayTelemetryMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        TransportApiProtos.TelemetryMsg deviceATelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName1, expectedKeys, 10000, 20000);
        TransportApiProtos.TelemetryMsg deviceBTelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName2, expectedKeys, 10000, 20000);
        gatewayTelemetryMsgProtoBuilder.addAllMsg(Arrays.asList(deviceATelemetryMsgProto, deviceBTelemetryMsgProto));
        TransportApiProtos.GatewayTelemetryMsg gatewayTelemetryMsg = gatewayTelemetryMsgProtoBuilder.build();
        processGatewayTelemetryTest(GATEWAY_TELEMETRY_TOPIC, expectedKeys, gatewayTelemetryMsg.toByteArray(), deviceName1, deviceName2);
    }

    /**
     * 功能：验证`Gateway Connect`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGatewayConnect() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .gatewayName("Test Post Telemetry gateway proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        String deviceName = "Device A";
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publish(GATEWAY_CONNECT_TOPIC, connectMsgProto.toByteArray());

        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100);

        assertNotNull(device);
        client.disconnect();
    }


    /**
     * 功能：验证消息载荷相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithMalformedPayloadAndSendAckOnErrorEnabled() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .sendAckOnValidationException(true)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_PROTO_PAYLOAD.getBytes());
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
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_PROTO_PAYLOAD.getBytes());
        callback.getDeliveryLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(callback.isPubAckReceived());
    }

    /**
     * 功能：验证消息载荷相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithMalformedPayloadAndSendAckOnErrorEnabledAndBackwardCompatibilityEnabled() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .enableCompatibilityWithJsonPayloadFormat(true)
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
    public void testPushTelemetryWithMalformedPayloadAndSendAckOnErrorDisabledAndBackwardCompatibilityEnabled() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .enableCompatibilityWithJsonPayloadFormat(true)
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
                .deviceName("Test Post Telemetry device proto payload")
                .gatewayName("Test Post Telemetry gateway proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .sendAckOnValidationException(true)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_PROTO_PAYLOAD.getBytes());
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
                .deviceName("Test Post Telemetry device proto payload")
                .gatewayName("Test Post Telemetry gateway proto payload")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .telemetryTopicFilter(POST_DATA_TELEMETRY_TOPIC)
                .build();
        processBeforeTest(configProperties);
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.publish(POST_DATA_TELEMETRY_TOPIC, MALFORMED_PROTO_PAYLOAD.getBytes());
        callback.getDeliveryLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(callback.isPubAckReceived());
    }

    /**
     * 功能：获取`Dynamic Schema`。
     * 参数：无。
     * 返回：处理结果。
     */
    private DynamicSchema getDynamicSchema() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        String deviceTelemetryProtoSchema = protoTransportPayloadConfiguration.getDeviceTelemetryProtoSchema();
        ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(deviceTelemetryProtoSchema);
        return DynamicProtoUtils.getDynamicSchema(protoFileElement, ProtoTransportPayloadConfiguration.TELEMETRY_PROTO_SCHEMA);
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    private DynamicMessage getDefaultDynamicMessage() {
        DynamicSchema telemetrySchema = getDynamicSchema();

        DynamicMessage.Builder nestedJsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = telemetrySchema.newMessageBuilder("PostTelemetry.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postTelemetryBuilder = telemetrySchema.newMessageBuilder("PostTelemetry");
        Descriptors.Descriptor postTelemetryMsgDescriptor = postTelemetryBuilder.getDescriptorForType();
        assertNotNull(postTelemetryMsgDescriptor);
        return postTelemetryBuilder
                .setField(postTelemetryMsgDescriptor.findFieldByName("key1"), "value1")
                .setField(postTelemetryMsgDescriptor.findFieldByName("key2"), true)
                .setField(postTelemetryMsgDescriptor.findFieldByName("key3"), 3.0)
                .setField(postTelemetryMsgDescriptor.findFieldByName("key4"), 4)
                .setField(postTelemetryMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
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
     * 功能：获取设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `expectedKeys`：键。
     * - `firstTs`：时间戳。
     * - `secondTs`：时间戳。
     * 返回：处理结果。
     */
    private TransportApiProtos.TelemetryMsg getDeviceTelemetryMsgProto(String deviceName, List<String> expectedKeys, long firstTs, long secondTs) {
        TransportApiProtos.TelemetryMsg.Builder deviceTelemetryMsgBuilder = TransportApiProtos.TelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto tsKvListProto1 = getTsKvListProto(expectedKeys, firstTs);
        TransportProtos.TsKvListProto tsKvListProto2 = getTsKvListProto(expectedKeys, secondTs);
        TransportProtos.PostTelemetryMsg.Builder msg = TransportProtos.PostTelemetryMsg.newBuilder();
        msg.addAllTsKvList(Arrays.asList(tsKvListProto1, tsKvListProto2));
        deviceTelemetryMsgBuilder.setDeviceName(deviceName);
        deviceTelemetryMsgBuilder.setMsg(msg);
        return deviceTelemetryMsgBuilder.build();
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `expectedKeys`：键。
     * - `ts`：时间戳。
     * 返回：匹配的数据集合。
     */
    private TransportProtos.TsKvListProto getTsKvListProto(List<String> expectedKeys, long ts) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
        builder.addAllKv(kvProtos);
        builder.setTs(ts);
        return builder.build();
    }
}
