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
package org.thingsboard.server.transport.mqtt.mqttv3.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_CLAIM_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CLAIM_TOPIC;

@Slf4j
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`MqttClaimDeviceTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class MqttClaimDeviceTest extends AbstractMqttIntegrationTest {

    /**
     * 字段说明：
     * 1. 保存 `CUSTOMER_USER_PASSWORD` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String CUSTOMER_USER_PASSWORD = "customerUser123!";

    /**
     * 字段说明：
     * 1. 保存 `customerAdmin` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected User customerAdmin;
    protected Customer savedCustomer;

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
                .deviceName("Test Claim device")
                .gatewayName("Test Claim gateway")
                .build();
        processBeforeTest(configProperties);
        createCustomerAndUser();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createCustomerAndUser` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void createCustomerAndUser() throws Exception {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test Claiming Customer");
        savedCustomer = doPost("/api/customer", customer, Customer.class);
        assertNotNull(savedCustomer);
        assertEquals(tenantId, savedCustomer.getTenantId());

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(savedCustomer.getId());
        user.setEmail("customer@thingsboard.org");

        customerAdmin = createUser(user, CUSTOMER_USER_PASSWORD);
        assertNotNull(customerAdmin);
        assertEquals(customerAdmin.getCustomerId(), savedCustomer.getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClaimingDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice(false);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClaimingDeviceWithoutSecretAndDuration` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice(true);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGatewayClaimingDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGatewayClaimingDevice() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device", false);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGatewayClaimingDeviceWithoutSecretAndDuration` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGatewayClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload", true);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `processTestClaimingDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processTestClaimingDevice(boolean emptyPayload) throws Exception {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        byte[] payloadBytes;
        byte[] failurePayloadBytes;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (emptyPayload) {
            payloadBytes = "{}".getBytes();
            failurePayloadBytes = "{\"durationMs\":1}".getBytes();
        } else {
            payloadBytes = "{\"secretKey\":\"value\", \"durationMs\":60000}".getBytes();
            failurePayloadBytes = "{\"secretKey\":\"value\", \"durationMs\":1}".getBytes();
        }
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateClaimResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateClaimResponse(boolean emptyPayload, MqttTestClient client, byte[] payloadBytes, byte[] failurePayloadBytes) throws Exception {
        client.publishAndWait(DEVICE_CLAIM_TOPIC, failurePayloadBytes);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest()),
                20,
                100
        );

        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publishAndWait(DEVICE_CLAIM_TOPIC, payloadBytes);
        client.disconnect();

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateGatewayClaimResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateGatewayClaimResponse(String deviceName, boolean emptyPayload, MqttTestClient client, byte[] failurePayloadBytes, byte[] payloadBytes) throws Exception {
        client.publishAndWait(GATEWAY_CLAIM_TOPIC, failurePayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100
        );

        assertNotNull(savedDevice);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publishAndWait(GATEWAY_CLAIM_TOPIC, payloadBytes);
        client.disconnect();

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );

        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processTestGatewayClaimingDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        String failurePayload;
        String payload;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (emptyPayload) {
            failurePayload = "{\"" + deviceName + "\": " + "{\"durationMs\":1}" + "}";
            payload = "{\"" + deviceName + "\": " + "{}" + "}";
        } else {
            failurePayload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":1}" + "}";
            payload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":60000}" + "}";
        }
        payloadBytes = payload.getBytes();
        failurePayloadBytes = failurePayload.getBytes();
        validateGatewayClaimResponse(deviceName, emptyPayload, client, failurePayloadBytes, payloadBytes);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processProtoTestGatewayClaimDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processProtoTestGatewayClaimDevice(String deviceName, boolean emptyPayload) throws Exception {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (emptyPayload) {
            payloadBytes = getGatewayClaimMsg(deviceName, 0, emptyPayload).toByteArray();
        } else {
            payloadBytes = getGatewayClaimMsg(deviceName, 60000, emptyPayload).toByteArray();
        }
        failurePayloadBytes = getGatewayClaimMsg(deviceName, 1, emptyPayload).toByteArray();

        validateGatewayClaimResponse(deviceName, emptyPayload, client, failurePayloadBytes, payloadBytes);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getGatewayClaimMsg` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TransportApiProtos.GatewayClaimMsg getGatewayClaimMsg(String deviceName, long duration, boolean emptyPayload) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportApiProtos.GatewayClaimMsg.Builder gatewayClaimMsgBuilder = TransportApiProtos.GatewayClaimMsg.newBuilder();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportApiProtos.ClaimDeviceMsg.Builder claimDeviceMsgBuilder = TransportApiProtos.ClaimDeviceMsg.newBuilder();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (!emptyPayload) {
            claimDeviceBuilder.setSecretKey("value");
        }
        if (duration > 0) {
            claimDeviceBuilder.setDurationMs(duration);
        }
        TransportApiProtos.ClaimDevice claimDevice = claimDeviceBuilder.build();
        claimDeviceMsgBuilder.setClaimRequest(claimDevice);
        claimDeviceMsgBuilder.setDeviceName(deviceName);
        TransportApiProtos.ClaimDeviceMsg claimDeviceMsg = claimDeviceMsgBuilder.build();
        gatewayClaimMsgBuilder.addMsg(claimDeviceMsg);
        return gatewayClaimMsgBuilder.build();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`MqttClaimDeviceTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
