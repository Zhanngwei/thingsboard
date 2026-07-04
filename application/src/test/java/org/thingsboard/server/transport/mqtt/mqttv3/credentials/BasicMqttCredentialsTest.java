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
package org.thingsboard.server.transport.mqtt.mqttv3.credentials;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_TOPIC;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`BasicMqttCredentialsTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class BasicMqttCredentialsTest extends AbstractMqttIntegrationTest {

    /**
     * 字段说明：
     * 1. 保存 `CLIENT_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String CLIENT_ID = "ClientId";
    public static final String USER_NAME1 = "UserName1";
    /**
     * 字段说明：
     * 1. 保存 `USER_NAME2` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String USER_NAME2 = "UserName2";
    public static final String USER_NAME3 = "UserName3";
    /**
     * 字段说明：
     * 1. 保存 `PASSWORD` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String PASSWORD = "secret";

    /**
     * 字段说明：
     * 1. 保存 `clientIdDevice` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Device clientIdDevice;
    private Device clientIdAndUserNameDevice1;
    /**
     * 字段说明：
     * 1. 保存 `clientIdAndUserNameAndPasswordDevice2` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Device clientIdAndUserNameAndPasswordDevice2;
    private Device clientIdAndUserNameAndPasswordDevice3;
    /**
     * 字段说明：
     * 1. 保存 `accessTokenDevice` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Device accessTokenDevice;
    private Device accessToken2Device;


    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `before` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void before() throws Exception {
        loginTenantAdmin();

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        BasicMqttCredentials credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        clientIdDevice = createDevice("clientIdDevice", credValue);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME1);
        clientIdAndUserNameDevice1 = createDevice("clientIdAndUserNameDevice", credValue);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME2);
        credValue.setPassword(PASSWORD);
        clientIdAndUserNameAndPasswordDevice2 = createDevice("clientIdAndUserNameAndPasswordDevice", credValue);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME3);
        credValue.setPassword(PASSWORD);
        clientIdAndUserNameAndPasswordDevice3 = createDevice("clientIdAndUserNameAndPasswordDevice2", credValue);

        accessTokenDevice = createDevice("accessTokenDevice", USER_NAME1);
        accessToken2Device = createDevice("accessToken2Device", USER_NAME2);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCorrectCredentials` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCorrectCredentials() throws Exception {
        // Check that correct devices receive telemetry
        MqttTestClient mqttTestClient1 = new MqttTestClient();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        mqttTestClient1.connectAndWait(USER_NAME1);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttTestClient mqttTestClient2 = new MqttTestClient(CLIENT_ID);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        mqttTestClient2.connectAndWait();

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttTestClient mqttTestClient3 = new MqttTestClient(CLIENT_ID);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        mqttTestClient3.connectAndWait(USER_NAME1);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttTestClient mqttTestClient4 = new MqttTestClient(CLIENT_ID);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        mqttTestClient4.connectAndWait(USER_NAME2, PASSWORD);

        // Also correct. Random clientId and password, but matches access token
        MqttTestClient mqttTestClient5 = new MqttTestClient(StringUtils.randomAlphanumeric(10));
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        mqttTestClient5.connectAndWait(USER_NAME2, StringUtils.randomAlphanumeric(10));

        testTelemetryIsDelivered(accessTokenDevice, mqttTestClient1);
        testTelemetryIsDelivered(clientIdDevice, mqttTestClient2);
        testTelemetryIsDelivered(clientIdAndUserNameDevice1, mqttTestClient3);
        testTelemetryIsDelivered(clientIdAndUserNameAndPasswordDevice2, mqttTestClient4);

        // Also correct. Random clientId and password, but matches access token
        testTelemetryIsDelivered(accessToken2Device, mqttTestClient5);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCorrectClientIdAndUserNameButWrongPassword` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCorrectClientIdAndUserNameButWrongPassword() throws Exception {
        // Not correct. Correct clientId and username, but wrong password
        MqttTestClient mqttTestClient = new MqttTestClient(CLIENT_ID);
        try {
            mqttTestClient.connectAndWait(USER_NAME3, "WRONG PASSWORD");
            Assert.fail(); // This should not happens, because we have a wrong password
        } catch (MqttException e) {
            Assert.assertEquals(5, e.getReasonCode()); // 4 - Reason code not authorized in MQTT v3
        }
        Assertions.assertThrows(MqttException.class, () -> {
            testTelemetryIsNotDelivered(clientIdAndUserNameAndPasswordDevice3, mqttTestClient);
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testTelemetryIsDelivered` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testTelemetryIsDelivered(Device device, MqttTestClient client) throws Exception {
        testTelemetryIsDelivered(device, client, true);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testTelemetryIsNotDelivered` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testTelemetryIsNotDelivered(Device device, MqttTestClient client) throws Exception {
        testTelemetryIsDelivered(device, client, false);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testTelemetryIsDelivered` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testTelemetryIsDelivered(Device device, MqttTestClient client, boolean ok) throws Exception {
        String randomKey = StringUtils.randomAlphanumeric(10);
        List<String> expectedKeys = Arrays.asList(randomKey);
        client.publishAndWait(DEVICE_TELEMETRY_TOPIC, JacksonUtil.toString(JacksonUtil.newObjectNode().put(randomKey, true)).getBytes());

        String deviceId = device.getId().getId().toString();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

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
        if (ok) {
            assertNotNull(actualKeys);

            Set<String> actualKeySet = new HashSet<>(actualKeys);
            Set<String> expectedKeySet = new HashSet<>(expectedKeys);

            assertEquals(expectedKeySet, actualKeySet);
        } else {
            assertNull(actualKeys);
        }
        client.disconnect();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Device createDevice(String deviceName, BasicMqttCredentials clientIdCredValue) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        device = doPost("/api/device", device, Device.class);

        DeviceCredentials clientIdCred =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        clientIdCred.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);


        clientIdCred.setCredentialsValue(JacksonUtil.toString(clientIdCredValue));
        doPost("/api/device/credentials", clientIdCred).andExpect(status().isOk());
        return device;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Device createDevice(String deviceName, String accessToken) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        device = doPost("/api/device", device, Device.class);

        DeviceCredentials clientIdCred =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        clientIdCred.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        clientIdCred.setCredentialsId(accessToken);
        doPost("/api/device/credentials", clientIdCred).andExpect(status().isOk());
        return device;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BasicMqttCredentialsTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
