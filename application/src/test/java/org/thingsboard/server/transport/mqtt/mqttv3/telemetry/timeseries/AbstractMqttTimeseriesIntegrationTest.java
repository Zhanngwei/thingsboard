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

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_TELEMETRY_TOPIC;

/**
 * 中文说明：
 * 1. `AbstractMqttTimeseriesIntegrationTest` 是 ThingsBoard Application 中验证 `AbstractMqttTimeseriesIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractMqttTimeseriesIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected static final String MALFORMED_JSON_PAYLOAD = "{\"key1\":, \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device")
                .gatewayName("Test Post Telemetry gateway")
                .build();
        processBeforeTest(configProperties);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetry() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryWithTs() throws Exception {
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}";
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_TOPIC, expectedKeys, payloadStr.getBytes(), true);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryOnShortTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_SHORT_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryOnShortJsonTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_SHORT_JSON_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPushTelemetryGateway() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        String payload = getGatewayTelemetryJsonPayload(deviceName1, deviceName2, "10000", "20000");
        processGatewayTelemetryTest(GATEWAY_TELEMETRY_TOPIC, expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }

    /**
     * 功能：验证`Gateway Connect`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGatewayConnect() throws Exception {
        String payload = "{\"device\":\"Device A\"}";
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publish(GATEWAY_CONNECT_TOPIC, payload.getBytes());

        String deviceName = "Device A";

        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
            20,
        100);

        assertNotNull(device);
        client.disconnect();
    }

    /**
     * 功能：处理消息载荷。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * - `withTs`：时间戳。
     * 返回：无。
     */
    protected void processJsonPayloadTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, boolean withTs) throws Exception {
        processTelemetryTest(topic, expectedKeys, payload, withTs, false);
    }

    /**
     * 功能：处理遥测。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * - `withTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void processTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, boolean withTs, boolean presenceFieldsTest) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        client.publishAndWait(topic, payload);
        client.disconnect();

        DeviceId deviceId = savedDevice.getId();

        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, actualKeySet);

        String getTelemetryValuesUrl;
        if (withTs) {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=15000&keys=" + String.join(",", actualKeySet);
        } else {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + String.join(",", actualKeySet);
        }
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;
        Map<String, List<Map<String, Object>>> values = null;
        while (start <= end) {
            values = doGetAsyncTyped(getTelemetryValuesUrl, new TypeReference<>() {});
            boolean valid = values.size() == expectedKeys.size();
            if (valid) {
                for (String key : expectedKeys) {
                    List<Map<String, Object>> tsValues = values.get(key);
                    if (tsValues != null && tsValues.size() > 0) {
                        Object ts = tsValues.get(0).get("ts");
                        if (ts == null) {
                            valid = false;
                            break;
                        }
                    } else {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(values);

        if (withTs) {
            assertTs(values, expectedKeys, 10000, 0);
        }
        if (presenceFieldsTest) {
            assertExplicitProtoFieldValues(values);
        } else {
            assertValues(values, 0);
        }
    }

    /**
     * 功能：处理遥测。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `expectedKeys`：键。
     * - `payload`：`payload` 参数。
     * - `firstDeviceName`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void processGatewayTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, String firstDeviceName, String secondDeviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publishAndWait(topic, payload);
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
        Set<String> firstDeviceActualKeySet = new HashSet<>(firstDeviceActualKeys);

        List<String> secondDeviceActualKeys = getActualKeysList(secondDevice.getId(), expectedKeys);
        Set<String> secondDeviceActualKeySet = new HashSet<>(secondDeviceActualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, firstDeviceActualKeySet);
        assertEquals(expectedKeySet, secondDeviceActualKeySet);

        String getTelemetryValuesUrlFirstDevice = getTelemetryValuesUrl(firstDevice.getId(), firstDeviceActualKeySet);
        String getTelemetryValuesUrlSecondDevice = getTelemetryValuesUrl(firstDevice.getId(), secondDeviceActualKeySet);

        Map<String, List<Map<String, Object>>> firstDeviceValues = doGetAsyncTyped(getTelemetryValuesUrlFirstDevice, new TypeReference<>() {});
        Map<String, List<Map<String, Object>>> secondDeviceValues = doGetAsyncTyped(getTelemetryValuesUrlSecondDevice, new TypeReference<>() {});

        assertGatewayDeviceData(firstDeviceValues, expectedKeys);
        assertGatewayDeviceData(secondDeviceValues, expectedKeys);
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

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", new TypeReference<>() {});
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    /**
     * 功能：获取消息载荷。
     * 参数：
     * - `deviceA`：设备信息或设备标识。
     * - `deviceB`：设备信息或设备标识。
     * - `firstTsValue`：值。
     * - `secondTsValue`：值。
     * 返回：文本结果。
     */
    protected String getGatewayTelemetryJsonPayload(String deviceA, String deviceB, String firstTsValue, String secondTsValue) {
        String payload = "[{\"ts\": " + firstTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}, " +
                "{\"ts\": " + secondTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}]";
        return "{\"" + deviceA + "\": " + payload + ",  \"" + deviceB + "\": " + payload + "}";
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `actualKeySet`：键。
     * 返回：文本结果。
     */
    private String getTelemetryValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=25000&keys=" + String.join(",", actualKeySet);
    }

    /**
     * 功能：执行 `assertGatewayDeviceData` 对应的处理。
     * 参数：
     * - `deviceValues`：设备信息或设备标识。
     * - `expectedKeys`：键。
     * 返回：无。
     */
    private void assertGatewayDeviceData(Map<String, List<Map<String, Object>>> deviceValues, List<String> expectedKeys) {

        assertEquals(2, deviceValues.get(expectedKeys.get(0)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(1)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(2)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(3)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(4)).size());

        assertTs(deviceValues, expectedKeys, 20000, 0);
        assertTs(deviceValues, expectedKeys, 10000, 1);

        assertValues(deviceValues, 0);
        assertValues(deviceValues, 1);

    }

    /**
     * 功能：执行 `assertValues` 对应的处理。
     * 参数：
     * - `deviceValues`：设备信息或设备标识。
     * - `arrayIndex`：`arrayIndex` 参数。
     * 返回：无。
     */
    private void assertValues(Map<String, List<Map<String, Object>>> deviceValues, int arrayIndex) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(arrayIndex).get("value");
            switch (key) {
                case "key1":
                    assertEquals("value1", value);
                    break;
                case "key2":
                    assertEquals("true", value);
                    break;
                case "key3":
                    assertEquals("3.0", value);
                    break;
                case "key4":
                    assertEquals("4", value);
                    break;
                case "key5":
                    assertEquals("{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", value);
                    break;
            }
        }
    }

    /**
     * 功能：执行 `assertExplicitProtoFieldValues` 对应的处理。
     * 参数：
     * - `deviceValues`：设备信息或设备标识。
     * 返回：无。
     */
    private void assertExplicitProtoFieldValues(Map<String, List<Map<String, Object>>> deviceValues) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(0).get("value");
            switch (key) {
                case "key1":
                    assertEquals("", value);
                    break;
                case "key2":
                    assertEquals("false", value);
                    break;
                case "key3":
                    assertEquals("0.0", value);
                    break;
                case "key4":
                    assertEquals("0", value);
                    break;
                case "key5":
                    assertEquals("{\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", value);
                    break;
            }
        }
    }

    /**
     * 功能：执行 `assertTs` 对应的处理。
     * 参数：
     * - `deviceValues`：设备信息或设备标识。
     * - `expectedKeys`：键。
     * - `ts`：时间戳。
     * - `arrayIndex`：`arrayIndex` 参数。
     * 返回：无。
     */
    private void assertTs(Map<String, List<Map<String, Object>>> deviceValues, List<String> expectedKeys, int ts, int arrayIndex) {
        assertEquals(ts, deviceValues.get(expectedKeys.get(0)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(1)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(2)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(3)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(4)).get(arrayIndex).get("ts"));
    }

    //    @Test - Unstable
    /**
     * 功能：验证QoS 等级相关场景。
     * 参数：无。
     * 返回：无。
     */
    public void testMqttQoSLevel() throws Exception {
        MqttTestClient client = new MqttTestClient();
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.connectAndWait(accessToken);
        client.subscribe(DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);
        String payload = "{\"key\":\"uniqueValue\"}";
//        TODO 3.1: we need to acknowledge subscription only after it is processed by device actor and not when the message is pushed to queue.
//        MqttClient -> SUB REQUEST -> Transport -> Kafka -> Device Actor (subscribed)
//        MqttClient <- SUB_ACK <- Transport
        Thread.sleep(5000);
        doPostAsync("/api/plugins/telemetry/" + savedDevice.getId() + "/SHARED_SCOPE", payload, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(payload.getBytes(), callback.getPayloadBytes());
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
    }

}
