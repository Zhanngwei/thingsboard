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
package org.thingsboard.server.transport.coap.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestCallback;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.query.EntityKeyType.CLIENT_ATTRIBUTE;
import static org.thingsboard.server.common.data.query.EntityKeyType.SHARED_ATTRIBUTE;

/**
 * 中文说明：
 * 1. 类目的：`CoapClientIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
@DaoSqlTest
public class CoapClientIntegrationTest extends AbstractCoapIntegrationTest {

    private static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";
    private static final List<String> EXPECTED_KEYS = Arrays.asList("key1", "key2", "key3", "key4", "key5");
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";


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
     * 功能：验证`Confirmable Requests`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConfirmableRequests() throws Exception {
        boolean confirmable = true;
        processAttributesTest(confirmable);
        processTwoWayRpcTest(confirmable);
        processTestRequestAttributesValuesFromTheServer(confirmable);
    }

    /**
     * 功能：验证`Non Confirmable Requests`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testNonConfirmableRequests() throws Exception {
        boolean confirmable = false;
        processAttributesTest(confirmable);
        processTwoWayRpcTest(confirmable);
        processTestRequestAttributesValuesFromTheServer(confirmable);
    }

    /**
     * 功能：处理`Attributes Test`。
     * 参数：
     * - `confirmable`：`confirmable` 参数。
     * 返回：无。
     */
    protected void processAttributesTest(boolean confirmable) throws Exception {
        client = createClientForFeatureWithConfirmableParameter(FeatureType.ATTRIBUTES, confirmable);
        CoapResponse coapResponse = client.postMethod(PAYLOAD_VALUES_STR.getBytes());
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());
        assertEquals("CoAP response type is wrong!", client.getType(), coapResponse.advanced().getType());

        DeviceId deviceId = savedDevice.getId();
        List<String> actualKeys = getActualKeysList(deviceId);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(EXPECTED_KEYS);
        assertEquals(expectedKeySet, actualKeySet);

        String attributesValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        ;
        List<Map<String, Object>> values = doGetAsyncTyped(attributesValuesUrl, new TypeReference<>() {
        });
        assertAttributesValues(values, actualKeySet);
        String deleteAttributesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `confirmable`：`confirmable` 参数。
     * 返回：无。
     */
    protected void processTwoWayRpcTest(boolean confirmable) throws Exception {
        client = createClientForFeatureWithConfirmableParameter(FeatureType.RPC, confirmable);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap, confirmable);
        String awaitAlias = "await Two Way Rpc (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.VALID.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve());
        validateCurrentStateNotification(callbackCoap);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        int expectedObserveCountAfterGpioRequest1 = callbackCoap.getObserve() + 1;
        String actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) first";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest1 == callbackCoap.getObserve());
        validateTwoWayStateChangedNotification(callbackCoap, actualResult);

        int expectedObserveCountAfterGpioRequest2 = callbackCoap.getObserve() + 1;
        actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) second";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest2 == callbackCoap.getObserve());

        validateTwoWayStateChangedNotification(callbackCoap, actualResult);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `confirmable`：`confirmable` 参数。
     * 返回：无。
     */
    protected void processTestRequestAttributesValuesFromTheServer(boolean confirmable) throws Exception {
        client = createClientForFeatureWithConfirmableParameter(FeatureType.ATTRIBUTES, confirmable);
        SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(savedDevice.getId());
        List<EntityKey> csKeys = getEntityKeys(CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE",
                PAYLOAD_VALUES_STR, String.class, status().isOk());

        CoapResponse coapResponse = client.postMethod(PAYLOAD_VALUES_STR);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        String keysParam = String.join(",", EXPECTED_KEYS);
        String featureTokenUrl = CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + keysParam + "&sharedKeys=" + keysParam;
        client.setURI(featureTokenUrl);
        CoapResponse response = client.getMethod();
        assertEquals("CoAP response type is wrong!", client.getType(), response.advanced().getType());
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
     * 功能：获取`Actual Keys List`。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：匹配的数据集合。
     */
    private List<String> getActualKeysList(DeviceId deviceId) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {
            });
            if (actualKeys.size() == EXPECTED_KEYS.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateCurrentStateNotification(CoapTestCallback callback) {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `callback`：处理完成后的回调。
     * - `actualResult`：`actualResult` 参数。
     * 返回：无。
     */
    private void validateTwoWayStateChangedNotification(CoapTestCallback callback, String actualResult) {
        assertEquals(DEVICE_RESPONSE, actualResult);
        assertNotNull(callback.getPayloadBytes());
    }

    /**
     * 中文说明：
     * 1. 类目的：`TestCoapCallbackForRPC` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    protected class TestCoapCallbackForRPC extends CoapTestCallback {

        /**
         * 客户端，用于发起外部调用或协议交互。
         */
        private final CoapTestClient client;

        /**
         * 当前操作是否成功。
         */
        @Getter
        private boolean wasSuccessful = false;

        TestCoapCallbackForRPC(CoapTestClient client) {
            this.client = client;
        }

        /**
         * 功能：处理`on Load`。
         * 参数：
         * - `response`：响应对象。
         * 返回：无。
         */
        @Override
        public void onLoad(CoapResponse response) {
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            observe = response.getOptions().getObserve();
            wasSuccessful = client.getType().equals(response.advanced().getType());
            if (observe != null) {
                if (observe > 0) {
                    processOnLoadResponse(response, client);
                }
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：无。
         * 返回：无。
         */
        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `response`：响应对象。
     * - `client`：客户端对象。
     * 返回：无。
     */
    protected void processOnLoadResponse(CoapResponse response, CoapTestClient client) {
        JsonNode responseJson = JacksonUtil.fromBytes(response.getPayload());
        int requestId = responseJson.get("id").asInt();
        client.setURI(CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.RPC, requestId));
        client.postMethod(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                log.warn("RPC {} command response ack: {}", requestId, response.getCode());
            }

            @Override
            public void onError() {
                log.warn("RPC {} command response ack error, no connect", requestId);
            }
        }, DEVICE_RESPONSE, MediaTypeRegistry.APPLICATION_JSON);
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `featureType`：类型。
     * - `confirmable`：`confirmable` 参数。
     * 返回：处理结果。
     */
    private CoapTestClient createClientForFeatureWithConfirmableParameter(FeatureType featureType, boolean confirmable) {
        CoapTestClient coapTestClient = new CoapTestClient(accessToken, featureType);
        if (confirmable) {
            coapTestClient.useCONs();
        } else {
            coapTestClient.useNONs();
        }
        return coapTestClient;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    private List<EntityKey> getEntityKeys(EntityKeyType scope) {
        return CoapClientIntegrationTest.EXPECTED_KEYS.stream().map(key -> new EntityKey(scope, key)).collect(Collectors.toList());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CoapClientIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
