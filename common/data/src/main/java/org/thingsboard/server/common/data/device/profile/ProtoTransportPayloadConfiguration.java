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
 * 1. `ProtoTransportPayloadConfiguration` 是 ThingsBoard Common Data 中描述传输层行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `TransportPayloadTypeConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
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
