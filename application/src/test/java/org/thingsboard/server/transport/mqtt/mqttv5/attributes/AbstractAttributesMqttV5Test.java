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
package org.thingsboard.server.transport.mqtt.mqttv5.attributes;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Before;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.AbstractMqttV5Test;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`AbstractAttributesMqttV5Test` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractAttributesMqttV5Test extends AbstractMqttV5Test {

    private static final String SHARED_ATTRIBUTES_PAYLOAD = "{\"sharedStr\":\"value1\",\"sharedBool\":true,\"sharedDbl\":42.0,\"sharedLong\":73," +
            "\"sharedJson\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    /**
     * 字段说明：
     * 1. 保存 `SHARED_ATTRIBUTES_DELETED_RESPONSE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String SHARED_ATTRIBUTES_DELETED_RESPONSE = "{\"deleted\":[\"sharedJson\"]}";

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
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
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .build();
        processBeforeTest(configProperties);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processAttributesPublishTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processAttributesPublishTest() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        client.publishAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, PAYLOAD_VALUES_STR.getBytes());
        client.disconnectAndWait();

        DeviceId deviceId = savedDevice.getId();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {
            });
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
        List<Map<String, Object>> values = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {
        });
        assertAttributesValues(values, actualKeySet);
        String deleteAttributesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processAttributesUpdatesTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processAttributesUpdatesTest() throws Exception {

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttV5TestCallback onUpdateCallback = new MqttV5TestCallback();
        client.setCallback(onUpdateCallback);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        client.subscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.ATTRIBUTES, 1);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getSubscribeLatch().await(3, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(onUpdateCallback, SHARED_ATTRIBUTES_PAYLOAD);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttV5TestCallback onDeleteCallback = new MqttV5TestCallback();
        client.setCallback(onDeleteCallback);
        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        onDeleteCallback.getSubscribeLatch().await(3, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(onDeleteCallback, SHARED_ATTRIBUTES_DELETED_RESPONSE);

        client.disconnect();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getAttributesValuesUrl` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertAttributesValues` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void assertAttributesValues(List<Map<String, Object>> deviceValues, Set<String> keySet) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Map<String, Object> map : deviceValues) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(keySet.contains(key));
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
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
     * 方法说明：
     * 1. 职责：执行 `validateUpdateAttributesResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateUpdateAttributesResponse(MqttV5TestCallback callback, String expectedResponse) {
        assertNotNull(callback.getPayloadBytes());
        assertEquals(JacksonUtil.toJsonNode(expectedResponse), JacksonUtil.fromBytes(callback.getPayloadBytes()));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractAttributesMqttV5Test` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
