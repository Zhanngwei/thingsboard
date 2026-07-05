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
package org.thingsboard.server.transport.mqtt.mqttv3.telemetry.attributes;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC;

/**
 * 中文说明：
 * 1. 类目的：`MqttAttributesProtoIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
@DaoSqlTest
public class MqttAttributesProtoIntegrationTest extends MqttAttributesIntegrationTest {

    /**
     * 主题常量，用于统一引用固定值。
     */
    private static final String POST_DATA_ATTRIBUTES_TOPIC = "proto/attributes";

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
     * 功能：验证`Push Attributes`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributes() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesTopicFilter(POST_DATA_ATTRIBUTES_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicMessage postAttributesMsg = getDefaultDynamicMessage();
        processAttributesTest(POST_DATA_ATTRIBUTES_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postAttributesMsg.toByteArray(), false);
    }

    /**
     * 功能：验证JSON相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesWithEnabledJsonBackwardCompatibility() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesTopicFilter(POST_DATA_ATTRIBUTES_TOPIC)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .build();
        processBeforeTest(configProperties);
        processJsonPayloadAttributesTest(POST_DATA_ATTRIBUTES_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), PAYLOAD_VALUES_STR.getBytes());
    }

    /**
     * 功能：验证Protobuf相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesWithExplicitPresenceProtoKeys() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesTopicFilter(POST_DATA_ATTRIBUTES_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicSchema attributesSchema = getDynamicSchema();

        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("key1"), "")
                .setField(postAttributesMsgDescriptor.findFieldByName("key2"), false)
                .setField(postAttributesMsgDescriptor.findFieldByName("key3"), 0.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("key4"), 0)
                .setField(postAttributesMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
        processAttributesTest(POST_DATA_ATTRIBUTES_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postAttributesMsg.toByteArray(), true);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesOnShortTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesTopicFilter(POST_DATA_ATTRIBUTES_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicMessage postAttributesMsg = getDefaultDynamicMessage();
        processAttributesTest(DEVICE_ATTRIBUTES_SHORT_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postAttributesMsg.toByteArray(), false);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesOnShortJsonTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesTopicFilter(POST_DATA_ATTRIBUTES_TOPIC)
                .build();
        processBeforeTest(configProperties);
        processJsonPayloadAttributesTest(DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), PAYLOAD_VALUES_STR.getBytes());
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesOnShortProtoTopic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesTopicFilter(POST_DATA_ATTRIBUTES_TOPIC)
                .build();
        processBeforeTest(configProperties);
        DynamicMessage postAttributesMsg = getDefaultDynamicMessage();
        processAttributesTest(DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC, Arrays.asList("key1", "key2", "key3", "key4", "key5"), postAttributesMsg.toByteArray(), false);
    }

    /**
     * 功能：验证`Push Attributes Gateway`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesGateway() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .gatewayName("Test Post Attributes gateway")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
        TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributesMsgProtoBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        TransportApiProtos.AttributesMsg firstDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName1, expectedKeys);
        TransportApiProtos.AttributesMsg secondDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName2, expectedKeys);
        gatewayAttributesMsgProtoBuilder.addAllMsg(Arrays.asList(firstDeviceAttributesMsgProto, secondDeviceAttributesMsgProto));
        TransportApiProtos.GatewayAttributesMsg gatewayAttributesMsg = gatewayAttributesMsgProtoBuilder.build();
        processGatewayAttributesTest(expectedKeys, gatewayAttributesMsg.toByteArray(), deviceName1, deviceName2);
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
        String deviceAttributesProtoSchema = protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema();
        ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(deviceAttributesProtoSchema);
        return DynamicProtoUtils.getDynamicSchema(protoFileElement, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    private DynamicMessage getDefaultDynamicMessage() {
        DynamicSchema attributesSchema = getDynamicSchema();

        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        return postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("key1"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("key2"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("key3"), 3.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("key4"), 4)
                .setField(postAttributesMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `expectedKeys`：键。
     * 返回：处理结果。
     */
    private TransportApiProtos.AttributesMsg getDeviceAttributesMsgProto(String deviceName, List<String> expectedKeys) {
        TransportApiProtos.AttributesMsg.Builder deviceAttributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
        TransportProtos.PostAttributeMsg msg = getPostAttributeMsg(expectedKeys);
        deviceAttributesMsgBuilder.setDeviceName(deviceName);
        deviceAttributesMsgBuilder.setMsg(msg);
        return deviceAttributesMsgBuilder.build();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttAttributesProtoIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
