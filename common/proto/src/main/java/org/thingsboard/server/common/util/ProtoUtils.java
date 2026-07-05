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
package org.thingsboard.server.common.util;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`ProtoUtils` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public class ProtoUtils {

    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final EntityType[] entityTypeByProtoNumber;

    static {
        int arraySize = Arrays.stream(EntityType.values()).mapToInt(EntityType::getProtoNumber).max().orElse(0);
        entityTypeByProtoNumber = new EntityType[arraySize + 1];
        Arrays.stream(EntityType.values()).forEach(entityType -> entityTypeByProtoNumber[entityType.getProtoNumber()] = entityType);
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public static TransportProtos.ComponentLifecycleMsgProto toProto(ComponentLifecycleMsg msg) {
        return TransportProtos.ComponentLifecycleMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setEntityType(toProto(msg.getEntityId().getEntityType()))
                .setEntityIdMSB(msg.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(msg.getEntityId().getId().getLeastSignificantBits())
                .setEvent(TransportProtos.ComponentLifecycleEvent.forNumber(msg.getEvent().ordinal()))
                .build();
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    public static TransportProtos.EntityTypeProto toProto(EntityType entityType) {
        return TransportProtos.EntityTypeProto.forNumber(entityType.getProtoNumber());
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static ComponentLifecycleMsg fromProto(TransportProtos.ComponentLifecycleMsgProto proto) {
        return new ComponentLifecycleMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                EntityIdFactory.getByTypeAndUuid(fromProto(proto.getEntityType()), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB())),
                ComponentLifecycleEvent.values()[proto.getEventValue()]
        );
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    public static EntityType fromProto(TransportProtos.EntityTypeProto entityType) {
        return entityTypeByProtoNumber[entityType.getNumber()];
    }


    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    public static TransportProtos.ToEdgeSyncRequestMsgProto toProto(ToEdgeSyncRequest request) {
        return TransportProtos.ToEdgeSyncRequestMsgProto.newBuilder()
                .setTenantIdMSB(request.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(request.getTenantId().getId().getLeastSignificantBits())
                .setRequestIdMSB(request.getId().getMostSignificantBits())
                .setRequestIdLSB(request.getId().getLeastSignificantBits())
                .setEdgeIdMSB(request.getEdgeId().getId().getMostSignificantBits())
                .setEdgeIdLSB(request.getEdgeId().getId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static ToEdgeSyncRequest fromProto(TransportProtos.ToEdgeSyncRequestMsgProto proto) {
        return new ToEdgeSyncRequest(
                new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()))
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `response`：响应对象。
     * 返回：处理结果。
     */
    public static TransportProtos.FromEdgeSyncResponseMsgProto toProto(FromEdgeSyncResponse response) {
        return TransportProtos.FromEdgeSyncResponseMsgProto.newBuilder()
                .setTenantIdMSB(response.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(response.getTenantId().getId().getLeastSignificantBits())
                .setResponseIdMSB(response.getId().getMostSignificantBits())
                .setResponseIdLSB(response.getId().getLeastSignificantBits())
                .setEdgeIdMSB(response.getEdgeId().getId().getMostSignificantBits())
                .setEdgeIdLSB(response.getEdgeId().getId().getLeastSignificantBits())
                .setSuccess(response.isSuccess())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static FromEdgeSyncResponse fromProto(TransportProtos.FromEdgeSyncResponseMsgProto proto) {
        return new FromEdgeSyncResponse(
                new UUID(proto.getResponseIdMSB(), proto.getResponseIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB())),
                proto.getSuccess()
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public static TransportProtos.EdgeEventUpdateMsgProto toProto(EdgeEventUpdateMsg msg) {
        return TransportProtos.EdgeEventUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setEdgeIdMSB(msg.getEdgeId().getId().getMostSignificantBits())
                .setEdgeIdLSB(msg.getEdgeId().getId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static EdgeEventUpdateMsg fromProto(TransportProtos.EdgeEventUpdateMsgProto proto) {
        return new EdgeEventUpdateMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()))
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.DeviceEdgeUpdateMsgProto toProto(DeviceEdgeUpdateMsg msg) {
        TransportProtos.DeviceEdgeUpdateMsgProto.Builder builder = TransportProtos.DeviceEdgeUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits());

        if (msg.getEdgeId() != null) {
            builder.setEdgeIdMSB(msg.getEdgeId().getId().getMostSignificantBits())
                    .setEdgeIdLSB(msg.getEdgeId().getId().getLeastSignificantBits());
        }

        return builder.build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static DeviceEdgeUpdateMsg fromProto(TransportProtos.DeviceEdgeUpdateMsgProto proto) {
        EdgeId edgeId = null;
        if (proto.hasEdgeIdMSB() && proto.hasEdgeIdLSB()) {
            edgeId = new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()));
        }
        return new DeviceEdgeUpdateMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                edgeId);
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.DeviceNameOrTypeUpdateMsgProto toProto(DeviceNameOrTypeUpdateMsg msg) {
        return TransportProtos.DeviceNameOrTypeUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceName())
                .setDeviceType(msg.getDeviceType())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static DeviceNameOrTypeUpdateMsg fromProto(TransportProtos.DeviceNameOrTypeUpdateMsgProto proto) {
        return new DeviceNameOrTypeUpdateMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                proto.getDeviceName(),
                proto.getDeviceType()
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.DeviceAttributesEventMsgProto toProto(DeviceAttributesEventNotificationMsg msg) {
        TransportProtos.DeviceAttributesEventMsgProto.Builder builder = TransportProtos.DeviceAttributesEventMsgProto.newBuilder();
        builder.setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setDeleted(msg.isDeleted());

        if (msg.getScope() != null) {
            builder.setScope(TransportProtos.AttributeScopeProto.valueOf(msg.getScope()));
        }

        if (msg.getDeletedKeys() != null) {
            for (AttributeKey key : msg.getDeletedKeys()) {
                builder.addDeletedKeys(TransportProtos.AttributeKey.newBuilder()
                        .setScope(TransportProtos.AttributeScopeProto.valueOf(key.getScope()))
                        .setAttributeKey(key.getAttributeKey())
                        .build());
            }
        }

        if (msg.getValues() != null) {
            for (AttributeKvEntry attributeKvEntry : msg.getValues()) {
                TransportProtos.AttributeValueProto.Builder attributeValueBuilder = TransportProtos.AttributeValueProto.newBuilder()
                        .setLastUpdateTs(attributeKvEntry.getLastUpdateTs())
                        .setKey(attributeKvEntry.getKey());
                switch (attributeKvEntry.getDataType()) {
                    case BOOLEAN:
                        attributeKvEntry.getBooleanValue().ifPresent(attributeValueBuilder::setBoolV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getBooleanValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.BOOLEAN_V);
                        break;
                    case STRING:
                        attributeKvEntry.getStrValue().ifPresent(attributeValueBuilder::setStringV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getStrValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.STRING_V);
                        break;
                    case DOUBLE:
                        attributeKvEntry.getDoubleValue().ifPresent(attributeValueBuilder::setDoubleV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getDoubleValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.DOUBLE_V);
                        break;
                    case LONG:
                        attributeKvEntry.getLongValue().ifPresent(attributeValueBuilder::setLongV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getLongValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                        break;
                    case JSON:
                        attributeKvEntry.getJsonValue().ifPresent(attributeValueBuilder::setJsonV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getJsonValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.JSON_V);
                        break;
                }
                builder.addValues(attributeValueBuilder.build());
            }
        }
        return builder.build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.DeviceAttributesEventMsgProto proto) {
        return new DeviceAttributesEventNotificationMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                getAttributeKeySetFromProto(proto.getDeletedKeysList()),
                proto.hasScope() ? proto.getScope().name() : null,
                getAttributesKvEntryFromProto(proto.getValuesList()),
                proto.getDeleted()
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.DeviceCredentialsUpdateMsgProto toProto(DeviceCredentialsUpdateNotificationMsg msg) {
        TransportProtos.DeviceCredentialsProto.Builder protoBuilder = TransportProtos.DeviceCredentialsProto.newBuilder()
                .setDeviceIdMSB(msg.getDeviceCredentials().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceCredentials().getDeviceId().getId().getLeastSignificantBits())
                .setCredentialsId(msg.getDeviceCredentials().getCredentialsId())
                .setCredentialsType(TransportProtos.CredentialsType.valueOf(msg.getDeviceCredentials().getCredentialsType().name()));

        if (msg.getDeviceCredentials().getCredentialsValue() != null) {
            protoBuilder.setCredentialsValue(msg.getDeviceCredentials().getCredentialsValue());
        }

        return TransportProtos.DeviceCredentialsUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setDeviceCredentials(protoBuilder.build())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.DeviceCredentialsUpdateMsgProto proto) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(new DeviceId(new UUID(proto.getDeviceCredentials().getDeviceIdMSB(), proto.getDeviceCredentials().getDeviceIdLSB())));
        deviceCredentials.setCredentialsId(proto.getDeviceCredentials().getCredentialsId());
        deviceCredentials.setCredentialsValue(proto.getDeviceCredentials().hasCredentialsValue() ? proto.getDeviceCredentials().getCredentialsValue() : null);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(proto.getDeviceCredentials().getCredentialsType().name()));
        return new DeviceCredentialsUpdateNotificationMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                deviceCredentials
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.ToDeviceRpcRequestActorMsgProto toProto(ToDeviceRpcRequestActorMsg msg) {
        TransportProtos.ToDeviceRpcRequestMsg proto = TransportProtos.ToDeviceRpcRequestMsg.newBuilder()
                .setMethodName(msg.getMsg().getBody().getMethod())
                .setParams(msg.getMsg().getBody().getParams())
                .setExpirationTime(msg.getMsg().getExpirationTime())
                .setRequestIdMSB(msg.getMsg().getId().getMostSignificantBits())
                .setRequestIdLSB(msg.getMsg().getId().getLeastSignificantBits())
                .setOneway(msg.getMsg().isOneway())
                .build();

        return TransportProtos.ToDeviceRpcRequestActorMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setServiceId(msg.getServiceId())
                .setToDeviceRpcRequestMsg(proto)
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.ToDeviceRpcRequestActorMsgProto proto) {
        TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg = proto.getToDeviceRpcRequestMsg();
        ToDeviceRpcRequest toDeviceRpcRequest = new ToDeviceRpcRequest(
                new UUID(toDeviceRpcRequestMsg.getRequestIdMSB(), toDeviceRpcRequestMsg.getRequestIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                toDeviceRpcRequestMsg.getOneway(),
                toDeviceRpcRequestMsg.getExpirationTime(),
                new ToDeviceRpcRequestBody(toDeviceRpcRequestMsg.getMethodName(), toDeviceRpcRequestMsg.getParams()),
                toDeviceRpcRequestMsg.getPersisted(), 0, "");
        return new ToDeviceRpcRequestActorMsg(proto.getServiceId(), toDeviceRpcRequest);
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.FromDeviceRpcResponseActorMsgProto toProto(FromDeviceRpcResponseActorMsg msg) {
        TransportProtos.FromDeviceRPCResponseProto.Builder builder = TransportProtos.FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(msg.getMsg().getId().getMostSignificantBits())
                .setRequestIdLSB(msg.getMsg().getId().getLeastSignificantBits())
                .setError(msg.getMsg().getError().isPresent() ? msg.getMsg().getError().get().ordinal() : -1);
        if (msg.getMsg().getResponse().isPresent()) {
            builder.setResponse(msg.getMsg().getResponse().get());
        }

        return TransportProtos.FromDeviceRpcResponseActorMsgProto.newBuilder()
                .setRequestId(msg.getRequestId())
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setRpcResponse(builder.build())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.FromDeviceRpcResponseActorMsgProto proto) {
        FromDeviceRpcResponse fromDeviceRpcResponse = new FromDeviceRpcResponse(
                new UUID(proto.getRpcResponse().getRequestIdMSB(), proto.getRpcResponse().getRequestIdLSB()),
                proto.getRpcResponse().getResponse(),
                proto.getRpcResponse().getError() >= 0 ? RpcError.values()[proto.getRpcResponse().getError()] : null);
        return new FromDeviceRpcResponseActorMsg(
                proto.getRequestId(),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                fromDeviceRpcResponse
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.RemoveRpcActorMsgProto toProto(RemoveRpcActorMsg msg) {
        return TransportProtos.RemoveRpcActorMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setRequestIdMSB(msg.getRequestId().getMostSignificantBits())
                .setRequestIdLSB(msg.getRequestId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.RemoveRpcActorMsgProto proto) {
        return new RemoveRpcActorMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
        );
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TransportProtos.DeviceDeleteMsgProto toProto(DeviceDeleteMsg msg) {
        return TransportProtos.DeviceDeleteMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static DeviceDeleteMsg fromProto(TransportProtos.DeviceDeleteMsgProto proto) {
        return new DeviceDeleteMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())));
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public static TransportProtos.ToDeviceActorNotificationMsgProto toProto(ToDeviceActorNotificationMsg msg) {
        if (msg instanceof DeviceEdgeUpdateMsg) {
            DeviceEdgeUpdateMsg updateMsg = (DeviceEdgeUpdateMsg) msg;
            TransportProtos.DeviceEdgeUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceEdgeUpdateMsg(proto).build();
        } else if (msg instanceof DeviceNameOrTypeUpdateMsg) {
            DeviceNameOrTypeUpdateMsg updateMsg = (DeviceNameOrTypeUpdateMsg) msg;
            TransportProtos.DeviceNameOrTypeUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceNameOrTypeMsg(proto).build();
        } else if (msg instanceof DeviceAttributesEventNotificationMsg) {
            DeviceAttributesEventNotificationMsg updateMsg = (DeviceAttributesEventNotificationMsg) msg;
            TransportProtos.DeviceAttributesEventMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceAttributesEventMsg(proto).build();
        } else if (msg instanceof DeviceCredentialsUpdateNotificationMsg) {
            DeviceCredentialsUpdateNotificationMsg updateMsg = (DeviceCredentialsUpdateNotificationMsg) msg;
            TransportProtos.DeviceCredentialsUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceCredentialsUpdateMsg(proto).build();
        } else if (msg instanceof ToDeviceRpcRequestActorMsg) {
            ToDeviceRpcRequestActorMsg updateMsg = (ToDeviceRpcRequestActorMsg) msg;
            TransportProtos.ToDeviceRpcRequestActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setToDeviceRpcRequestMsg(proto).build();
        } else if (msg instanceof FromDeviceRpcResponseActorMsg) {
            FromDeviceRpcResponseActorMsg updateMsg = (FromDeviceRpcResponseActorMsg) msg;
            TransportProtos.FromDeviceRpcResponseActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setFromDeviceRpcResponseMsg(proto).build();
        } else if (msg instanceof RemoveRpcActorMsg) {
            RemoveRpcActorMsg updateMsg = (RemoveRpcActorMsg) msg;
            TransportProtos.RemoveRpcActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setRemoveRpcActorMsg(proto).build();
        } else if (msg instanceof DeviceDeleteMsg) {
            DeviceDeleteMsg updateMsg = (DeviceDeleteMsg) msg;
            TransportProtos.DeviceDeleteMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceDeleteMsg(proto).build();
        }
        return null;
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static ToDeviceActorNotificationMsg fromProto(TransportProtos.ToDeviceActorNotificationMsgProto proto) {
        if (proto.hasDeviceEdgeUpdateMsg()) {
            return fromProto(proto.getDeviceEdgeUpdateMsg());
        } else if (proto.hasDeviceNameOrTypeMsg()) {
            return fromProto(proto.getDeviceNameOrTypeMsg());
        } else if (proto.hasDeviceAttributesEventMsg()) {
            return fromProto(proto.getDeviceAttributesEventMsg());
        } else if (proto.hasDeviceCredentialsUpdateMsg()) {
            return fromProto(proto.getDeviceCredentialsUpdateMsg());
        } else if (proto.hasToDeviceRpcRequestMsg()) {
            return fromProto(proto.getToDeviceRpcRequestMsg());
        } else if (proto.hasFromDeviceRpcResponseMsg()) {
            return fromProto(proto.getFromDeviceRpcResponseMsg());
        } else if (proto.hasRemoveRpcActorMsg()) {
            return fromProto(proto.getRemoveRpcActorMsg());
        } else if (proto.hasDeviceDeleteMsg()) {
            return fromProto(proto.getDeviceDeleteMsg());
        }
        return null;
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `deletedKeysList`：键。
     * 返回：匹配的数据集合。
     */
    private static Set<AttributeKey> getAttributeKeySetFromProto(List<TransportProtos.AttributeKey> deletedKeysList) {
        if (deletedKeysList.isEmpty()) {
            return null;
        }
        return deletedKeysList.stream()
                .map(attributeKey -> new AttributeKey(attributeKey.getScope().name(), attributeKey.getAttributeKey()))
                .collect(Collectors.toSet());
    }

    /**
     * 功能：获取Protobuf。
     * 参数：
     * - `valuesList`：值。
     * 返回：匹配的数据集合。
     */
    private static List<AttributeKvEntry> getAttributesKvEntryFromProto(List<TransportProtos.AttributeValueProto> valuesList) {
        if (valuesList.isEmpty()) {
            return null;
        }
        List<AttributeKvEntry> result = new ArrayList<>();
        for (TransportProtos.AttributeValueProto kvEntry : valuesList) {
            boolean hasValue = kvEntry.getHasV();
            KvEntry entry = null;
            switch (kvEntry.getType()) {
                case BOOLEAN_V:
                    entry = new BooleanDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getBoolV() : null);
                    break;
                case LONG_V:
                    entry = new LongDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getLongV() : null);
                    break;
                case DOUBLE_V:
                    entry = new DoubleDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getDoubleV() : null);
                    break;
                case STRING_V:
                    entry = new StringDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getStringV() : null);
                    break;
                case JSON_V:
                    entry = new JsonDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getJsonV() : null);
                    break;
            }
            result.add(new BaseAttributeKvEntry(kvEntry.getLastUpdateTs(), entry));
        }
        return result;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ProtoUtils` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
