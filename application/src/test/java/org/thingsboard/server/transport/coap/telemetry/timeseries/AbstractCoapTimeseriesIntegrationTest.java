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
package org.thingsboard.server.transport.coap.telemetry.timeseries;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractCoapTimeseriesIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractCoapTimeseriesIntegrationTest extends AbstractCoapIntegrationTest {

    private static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `beforeTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device")
                .build();
        processBeforeTest(configProperties);
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `afterTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testPushTelemetry` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testPushTelemetry() throws Exception {
        processJsonPayloadTelemetryTest(PAYLOAD_VALUES_STR.getBytes(), false);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testPushTelemetryWithTs` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testPushTelemetryWithTs() throws Exception {
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}";
        processJsonPayloadTelemetryTest(payloadStr.getBytes(), true);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processJsonPayloadTelemetryTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processJsonPayloadTelemetryTest(byte[] payloadBytes, boolean withTs) throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processTestPostTelemetry(payloadBytes, expectedKeys, withTs, false);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processTestPostTelemetry` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processTestPostTelemetry(byte[] payloadBytes, List<String> expectedKeys, boolean withTs, boolean presenceFieldsTest) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.TELEMETRY);
        CoapResponse coapResponse = client.postMethod(payloadBytes);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        DeviceId deviceId = savedDevice.getId();
        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);
        assertEquals(expectedKeySet, actualKeySet);

        String getTelemetryValuesUrl;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (withTs) {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=15000&keys=" + String.join(",", actualKeySet);
        } else {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + String.join(",", actualKeySet);
        }
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;
        Map<String, List<Map<String, Object>>> values = null;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        while (start <= end) {
            values = doGetAsyncTyped(getTelemetryValuesUrl, new TypeReference<>() {});
            boolean valid = values.size() == expectedKeys.size();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (valid) {
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (String key : expectedKeys) {
                    List<Map<String, Object>> tsValues = values.get(key);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (tsValues != null && tsValues.size() > 0) {
                        Object ts = tsValues.get(0).get("ts");
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (valid) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(values);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (withTs) {
            assertTs(values, expectedKeys, 10000, 0);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (presenceFieldsTest) {
            assertExplicitProtoFieldValues(values);
        } else {
            assertValues(values, 0);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertTs` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertTs(Map<String, List<Map<String, Object>>> deviceValues, List<String> expectedKeys, int ts, int arrayIndex) {
        assertEquals(ts, deviceValues.get(expectedKeys.get(0)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(1)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(2)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(3)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(4)).get(arrayIndex).get("ts"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertValues` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertValues(Map<String, List<Map<String, Object>>> deviceValues, int arrayIndex) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(arrayIndex).get("value");
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
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
     * 方法说明：
     * 1. 职责：执行 `assertExplicitProtoFieldValues` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertExplicitProtoFieldValues(Map<String, List<Map<String, Object>>> deviceValues) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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
     * 方法说明：
     * 1. 职责：执行 `getActualKeysList` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

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

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractCoapTimeseriesIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
