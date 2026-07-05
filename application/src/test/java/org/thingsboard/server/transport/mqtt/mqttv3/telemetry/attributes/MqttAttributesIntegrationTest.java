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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_ATTRIBUTES_TOPIC;

/**
 * 中文说明：
 * 1. 类目的：`MqttAttributesIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
@DaoSqlTest
public class MqttAttributesIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .gatewayName("Test Post Attributes gateway")
                .build();
        processBeforeTest(configProperties);
    }

    /**
     * 功能：验证`Push Attributes`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributes() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(DEVICE_ATTRIBUTES_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesOnShortTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(DEVICE_ATTRIBUTES_SHORT_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesOnShortJsonTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    /**
     * 功能：验证`Push Attributes Gateway`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributesGateway() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        String payload = getGatewayAttributesJsonPayload(deviceName1, deviceName2);
        processGatewayAttributesTest(expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }

    /**
     * 功能：处理消息载荷。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    protected void processJsonPayloadAttributesTest(String topic, List<String> expectedKeys, byte[] payload) throws Exception {
        processAttributesTest(topic, expectedKeys, payload, false);
    }

    /**
     * 功能：处理`Attributes Test`。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * - `presenceFieldsTest`：`presenceFieldsTest` 参数。
     * 返回：无。
     */
    protected void processAttributesTest(String topic, List<String> expectedKeys, byte[] payload, boolean presenceFieldsTest) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);

        client.publishAndWait(topic, payload);
        client.disconnect();

        DeviceId deviceId = savedDevice.getId();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {});
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, actualKeySet);

        String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet);
        List<Map<String, Object>> values = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {});
        if (presenceFieldsTest) {
            assertAttributesProtoValues(values, actualKeySet);
        } else {
            assertAttributesValues(values, actualKeySet);
        }
        String deleteAttributesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    /**
     * 功能：处理`Gateway Attributes Test`。
     * 参数：
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * - `firstDeviceName`：设备信息或设备标识。
     * - `secondDeviceName`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processGatewayAttributesTest(List<String> expectedKeys, byte[] payload, String firstDeviceName, String secondDeviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);

        client.publishAndWait(GATEWAY_ATTRIBUTES_TOPIC, payload);
        client.disconnect();

        Device firstDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + firstDeviceName, Device.class),
                20,
                100);

        assertNotNull(firstDevice);

        Device secondDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + secondDeviceName, Device.class),
                20,
                100);

        assertNotNull(secondDevice);

        List<String> firstDeviceActualKeys = getActualKeysList(firstDevice.getId(), expectedKeys);
        assertNotNull(firstDeviceActualKeys);
        Set<String> firstDeviceActualKeySet = new HashSet<>(firstDeviceActualKeys);

        List<String> secondDeviceActualKeys = getActualKeysList(secondDevice.getId(), expectedKeys);
        assertNotNull(secondDeviceActualKeys);
        Set<String> secondDeviceActualKeySet = new HashSet<>(secondDeviceActualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, firstDeviceActualKeySet);
        assertEquals(expectedKeySet, secondDeviceActualKeySet);

        String getAttributesValuesUrlFirstDevice = getAttributesValuesUrl(firstDevice.getId(), firstDeviceActualKeySet);
        String getAttributesValuesUrlSecondDevice = getAttributesValuesUrl(firstDevice.getId(), secondDeviceActualKeySet);

        List<Map<String, Object>> firstDeviceValues = doGetAsyncTyped(getAttributesValuesUrlFirstDevice, new TypeReference<>() {});
        List<Map<String, Object>> secondDeviceValues = doGetAsyncTyped(getAttributesValuesUrlSecondDevice, new TypeReference<>() {});

        assertAttributesValues(firstDeviceValues, expectedKeySet);
        assertAttributesValues(secondDeviceValues, expectedKeySet);

    }

    /**
     * 功能：获取`Actual Keys List`。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `expectedKeys`：键。
     * 返回：匹配的数据集合。
     */
    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 3000;
        List<String> firstDeviceActualKeys = null;
        while (start <= end) {
            firstDeviceActualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {});
            if (firstDeviceActualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return firstDeviceActualKeys;
    }

    /**
     * 功能：执行 `assertAttributesValues` 对应的处理。
     * 参数：
     * - `deviceValues`：设备信息或设备标识。
     * - `keySet`：键。
     * 返回：无。
     */
    @SuppressWarnings("unchecked")
    protected void assertAttributesValues(List<Map<String, Object>> deviceValues, Set<String> keySet) throws JsonProcessingException {
        for (Map<String, Object> map : deviceValues) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(keySet.contains(key));
            switch (key) {
                case "key1":
                    assertEquals("value1", value);
                    break;
                case "key2":
                    assertEquals(true, value);
                    break;
                case "key3":
                    assertEquals(3.0, value);
                    break;
                case "key4":
                    assertEquals(4, value);
                    break;
                case "key5":
                    assertNotNull(value);
                    assertEquals(3, ((LinkedHashMap) value).size());
                    assertEquals(42, ((LinkedHashMap) value).get("someNumber"));
                    assertEquals(Arrays.asList(1, 2, 3), ((LinkedHashMap) value).get("someArray"));
                    LinkedHashMap<String, String> someNestedObject = (LinkedHashMap) ((LinkedHashMap) value).get("someNestedObject");
                    assertEquals("value", someNestedObject.get("key"));
                    break;
            }
        }
    }

    /**
     * 功能：执行 `assertAttributesProtoValues` 对应的处理。
     * 参数：
     * - `values`：值。
     * - `keySet`：键。
     * 返回：无。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertAttributesProtoValues(List<Map<String, Object>> values, Set<String> keySet) {
        for (Map<String, Object> map : values) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(keySet.contains(key));
            switch (key) {
                case "key1":
                    assertEquals("", value);
                    break;
                case "key5":
                    assertNotNull(value);
                    assertEquals(2, ((LinkedHashMap) value).size());
                    assertEquals(Arrays.asList(1, 2, 3), ((LinkedHashMap) value).get("someArray"));
                    LinkedHashMap<String, String> someNestedObject = (LinkedHashMap) ((LinkedHashMap) value).get("someNestedObject");
                    assertEquals("value", someNestedObject.get("key"));
                    break;
            }
        }
    }

    /**
     * 功能：获取消息载荷。
     * 参数：
     * - `deviceA`：设备信息或设备标识。
     * - `deviceB`：设备信息或设备标识。
     * 返回：文本结果。
     */
    protected String getGatewayAttributesJsonPayload(String deviceA, String deviceB) {
        return "{\"" + deviceA + "\": " + PAYLOAD_VALUES_STR + ",  \"" + deviceB + "\": " + PAYLOAD_VALUES_STR + "}";
    }

    /**
     * 功能：获取URL 地址。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `actualKeySet`：键。
     * 返回：文本结果。
     */
    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttAttributesIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
