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
package org.thingsboard.server.transport.coap.attributes;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestCallback;
import org.thingsboard.server.transport.coap.CoapTestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractCoapAttributesIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractCoapAttributesIntegrationTest extends AbstractCoapIntegrationTest {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `defaultTransportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    DefaultTransportService defaultTransportService;

    public static final String ATTRIBUTES_SCHEMA_STR = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  string clientStr = 1;\n" +
            "  bool clientBool = 2;\n" +
            "  double clientDbl = 3;\n" +
            "  int32 clientLong = 4;\n" +
            "  JsonObject clientJson = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String CLIENT_ATTRIBUTES_PAYLOAD = "{\"clientStr\":\"value1\",\"clientBool\":true,\"clientDbl\":42.0,\"clientLong\":73," +
            "\"clientJson\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    private static final String SHARED_ATTRIBUTES_PAYLOAD = "{\"sharedStr\":\"value1\",\"sharedBool\":true,\"sharedDbl\":42.0,\"sharedLong\":73," +
            "\"sharedJson\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    protected static final String SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION = "{\"sharedStr\":\"value\",\"sharedBool\":false,\"sharedDbl\":41.0,\"sharedLong\":72," +
            "\"sharedJson\":{\"someNumber\":41,\"someArray\":[],\"someNestedObject\":{\"key\":\"value\"}}}";

    /**
     * 字段说明：
     * 1. 保存 `SHARED_ATTRIBUTES_DELETED_RESPONSE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String SHARED_ATTRIBUTES_DELETED_RESPONSE = "{\"deleted\":[\"sharedJson\"]}";

    /**
     * 方法说明：
     * 1. 职责：执行 `getTsKvProtoList` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<TransportProtos.TsKvProto> getTsKvProtoList(String attributePrefix) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto(attributePrefix + "Str", "value1", TransportProtos.KeyValueType.STRING_V);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto(attributePrefix + "Bool", "true", TransportProtos.KeyValueType.BOOLEAN_V);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto(attributePrefix + "Dbl", "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto(attributePrefix + "Long", "73", TransportProtos.KeyValueType.LONG_V);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto(attributePrefix + "Json", "{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        List<TransportProtos.TsKvProto> tsKvProtoList = new ArrayList<>();
        tsKvProtoList.add(tsKvProtoAttribute1);
        tsKvProtoList.add(tsKvProtoAttribute2);
        tsKvProtoList.add(tsKvProtoAttribute3);
        tsKvProtoList.add(tsKvProtoAttribute4);
        tsKvProtoList.add(tsKvProtoAttribute5);
        return tsKvProtoList;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTsKvProto` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected TransportProtos.TsKvProto getTsKvProto(String key, String value, TransportProtos.KeyValueType keyValueType) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.TsKvProto.Builder tsKvProtoBuilder = TransportProtos.TsKvProto.newBuilder();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.KeyValueProto keyValueProto = getKeyValueProto(key, value, keyValueType);
        tsKvProtoBuilder.setKv(keyValueProto);
        return tsKvProtoBuilder.build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntityKeys` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<EntityKey> getEntityKeys(List<String> keys, EntityKeyType scope) {
        return keys.stream().map(key -> new EntityKey(scope, key)).collect(Collectors.toList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getAttributesProtoPayloadBytes` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private byte[] getAttributesProtoPayloadBytes() {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        CoapDeviceProfileTransportConfiguration coapTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema());
        DynamicSchema attributesSchema = DynamicProtoUtils.getDynamicSchema(protoFileElement, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);

        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("clientStr"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("clientBool"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientDbl"), 42.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientLong"), 73)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientJson"), jsonObject)
                .build();
        return postAttributesMsg.toByteArray();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processJsonTestRequestAttributesValuesFromTheServer` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processJsonTestRequestAttributesValuesFromTheServer() throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(savedDevice.getId());
        String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";
        String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        List<String> clientKeysList = List.of(clientKeysStr.split(","));
        List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE",
                SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        CoapResponse coapResponse = client.postMethod(CLIENT_ATTRIBUTES_PAYLOAD);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        String featureTokenUrl = CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + clientKeysStr + "&sharedKeys=" + sharedKeysStr;
        client.setURI(featureTokenUrl);
        validateJsonResponse(client.getMethod());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processProtoTestRequestAttributesValuesFromTheServer` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processProtoTestRequestAttributesValuesFromTheServer() throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(savedDevice.getId());
        String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";
        String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        List<String> clientKeysList = List.of(clientKeysStr.split(","));
        List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE",
                SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        CoapResponse coapResponse = client.postMethod(getAttributesProtoPayloadBytes());
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        String featureTokenUrl = CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + clientKeysStr + "&sharedKeys=" + sharedKeysStr;
        client.setURI(featureTokenUrl);
        validateProtoResponse(client.getMethod());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processJsonTestSubscribeToAttributesUpdates` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processJsonTestSubscribeToAttributesUpdates(boolean emptyCurrentStateNotification) throws Exception {
        if (!emptyCurrentStateNotification) {
            doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION, String.class, status().isOk());
        }

        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        CoapTestCallback callbackCoap = new CoapTestCallback();

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        String awaitAlias = "await Json Test Subscribe To AttributesUpdates (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve().intValue());
        if (emptyCurrentStateNotification) {
            validateUpdateAttributesJsonResponse(callbackCoap, "{}");
        } else {
            validateUpdateAttributesJsonResponse(callbackCoap, SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION);
        }

        int expectedObserveForAttributesUpdate = callbackCoap.getObserve().intValue() + 1;
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        awaitAlias = "await Json Test Subscribe To AttributesUpdates (add attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesUpdate == callbackCoap.getObserve().intValue());
        validateUpdateAttributesJsonResponse(callbackCoap, SHARED_ATTRIBUTES_PAYLOAD);

        int expectedObserveForAttributesDelete = callbackCoap.getObserve().intValue() + 1;
        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        awaitAlias = "await Json Test Subscribe To AttributesUpdates (deleted attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesDelete == callbackCoap.getObserve().intValue());
        validateUpdateAttributesJsonResponse(callbackCoap, SHARED_ATTRIBUTES_DELETED_RESPONSE);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());

        awaitClientAfterCancelObserve();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processProtoTestSubscribeToAttributesUpdates` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processProtoTestSubscribeToAttributesUpdates(boolean emptyCurrentStateNotification) throws Exception {
        if (!emptyCurrentStateNotification) {
            doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION, String.class, status().isOk());
        }

        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        CoapTestCallback callbackCoap = new CoapTestCallback();

        String awaitAlias = "await Proto Test Subscribe To Attributes Updates (add attributes)";
        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve().intValue());

        if (emptyCurrentStateNotification) {
            validateEmptyCurrentStateAttributesProtoResponse(callbackCoap);
        } else {
            validateCurrentStateAttributesProtoResponse(callbackCoap);
        }

        int expectedObserveForAttributesUpdate = callbackCoap.getObserve().intValue() + 1;
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        awaitAlias = "await Proto Test Subscribe To Attributes Updates (add attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesUpdate == callbackCoap.getObserve().intValue());
        validateUpdateProtoAttributesResponse(callbackCoap, expectedObserveForAttributesUpdate);

        int expectedObserveForAttributesDelete = callbackCoap.getObserve().intValue() + 1;
        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        awaitAlias = "await Proto Test Subscribe To Attributes Updates (deleted attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesDelete == callbackCoap.getObserve().intValue());
        validateDeleteProtoAttributesResponse(callbackCoap, expectedObserveForAttributesDelete);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());

        awaitClientAfterCancelObserve();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateJsonResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateJsonResponse(CoapResponse getAttributesResponse) throws InvalidProtocolBufferException {
        assertEquals(CoAP.ResponseCode.CONTENT, getAttributesResponse.getCode());
        String expectedResponse = "{\"client\":" + CLIENT_ATTRIBUTES_PAYLOAD + ",\"shared\":" + SHARED_ATTRIBUTES_PAYLOAD + "}";
        assertEquals(JacksonUtil.toJsonNode(expectedResponse), JacksonUtil.fromBytes(getAttributesResponse.getPayload()));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateProtoResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateProtoResponse(CoapResponse getAttributesResponse) throws InterruptedException, InvalidProtocolBufferException {
        TransportProtos.GetAttributeResponseMsg expectedAttributesResponse = getExpectedAttributeResponseMsg();
        TransportProtos.GetAttributeResponseMsg actualAttributesResponse = TransportProtos.GetAttributeResponseMsg.parseFrom(getAttributesResponse.getPayload());
        assertEquals(expectedAttributesResponse.getRequestId(), actualAttributesResponse.getRequestId());
        List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
        assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateUpdateAttributesJsonResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateUpdateAttributesJsonResponse(CoapTestCallback callback, String expectedResponse) {
        assertNotNull(callback.getPayloadBytes());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(expectedResponse), JacksonUtil.toJsonNode(response));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateEmptyCurrentStateAttributesProtoResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateEmptyCurrentStateAttributesProtoResponse(CoapTestCallback callback) throws InvalidProtocolBufferException {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateCurrentStateAttributesProtoResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateCurrentStateAttributesProtoResponse(CoapTestCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder expectedCurrentStateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto("sharedStr", "value", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto("sharedBool", "false", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto("sharedDbl", "41.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto("sharedLong", "72", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto("sharedJson", "{\"someNumber\":41,\"someArray\":[],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
        List<TransportProtos.TsKvProto> tsKvProtoList = new ArrayList<>();
        tsKvProtoList.add(tsKvProtoAttribute1);
        tsKvProtoList.add(tsKvProtoAttribute2);
        tsKvProtoList.add(tsKvProtoAttribute3);
        tsKvProtoList.add(tsKvProtoAttribute4);
        tsKvProtoList.add(tsKvProtoAttribute5);
        TransportProtos.AttributeUpdateNotificationMsg expectedCurrentStateNotificationMsg = expectedCurrentStateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList).build();
        TransportProtos.AttributeUpdateNotificationMsg actualCurrentStateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedCurrentStateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualCurrentStateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateUpdateProtoAttributesResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateUpdateProtoAttributesResponse(CoapTestCallback callback, int expectedObserveCnt) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList("shared");
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateDeleteProtoAttributesResponse` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void validateDeleteProtoAttributesResponse(CoapTestCallback callback, int expectedObserveCnt) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("sharedJson");

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("sharedJson", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `awaitClientAfterCancelObserve` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void awaitClientAfterCancelObserve() {
        Awaitility.await("awaitClientAfterCancelObserve")
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> {
                    log.trace("awaiting defaultTransportService.sessions is empty");
                    return defaultTransportService.sessions.isEmpty();
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getExpectedAttributeResponseMsg` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TransportProtos.GetAttributeResponseMsg getExpectedAttributeResponseMsg() {
        TransportProtos.GetAttributeResponseMsg.Builder result = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> csTsKvProtoList = getTsKvProtoList("client");
        List<TransportProtos.TsKvProto> shTsKvProtoList = getTsKvProtoList("shared");
        result.addAllClientAttributeList(csTsKvProtoList);
        result.addAllSharedAttributeList(shTsKvProtoList);
        result.setRequestId(0);
        return result.build();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractCoapAttributesIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
