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
package org.thingsboard.server.transport.coap.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestCallback;
import org.thingsboard.server.transport.coap.CoapTestClient;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractCoapServerSideRpcIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractCoapServerSideRpcIntegrationTest extends AbstractCoapIntegrationTest {

    public static final  String RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  optional string method = 1;\n" +
            "  optional int32 requestId = 2;\n" +
            "  Params params = 3;\n" +
            "\n" +
            "  message Params {\n" +
            "      optional string pin = 1;\n" +
            "      optional int32 value = 2;\n" +
            "   }\n" +
            "}";

    /**
     * 字段说明：
     * 1. 保存 `DEVICE_RESPONSE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    /**
     * 字段说明：
     * 1. 保存 `asyncContextTimeoutToUseRpcPlugin` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final Long asyncContextTimeoutToUseRpcPlugin = 10000L;

    /**
     * 方法说明：
     * 1. 职责：执行 `processOneWayRpcTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processOneWayRpcTest(boolean protobuf) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.RPC);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client, true, protobuf);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        String awaitAlias = "await One Way Rpc (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.VALID.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null && 0 == callbackCoap.getObserve());
        validateCurrentStateNotification(callbackCoap);
        int expectedObserveAfterRpcProcessed = callbackCoap.getObserve() + 1;
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await One Way Rpc setGpio(method, params, value)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null && expectedObserveAfterRpcProcessed == callbackCoap.getObserve());
        validateOneWayStateChangedNotification(callbackCoap, result);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processTwoWayRpcTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processTwoWayRpcTest(String expectedResponseResult, boolean protobuf) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.RPC);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client, false, protobuf);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
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
        validateTwoWayStateChangedNotification(callbackCoap, expectedResponseResult, actualResult);

        int expectedObserveCountAfterGpioRequest2 = callbackCoap.getObserve() + 1;
        actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) first";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest2 == callbackCoap.getObserve());

        validateTwoWayStateChangedNotification(callbackCoap, expectedResponseResult, actualResult);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processOnLoadResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `processOnLoadProtoResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processOnLoadProtoResponse(CoapResponse response, CoapTestClient client) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = getProtoTransportPayloadConfiguration();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        ProtoFileElement rpcRequestProtoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        DynamicSchema rpcRequestProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcRequestProtoFileElement, ProtoTransportPayloadConfiguration.RPC_REQUEST_PROTO_SCHEMA);

        byte[] requestPayload = response.getPayload();
        DynamicMessage.Builder rpcRequestMsg = rpcRequestProtoSchema.newMessageBuilder("RpcRequestMsg");
        Descriptors.Descriptor rpcRequestMsgDescriptor = rpcRequestMsg.getDescriptorForType();
        try {
            DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rpcRequestMsgDescriptor, requestPayload);
            Descriptors.FieldDescriptor requestIdDescriptor = rpcRequestMsgDescriptor.findFieldByName("requestId");
            int requestId = (int) dynamicMessage.getField(requestIdDescriptor);
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            ProtoFileElement rpcResponseProtoSchemaFile = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema());
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            DynamicSchema rpcResponseProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcResponseProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_RESPONSE_PROTO_SCHEMA);
            DynamicMessage.Builder rpcResponseBuilder = rpcResponseProtoSchema.newMessageBuilder("RpcResponseMsg");
            Descriptors.Descriptor rpcResponseMsgDescriptor = rpcResponseBuilder.getDescriptorForType();
            DynamicMessage rpcResponseMsg = rpcResponseBuilder
                    .setField(rpcResponseMsgDescriptor.findFieldByName("payload"), DEVICE_RESPONSE)
                    .build();
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
            }, rpcResponseMsg.toByteArray(), MediaTypeRegistry.APPLICATION_JSON);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (InvalidProtocolBufferException e) {
            log.warn("Command Response Ack Error, Invalid response received: ", e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getProtoTransportPayloadConfiguration` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ProtoTransportPayloadConfiguration getProtoTransportPayloadConfiguration() {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        return (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateCurrentStateNotification` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void validateCurrentStateNotification(CoapTestCallback callback) {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateOneWayStateChangedNotification` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void validateOneWayStateChangedNotification(CoapTestCallback callback, String result) {
        assertTrue(StringUtils.isEmpty(result));
        assertNotNull(callback.getPayloadBytes());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateTwoWayStateChangedNotification` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void validateTwoWayStateChangedNotification(CoapTestCallback callback, String expectedResult, String actualResult) {
        assertEquals(expectedResult, actualResult);
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
         * 字段说明：
         * 1. 保存 `client` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final CoapTestClient client;
        private final boolean isOneWayRpc;
        /**
         * 字段说明：
         * 1. 保存 `protobuf` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final boolean protobuf;

        TestCoapCallbackForRPC(CoapTestClient client, boolean isOneWayRpc, boolean protobuf) {
            this.client = client;
            this.isOneWayRpc = isOneWayRpc;
            this.protobuf = protobuf;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onLoad` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onLoad(CoapResponse response) {
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            observe = response.getOptions().getObserve();
            if (observe != null) {
                if (!isOneWayRpc && observe > 0) {
                    if (!protobuf){
                        processOnLoadResponse(response, client);
                    } else {
                        processOnLoadProtoResponse(response, client);
                    }
                }
            }
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onError` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractCoapServerSideRpcIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
