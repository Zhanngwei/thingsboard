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
package org.thingsboard.server.transport.mqtt.mqttv5.timeseries;

import com.fasterxml.jackson.core.type.TypeReference;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.transport.mqtt.mqttv5.AbstractMqttV5Test;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttV5TimeseriesTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractMqttV5TimeseriesTest extends AbstractMqttV5Test {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    /**
     * 功能：处理时序数据。
     * 参数：无。
     * 返回：无。
     */
    protected void processTimeseriesMqttV5UploadTest() throws Exception {

        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");

        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        client.publishAndWait(MqttTopics.DEVICE_TELEMETRY_TOPIC, PAYLOAD_VALUES_STR.getBytes());
        client.disconnect();

        DeviceId deviceId = savedDevice.getId();

        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, actualKeySet);

        String getTelemetryValuesUrl;
        getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + String.join(",", actualKeySet);
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;
        Map<String, List<Map<String, Object>>> values = null;
        while (start <= end) {
            values = doGetAsyncTyped(getTelemetryValuesUrl, new TypeReference<>() {
            });
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
        assertValues(values);
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
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", new TypeReference<>() {
            });
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    /**
     * 功能：执行 `assertValues` 对应的处理。
     * 参数：
     * - `deviceValues`：设备信息或设备标识。
     * 返回：无。
     */
    private void assertValues(Map<String, List<Map<String, Object>>> deviceValues) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(0).get("value");
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
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttV5TimeseriesTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
