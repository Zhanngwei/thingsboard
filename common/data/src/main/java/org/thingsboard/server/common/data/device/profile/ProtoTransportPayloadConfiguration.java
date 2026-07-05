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
package org.thingsboard.server.common.data.device.profile;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.Location;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;

/**
 * 中文说明：
 * 1. 类目的：`ProtoTransportPayloadConfiguration` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Slf4j
@Data
public class ProtoTransportPayloadConfiguration implements TransportPayloadTypeConfiguration {

    public static final Location LOCATION = new Location("", "", -1, -1);
    /**
     * Protobuf常量，用于统一引用固定值。
     */
    public static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    public static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_RESPONSE_PROTO_SCHEMA = "rpc response proto schema";
    public static final String RPC_REQUEST_PROTO_SCHEMA = "rpc request proto schema";
    /**
     * Protobuf常量，用于统一引用固定值。
     */
    private static final String PROTO_3_SYNTAX = "proto3";

    /**
     * 设备对象，用于描述当前业务场景。
     */
    private String deviceTelemetryProtoSchema;
    private String deviceAttributesProtoSchema;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private String deviceRpcRequestProtoSchema;
    private String deviceRpcResponseProtoSchema;

    /**
     * 是否使用 JSON 载荷格式。
     */
    private boolean enableCompatibilityWithJsonPayloadFormat;
    private boolean useJsonPayloadFormatForDefaultDownlinkTopics;

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TransportPayloadType getTransportPayloadType() {
        return TransportPayloadType.PROTOBUF;
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `deviceTelemetryProtoSchema`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Descriptors.Descriptor getTelemetryDynamicMessageDescriptor(String deviceTelemetryProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceTelemetryProtoSchema, TELEMETRY_PROTO_SCHEMA);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `deviceAttributesProtoSchema`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Descriptors.Descriptor getAttributesDynamicMessageDescriptor(String deviceAttributesProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceAttributesProtoSchema, ATTRIBUTES_PROTO_SCHEMA);
    }

    /**
     * 功能：获取RPC。
     * 参数：
     * - `deviceRpcResponseProtoSchema`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Descriptors.Descriptor getRpcResponseDynamicMessageDescriptor(String deviceRpcResponseProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceRpcResponseProtoSchema, RPC_RESPONSE_PROTO_SCHEMA);
    }

    /**
     * 功能：获取RPC。
     * 参数：
     * - `deviceRpcRequestProtoSchema`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public DynamicMessage.Builder getRpcRequestDynamicMessageBuilder(String deviceRpcRequestProtoSchema) {
        return DynamicProtoUtils.getDynamicMessageBuilder(deviceRpcRequestProtoSchema, RPC_REQUEST_PROTO_SCHEMA);
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeviceRpcResponseProtoSchema() {
        if (StringUtils.isNotEmpty(deviceRpcResponseProtoSchema)) {
            return deviceRpcResponseProtoSchema;
        } else {
            return "syntax =\"proto3\";\n" +
                    "package rpc;\n" +
                    "\n" +
                    "message RpcResponseMsg {\n" +
                    "  optional string payload = 1;\n" +
                    "}";
        }
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeviceRpcRequestProtoSchema() {
        if (StringUtils.isNotEmpty(deviceRpcRequestProtoSchema)) {
            return deviceRpcRequestProtoSchema;
        } else {
            return "syntax =\"proto3\";\n" +
                    "package rpc;\n" +
                    "\n" +
                    "message RpcRequestMsg {\n" +
                    "  optional string method = 1;\n" +
                    "  optional int32 requestId = 2;\n" +
                    "  optional string params = 3;\n" +
                    "}";
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ProtoTransportPayloadConfiguration` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
