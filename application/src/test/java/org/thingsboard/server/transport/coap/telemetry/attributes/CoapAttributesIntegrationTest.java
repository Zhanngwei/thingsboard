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
package org.thingsboard.server.transport.coap.telemetry.attributes;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 中文说明：
 * 1. 类目的：`CoapAttributesIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
@DaoSqlTest
public class CoapAttributesIntegrationTest extends AbstractCoapIntegrationTest {

    private static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .build();
        processBeforeTest(configProperties);
    }

    /**
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    /**
     * 功能：验证`Push Attributes`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushAttributes() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    /**
     * 功能：处理消息载荷。
     * 参数：
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    protected void processJsonPayloadAttributesTest(List<String> expectedKeys, byte[] payload) throws Exception {
        processAttributesTest(expectedKeys, payload, false);
    }

    /**
     * 功能：处理`Attributes Test`。
     * 参数：
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * - `presenceFieldsTest`：`presenceFieldsTest` 参数。
     * 返回：无。
     */
    protected void processAttributesTest(List<String> expectedKeys, byte[] payload, boolean presenceFieldsTest) throws Exception {

        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        CoapResponse coapResponse = client.postMethod(payload);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        DeviceId deviceId = savedDevice.getId();
        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
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
     * 功能：执行 `assertAttributesValues` 对应的处理。
     * 参数：
     * - `deviceValues`：设备信息或设备标识。
     * - `keySet`：键。
     * 返回：无。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void assertAttributesValues(List<Map<String, Object>> deviceValues, Set<String> keySet) {
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
     * 功能：获取`Actual Keys List`。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `expectedKeys`：键。
     * 返回：匹配的数据集合。
     */
    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
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
        return actualKeys;
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
 * 1. 核心职责：`CoapAttributesIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
