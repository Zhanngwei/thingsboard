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
package org.thingsboard.server.service.edge.rpc.constructor.telemetry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.gen.edge.v1.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`EntityDataMsgConstructor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
@Component
@TbCoreComponent
public class EntityDataMsgConstructor {

    /**
     * 功能：执行 `constructEntityDataMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `actionType`：类型。
     * - `entityData`：待处理数据。
     * 返回：处理结果。
     */
    public EntityDataProto constructEntityDataMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType actionType, JsonElement entityData) {
        EntityDataProto.Builder builder = EntityDataProto.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name());
        switch (actionType) {
            case TIMESERIES_UPDATED:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    long ts;
                    if (data.get("ts") != null && !data.get("ts").isJsonNull()) {
                        ts = data.getAsJsonPrimitive("ts").getAsLong();
                    } else {
                        ts = System.currentTimeMillis();
                    }
                    builder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(data.getAsJsonObject("data"), ts));
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to telemetry proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case ATTRIBUTES_UPDATED:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    TransportProtos.PostAttributeMsg attributesUpdatedMsg = JsonConverter.convertToAttributesProto(data.getAsJsonObject("kv"));
                    builder.setAttributesUpdatedMsg(attributesUpdatedMsg);
                    builder.setPostAttributeScope(getScopeOfDefault(data));
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to AttributesUpdatedMsg proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case POST_ATTRIBUTES:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    TransportProtos.PostAttributeMsg postAttributesMsg = JsonConverter.convertToAttributesProto(data.getAsJsonObject("kv"));
                    builder.setPostAttributesMsg(postAttributesMsg);
                    builder.setPostAttributeScope(getScopeOfDefault(data));
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to PostAttributesMsg, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case ATTRIBUTES_DELETED:
                try {
                    AttributeDeleteMsg.Builder attributeDeleteMsg = AttributeDeleteMsg.newBuilder();
                    attributeDeleteMsg.setScope(entityData.getAsJsonObject().getAsJsonPrimitive("scope").getAsString());
                    JsonArray jsonArray = entityData.getAsJsonObject().getAsJsonArray("keys");
                    List<String> keys = new Gson().fromJson(jsonArray.toString(), new TypeToken<>(){}.getType());
                    attributeDeleteMsg.addAllAttributeNames(keys);
                    attributeDeleteMsg.build();
                    builder.setAttributeDeleteMsg(attributeDeleteMsg);
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to AttributeDeleteMsg proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
        }
        return builder.build();
    }

    /**
     * 功能：获取`Scope Of Default`。
     * 参数：
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    private String getScopeOfDefault(JsonObject data) {
        JsonPrimitive scope = data.getAsJsonPrimitive("scope");
        String result = DataConstants.SERVER_SCOPE;
        if (scope != null && StringUtils.isNotBlank(scope.getAsString())) {
            result = scope.getAsString();
        }
        return result;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityDataMsgConstructor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
