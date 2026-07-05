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
package org.thingsboard.server.transport;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. 类目的：`AbstractTransportIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public abstract class AbstractTransportIntegrationTest extends AbstractControllerTest {

    /**
     * 超时时间常量，用于统一引用固定值。
     */
    protected static final int DEFAULT_WAIT_TIMEOUT_SECONDS = 30;

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    protected static final String MQTT_URL = "tcp://localhost:1883";
    protected static final String COAP_BASE_URL = "coap://localhost:5683/api/v1/";

    protected static final AtomicInteger atomicInteger = new AtomicInteger(2);

    public static final String DEVICE_TELEMETRY_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostTelemetry {\n" +
            "  optional string key1 = 1;\n" +
            "  optional bool key2 = 2;\n" +
            "  optional double key3 = 3;\n" +
            "  optional int32 key4 = 4;\n" +
            "  JsonObject key5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    optional int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    optional NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       optional string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    public static final String DEVICE_ATTRIBUTES_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  optional string key1 = 1;\n" +
            "  optional bool key2 = 2;\n" +
            "  optional double key3 = 3;\n" +
            "  optional int32 key4 = 4;\n" +
            "  JsonObject key5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    optional int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       optional string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    public static final String DEVICE_RPC_RESPONSE_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcResponseMsg {\n" +
            "  optional string payload = 1;\n" +
            "}";

    public static final String DEVICE_RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  optional string method = 1;\n" +
            "  optional int32 requestId = 2;\n" +
            "  optional string params = 3;\n" +
            "}";

    /**
     * 设备对象，用于描述当前业务场景。
     */
    protected Device savedDevice;
    protected String accessToken;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    protected DeviceProfile deviceProfile;

    /**
     * 功能：获取`Kv Protos`。
     * 参数：
     * - `expectedKeys`：键。
     * 返回：匹配的数据集合。
     */
    protected List<TransportProtos.KeyValueProto> getKvProtos(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> keyValueProtos = new ArrayList<>();
        TransportProtos.KeyValueProto strKeyValueProto = getKeyValueProto(expectedKeys.get(0), "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.KeyValueProto boolKeyValueProto = getKeyValueProto(expectedKeys.get(1), "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.KeyValueProto dblKeyValueProto = getKeyValueProto(expectedKeys.get(2), "3.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.KeyValueProto longKeyValueProto = getKeyValueProto(expectedKeys.get(3), "4", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.KeyValueProto jsonKeyValueProto = getKeyValueProto(expectedKeys.get(4), "{\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}", TransportProtos.KeyValueType.JSON_V);
        keyValueProtos.add(strKeyValueProto);
        keyValueProtos.add(boolKeyValueProto);
        keyValueProtos.add(dblKeyValueProto);
        keyValueProtos.add(longKeyValueProto);
        keyValueProtos.add(jsonKeyValueProto);
        return keyValueProtos;
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `key`：键。
     * - `strValue`：值。
     * - `type`：类型。
     * 返回：处理结果。
     */
    protected TransportProtos.KeyValueProto getKeyValueProto(String key, String strValue, TransportProtos.KeyValueType type) {
        TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
        keyValueProtoBuilder.setKey(key);
        keyValueProtoBuilder.setType(type);
        switch (type) {
            case BOOLEAN_V:
                keyValueProtoBuilder.setBoolV(Boolean.parseBoolean(strValue));
                break;
            case LONG_V:
                keyValueProtoBuilder.setLongV(Long.parseLong(strValue));
                break;
            case DOUBLE_V:
                keyValueProtoBuilder.setDoubleV(Double.parseDouble(strValue));
                break;
            case STRING_V:
                keyValueProtoBuilder.setStringV(strValue);
                break;
            case JSON_V:
                keyValueProtoBuilder.setJsonV(strValue);
                break;
        }
        return keyValueProtoBuilder.build();
    }

    /**
     * 功能：执行 `doExecuteWithRetriesAndInterval` 对应的处理。
     * 参数：
     * - `supplier`：`supplier` 参数。
     * - `retries`：`retries` 参数。
     * - `intervalMs`：`intervalMs` 参数。
     * 返回：处理结果。
     */
    protected <T> T doExecuteWithRetriesAndInterval(SupplierWithThrowable<T> supplier, int retries, int intervalMs) throws Exception {
        int count = 0;
        T result = null;
        Throwable lastException = null;
        while (count < retries) {
            try {
                result = supplier.get();
                if (result != null) {
                    return result;
                }
            } catch (Throwable e) {
                lastException = e;
            }
            count++;
            if (count < retries) {
                Thread.sleep(intervalMs);
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        } else {
            return result;
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`SupplierWithThrowable` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    @FunctionalInterface
    public interface SupplierWithThrowable<T> {
        /**
         * 功能：执行 `get` 对应的处理。
         * 参数：无。
         * 返回：处理结果。
         */
        T get() throws Throwable;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractTransportIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
