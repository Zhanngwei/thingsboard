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
package org.thingsboard.server.transport.mqtt.mqttv3.attributes.request;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.attributes.AbstractMqttAttributesIntegrationTest;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：
 * 1. `MqttAttributesRequestBackwardCompatibilityIntegrationTest` 是 ThingsBoard Application 中验证 `MqttAttributesRequestBackwardCompatibilityIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttAttributesIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class MqttAttributesRequestBackwardCompatibilityIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRequestAttributesValuesFromTheServerWithEnabledJsonCompatibility() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server proto")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesProtoSchema(ATTRIBUTES_SCHEMA_STR)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .build();
        processBeforeTest(configProperties);
        processProtoTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRequestAttributesValuesFromTheServerWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server proto")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesProtoSchema(ATTRIBUTES_SCHEMA_STR)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .useJsonPayloadFormatForDefaultDownlinkTopics(true)
                .build();
        processBeforeTest(configProperties);
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRequestAttributesValuesFromTheServerOnShortTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server proto")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesProtoSchema(ATTRIBUTES_SCHEMA_STR)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .useJsonPayloadFormatForDefaultDownlinkTopics(true)
                .build();
        processBeforeTest(configProperties);
        processProtoTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRequestAttributesValuesFromTheServerOnShortProtoTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server proto")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesProtoSchema(ATTRIBUTES_SCHEMA_STR)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .useJsonPayloadFormatForDefaultDownlinkTopics(true)
                .build();
        processBeforeTest(configProperties);
        processProtoTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX);
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRequestAttributesValuesFromTheServerGatewayWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Gateway Test Request attribute values from the server proto")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .useJsonPayloadFormatForDefaultDownlinkTopics(true)
                .build();
        processBeforeTest(configProperties);
        processProtoTestGatewayRequestAttributesValuesFromTheServer();
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRequestAttributesValuesFromTheServerOnShortJsonTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server proto")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .enableCompatibilityWithJsonPayloadFormat(true)
                .useJsonPayloadFormatForDefaultDownlinkTopics(true)
                .build();
        processBeforeTest(configProperties);
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX);
    }


    /**
     * 功能：获取`Kv Protos`。
     * 参数：
     * - `expectedKeys`：键。
     * 返回：匹配的数据集合。
     */
    protected List<TransportProtos.KeyValueProto> getKvProtos(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> keyValueProtos = new ArrayList<>();
        TransportProtos.KeyValueProto strKeyValueProto = getKeyValueProto(expectedKeys.get(0), "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.KeyValueProto boolKeyValueProto = getKeyValueProto(expectedKeys.get(1), "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.KeyValueProto dblKeyValueProto = getKeyValueProto(expectedKeys.get(2), "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.KeyValueProto longKeyValueProto = getKeyValueProto(expectedKeys.get(3), "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.KeyValueProto jsonKeyValueProto = getKeyValueProto(expectedKeys.get(4), "{\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}", TransportProtos.KeyValueType.JSON_V);
        keyValueProtos.add(strKeyValueProto);
        keyValueProtos.add(boolKeyValueProto);
        keyValueProtos.add(dblKeyValueProto);
        keyValueProtos.add(longKeyValueProto);
        keyValueProtos.add(jsonKeyValueProto);
        return keyValueProtos;
    }

}
