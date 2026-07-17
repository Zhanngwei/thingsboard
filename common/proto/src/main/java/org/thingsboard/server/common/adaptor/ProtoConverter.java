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
package org.thingsboard.server.common.adaptor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 中文说明：
 * 1. `ProtoConverter` 是 ThingsBoard Common 中转换 `Proto` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 它直接协作于源模型、目标模型和相关编解码类型。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
 */
@Slf4j
public class ProtoConverter {

    public static final Gson GSON = new Gson();
    public static final JsonParser JSON_PARSER = new JsonParser();

    /**
     * 功能：转换遥测。
     * 参数：
     * - `payload`：`payload` 参数。
     * 返回：处理结果。
     */
    public static TransportProtos.PostTelemetryMsg convertToTelemetryProto(byte[] payload) throws InvalidProtocolBufferException, IllegalArgumentException {
        TransportProtos.TsKvListProto protoPayload = TransportProtos.TsKvListProto.parseFrom(payload);
        TransportProtos.PostTelemetryMsg.Builder postTelemetryMsgBuilder = TransportProtos.PostTelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto tsKvListProto = validateTsKvListProto(protoPayload);
        postTelemetryMsgBuilder.addTsKvList(tsKvListProto);
        return postTelemetryMsgBuilder.build();
    }

    /**
     * 功能：校验遥测。
     * 参数：
     * - `payload`：`payload` 参数。
     * 返回：判断结果。
     */
    public static TransportProtos.PostTelemetryMsg validatePostTelemetryMsg(byte[] payload) throws InvalidProtocolBufferException, IllegalArgumentException {
        TransportProtos.PostTelemetryMsg msg = TransportProtos.PostTelemetryMsg.parseFrom(payload);
        TransportProtos.PostTelemetryMsg.Builder postTelemetryMsgBuilder = TransportProtos.PostTelemetryMsg.newBuilder();
        List<TransportProtos.TsKvListProto> tsKvListProtoList = msg.getTsKvListList();
        if (!CollectionUtils.isEmpty(tsKvListProtoList)) {
            List<TransportProtos.TsKvListProto> tsKvListProtos = new ArrayList<>();
            tsKvListProtoList.forEach(tsKvListProto -> {
                TransportProtos.TsKvListProto transportTsKvListProto = validateTsKvListProto(tsKvListProto);
                tsKvListProtos.add(transportTsKvListProto);
            });
            postTelemetryMsgBuilder.addAllTsKvList(tsKvListProtos);
            return postTelemetryMsgBuilder.build();
        } else {
            throw new IllegalArgumentException("TsKv list is empty!");
        }
    }

    /**
     * 功能：校验属性。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    public static TransportProtos.PostAttributeMsg validatePostAttributeMsg(TransportProtos.PostAttributeMsg msg) throws IllegalArgumentException, InvalidProtocolBufferException {
        if (!CollectionUtils.isEmpty(msg.getKvList())) {
            byte[] bytes = msg.toByteArray();
            TransportProtos.PostAttributeMsg proto = TransportProtos.PostAttributeMsg.parseFrom(bytes);
            List<TransportProtos.KeyValueProto> kvList = proto.getKvList();
            List<TransportProtos.KeyValueProto> keyValueProtos = validateKeyValueProtos(kvList);
            TransportProtos.PostAttributeMsg.Builder result = TransportProtos.PostAttributeMsg.newBuilder();
            result.addAllKv(keyValueProtos);
            result.setShared(msg.getShared());
            return result.build();
        } else {
            throw new IllegalArgumentException("KeyValue list is empty!");
        }
    }

    /**
     * 功能：转换设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `bytes`：`bytes` 参数。
     * 返回：处理结果。
     */
    public static TransportProtos.ClaimDeviceMsg convertToClaimDeviceProto(DeviceId deviceId, byte[] bytes) throws InvalidProtocolBufferException {
        if (bytes == null) {
            return buildClaimDeviceMsg(deviceId, DataConstants.DEFAULT_SECRET_KEY, 0);
        }
        TransportApiProtos.ClaimDevice proto = TransportApiProtos.ClaimDevice.parseFrom(bytes);
        String secretKey = proto.getSecretKey() != null ? proto.getSecretKey() : DataConstants.DEFAULT_SECRET_KEY;
        long durationMs = proto.getDurationMs();
        return buildClaimDeviceMsg(deviceId, secretKey, durationMs);
    }

    /**
     * 功能：转换属性。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * - `requestId`：请求ID。
     * 返回：处理结果。
     */
    public static TransportProtos.GetAttributeRequestMsg convertToGetAttributeRequestMessage(byte[] bytes, int requestId) throws InvalidProtocolBufferException, RuntimeException {
        TransportApiProtos.AttributesRequest proto = TransportApiProtos.AttributesRequest.parseFrom(bytes);
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        result.setRequestId(requestId);
        String clientKeys = proto.getClientKeys();
        String sharedKeys = proto.getSharedKeys();
        if (!StringUtils.isEmpty(clientKeys)) {
            List<String> clientKeysList = Arrays.asList(clientKeys.split(","));
            result.addAllClientAttributeNames(clientKeysList);
        }
        if (!StringUtils.isEmpty(sharedKeys)) {
            List<String> sharedKeysList = Arrays.asList(sharedKeys.split(","));
            result.addAllSharedAttributeNames(sharedKeysList);
        }
        return result.build();
    }

    /**
     * 功能：转换RPC。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * - `requestId`：请求ID。
     * 返回：处理结果。
     */
    public static TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(byte[] bytes, int requestId) throws InvalidProtocolBufferException {
        TransportApiProtos.RpcRequest proto = TransportApiProtos.RpcRequest.parseFrom(bytes);
        String method = proto.getMethod();
        String params = proto.getParams();
        return TransportProtos.ToServerRpcRequestMsg.newBuilder().setRequestId(requestId).setMethodName(method).setParams(params).build();
    }

    /**
     * 功能：构建设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `secretKey`：键。
     * - `durationMs`：`durationMs` 参数。
     * 返回：处理结果。
     */
    private static TransportProtos.ClaimDeviceMsg buildClaimDeviceMsg(DeviceId deviceId, String secretKey, long durationMs) {
        TransportProtos.ClaimDeviceMsg.Builder result = TransportProtos.ClaimDeviceMsg.newBuilder();
        return result
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setSecretKey(secretKey)
                .setDurationMs(durationMs)
                .build();
    }

    /**
     * 功能：校验时间戳。
     * 参数：
     * - `tsKvListProto`：数据列表。
     * 返回：判断结果。
     */
    private static TransportProtos.TsKvListProto validateTsKvListProto(TransportProtos.TsKvListProto tsKvListProto) {
        TransportProtos.TsKvListProto.Builder tsKvListBuilder = TransportProtos.TsKvListProto.newBuilder();
        long ts = tsKvListProto.getTs();
        if (ts == 0) {
            ts = System.currentTimeMillis();
        }
        tsKvListBuilder.setTs(ts);
        List<TransportProtos.KeyValueProto> kvList = tsKvListProto.getKvList();
        if (!CollectionUtils.isEmpty(kvList)) {
            List<TransportProtos.KeyValueProto> keyValueListProtos = validateKeyValueProtos(kvList);
            tsKvListBuilder.addAllKv(keyValueListProtos);
            return tsKvListBuilder.build();
        } else {
            throw new IllegalArgumentException("KeyValue list is empty!");
        }
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * 返回：处理结果。
     */
    public static TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(byte[] bytes) throws InvalidProtocolBufferException {
        return TransportProtos.ProvisionDeviceRequestMsg.parseFrom(bytes);
    }

    /**
     * 功能：校验键。
     * 参数：
     * - `kvList`：数据列表。
     * 返回：判断结果。
     */
    private static List<TransportProtos.KeyValueProto> validateKeyValueProtos(List<TransportProtos.KeyValueProto> kvList) {
        kvList.forEach(keyValueProto -> {
            String key = keyValueProto.getKey();
            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("Invalid key value: " + key + "!");
            }
            TransportProtos.KeyValueType type = keyValueProto.getType();
            switch (type) {
                case BOOLEAN_V:
                case LONG_V:
                case DOUBLE_V:
                    break;
                case STRING_V:
                    if (StringUtils.isEmpty(keyValueProto.getStringV())) {
                        throw new IllegalArgumentException("Value is empty for key: " + key + "!");
                    }
                    break;
                case JSON_V:
                    try {
                        JSON_PARSER.parse(keyValueProto.getJsonV());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Can't parse value: " + keyValueProto.getJsonV() + " for key: " + key + "!");
                    }
                    break;
                case UNRECOGNIZED:
                    throw new IllegalArgumentException("Unsupported keyValueType: " + type + "!");
            }
        });
        return kvList;
    }

    /**
     * 功能：转换RPC。
     * 参数：
     * - `toDeviceRpcRequestMsg`：设备信息或设备标识。
     * - `rpcRequestDynamicMessageBuilder`：请求对象。
     * 返回：处理结果。
     */
    public static byte[] convertToRpcRequest(TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException {
        rpcRequestDynamicMessageBuilder = rpcRequestDynamicMessageBuilder.getDefaultInstanceForType().newBuilderForType();
        JsonObject rpcRequestJson = new JsonObject();
        rpcRequestJson.addProperty("method", toDeviceRpcRequestMsg.getMethodName());
        rpcRequestJson.addProperty("requestId", toDeviceRpcRequestMsg.getRequestId());
        String params = toDeviceRpcRequestMsg.getParams();
        try {
            JsonElement paramsElement = JSON_PARSER.parse(params);
            rpcRequestJson.add("params", paramsElement);
            DynamicMessage dynamicRpcRequest = DynamicProtoUtils.jsonToDynamicMessage(rpcRequestDynamicMessageBuilder, GSON.toJson(rpcRequestJson));
            return dynamicRpcRequest.toByteArray();
        } catch (Exception e) {
            throw new AdaptorException("Failed to convert ToDeviceRpcRequestMsg to Dynamic Rpc request message due to: ", e);
        }
    }

    /**
     * 功能：校验`Descriptor`。
     * 参数：
     * - `descriptor`：`descriptor` 参数。
     * 返回：判断结果。
     */
    public static Descriptors.Descriptor validateDescriptor(Descriptors.Descriptor descriptor) throws AdaptorException {
        if (descriptor == null) {
            throw new AdaptorException("Failed to get dynamic message descriptor!");
        }
        return descriptor;
    }

    /**
     * 功能：执行 `dynamicMsgToJson` 对应的处理。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * - `descriptor`：`descriptor` 参数。
     * 返回：文本结果。
     */
    public static String dynamicMsgToJson(byte[] bytes, Descriptors.Descriptor descriptor) throws InvalidProtocolBufferException {
        return DynamicProtoUtils.dynamicMsgToJson(descriptor, bytes);
    }

}
