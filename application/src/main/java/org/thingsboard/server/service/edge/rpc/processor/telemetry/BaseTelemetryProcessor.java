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
package org.thingsboard.server.service.edge.rpc.processor.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.gen.edge.v1.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`BaseTelemetryProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class BaseTelemetryProcessor extends BaseEdgeProcessor {

    private final Gson gson = new Gson();

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducer;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    abstract protected String getMsgSourceKey();

    /**
     * 功能：处理遥测。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityData`：待处理数据。
     * 返回：匹配的数据集合。
     */
    public List<ListenableFuture<Void>> processTelemetryMsg(TenantId tenantId, EntityDataProto entityData) {
        log.trace("[{}] processTelemetryMsg [{}]", tenantId, entityData);
        List<ListenableFuture<Void>> result = new ArrayList<>();
        EntityId entityId = constructEntityId(entityData.getEntityType(), entityData.getEntityIdMSB(), entityData.getEntityIdLSB());
        if (entityId != null && isEntityExists(tenantId, entityId)) {
            if ((entityData.hasPostAttributesMsg() || entityData.hasPostTelemetryMsg() || entityData.hasAttributesUpdatedMsg())) {
                Pair<TbMsgMetaData, CustomerId> pair = getBaseMsgMetadataAndCustomerId(tenantId, entityId);
                TbMsgMetaData metaData = pair.getKey();
                CustomerId customerId = pair.getValue();
                metaData.putValue(DataConstants.MSG_SOURCE_KEY, getMsgSourceKey());
                if (entityData.hasPostAttributesMsg()) {
                    result.add(processPostAttributes(tenantId, customerId, entityId, entityData.getPostAttributesMsg(), metaData));
                }
                if (entityData.hasAttributesUpdatedMsg()) {
                    metaData.putValue("scope", entityData.getPostAttributeScope());
                    result.add(processAttributesUpdate(tenantId, customerId, entityId, entityData.getAttributesUpdatedMsg(), metaData));
                }
                if (entityData.hasPostTelemetryMsg()) {
                    result.add(processPostTelemetry(tenantId, customerId, entityId, entityData.getPostTelemetryMsg(), metaData));
                }
                if (EntityType.DEVICE.equals(entityId.getEntityType())) {
                    DeviceId deviceId = new DeviceId(entityId.getId());

                    long currentTs = System.currentTimeMillis();

                    TransportProtos.DeviceActivityProto deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastActivityTime(currentTs).build();

                    log.trace("[{}][{}] device activity time is going to be updated, ts {}", tenantId, deviceId, currentTs);

                    TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
                    tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(deviceId.getId(),
                            TransportProtos.ToCoreMsg.newBuilder().setDeviceActivityMsg(deviceActivityMsg).build()), null);
                }
            }
            if (entityData.hasAttributeDeleteMsg()) {
                result.add(processAttributeDeleteMsg(tenantId, entityId, entityData.getAttributeDeleteMsg(), entityData.getEntityType()));
            }
        } else {
            log.warn("[{}] Skipping telemetry update msg because entity doesn't exists on edge, {}", tenantId, entityData);
        }
        return result;
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private Pair<TbMsgMetaData, CustomerId> getBaseMsgMetadataAndCustomerId(TenantId tenantId, EntityId entityId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        CustomerId customerId = null;
        switch (entityId.getEntityType()) {
            case DEVICE:
                Device device = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                if (device != null) {
                    customerId = device.getCustomerId();
                    metaData.putValue("deviceName", device.getName());
                    metaData.putValue("deviceType", device.getType());
                }
                break;
            case ASSET:
                Asset asset = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
                if (asset != null) {
                    customerId = asset.getCustomerId();
                    metaData.putValue("assetName", asset.getName());
                    metaData.putValue("assetType", asset.getType());
                }
                break;
            case ENTITY_VIEW:
                EntityView entityView = entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
                if (entityView != null) {
                    customerId = entityView.getCustomerId();
                    metaData.putValue("entityViewName", entityView.getName());
                    metaData.putValue("entityViewType", entityView.getType());
                }
                break;
            case EDGE:
                Edge edge = edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId()));
                if (edge != null) {
                    customerId = edge.getCustomerId();
                    metaData.putValue("edgeName", edge.getName());
                    metaData.putValue("edgeType", edge.getType());
                }
                break;
            default:
                log.debug("[{}] Using empty metadata for entityId [{}]", tenantId, entityId);
                break;
        }
        return new ImmutablePair<>(metaData, customerId != null ? customerId : new CustomerId(ModelConstants.NULL_UUID));
    }

    /**
     * 功能：处理遥测。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityId`：实体IDID。
     * - `msg`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> processPostTelemetry(TenantId tenantId, CustomerId customerId, EntityId entityId, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            metaData.putValue("ts", tsKv.getTs() + "");
            var defaultQueueAndRuleChain = getDefaultQueueNameAndRuleChainId(tenantId, entityId);
            TbMsg tbMsg = TbMsg.newMsg(defaultQueueAndRuleChain.getKey(), TbMsgType.POST_TELEMETRY_REQUEST, entityId, customerId, metaData, gson.toJson(json), defaultQueueAndRuleChain.getValue(), null);
            tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}] Can't process post telemetry [{}]", tenantId, msg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private Pair<String, RuleChainId> getDefaultQueueNameAndRuleChainId(TenantId tenantId, EntityId entityId) {
        RuleChainId ruleChainId = null;
        String queueName = null;
        if (EntityType.DEVICE.equals(entityId.getEntityType())) {
            DeviceProfile deviceProfile = deviceProfileCache.get(tenantId, new DeviceId(entityId.getId()));
            if (deviceProfile == null) {
                log.warn("[{}][{}] Device profile is null!", tenantId, entityId);
            } else {
                ruleChainId = deviceProfile.getDefaultRuleChainId();
                queueName = deviceProfile.getDefaultQueueName();
            }
        } else if (EntityType.ASSET.equals(entityId.getEntityType())) {
            AssetProfile assetProfile = assetProfileCache.get(tenantId, new AssetId(entityId.getId()));
            if (assetProfile == null) {
                log.warn("[{}][{}] Asset profile is null!", tenantId, entityId);
            } else {
                ruleChainId = assetProfile.getDefaultRuleChainId();
                queueName = assetProfile.getDefaultQueueName();
            }
        }
        return new ImmutablePair<>(queueName, ruleChainId);
    }

    /**
     * 功能：处理`Post Attributes`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityId`：实体IDID。
     * - `msg`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> processPostAttributes(TenantId tenantId, CustomerId customerId, EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        var defaultQueueAndRuleChain = getDefaultQueueNameAndRuleChainId(tenantId, entityId);
        TbMsg tbMsg = TbMsg.newMsg(defaultQueueAndRuleChain.getKey(), TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, customerId, metaData, gson.toJson(json), defaultQueueAndRuleChain.getValue(), null);
        tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't process post attributes [{}]", tenantId, msg, t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    /**
     * 功能：处理`Attributes Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityId`：实体IDID。
     * - `msg`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> processAttributesUpdate(TenantId tenantId,
                                                           CustomerId customerId,
                                                           EntityId entityId,
                                                           TransportProtos.PostAttributeMsg msg,
                                                           TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(json));
        String scope = metaData.getValue("scope");
        tsSubService.saveAndNotify(tenantId, entityId, scope, attributes, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                var defaultQueueAndRuleChain = getDefaultQueueNameAndRuleChainId(tenantId, entityId);
                TbMsg tbMsg = TbMsg.newMsg(defaultQueueAndRuleChain.getKey(), TbMsgType.ATTRIBUTES_UPDATED, entityId,
                        customerId, metaData, gson.toJson(json), defaultQueueAndRuleChain.getValue(), null);
                tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        futureToSet.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Can't process attributes update [{}]", tenantId, msg, t);
                        futureToSet.setException(t);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't process attributes update [{}]", tenantId, msg, t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeDeleteMsg`：待处理消息。
     * - `entityType`：实体对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> processAttributeDeleteMsg(TenantId tenantId, EntityId entityId, AttributeDeleteMsg attributeDeleteMsg,
                                                             String entityType) {

        String scope = attributeDeleteMsg.getScope();
        List<String> attributeKeys = attributeDeleteMsg.getAttributeNamesList();
        ListenableFuture<List<String>> removeAllFuture = attributesService.removeAll(tenantId, entityId, scope, attributeKeys);
        return Futures.transformAsync(removeAllFuture, removeAttributes -> {
            if (EntityType.DEVICE.name().equals(entityType)) {
                SettableFuture<Void> futureToSet = SettableFuture.create();
                tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(
                        tenantId, (DeviceId) entityId, scope, attributeKeys), new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        futureToSet.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Can't process attribute delete msg [{}]", tenantId, attributeDeleteMsg, t);
                        futureToSet.setException(t);
                    }
                });
                return futureToSet;
            } else {
                return Futures.immediateFuture(null);
            }
        }, dbCallbackExecutorService);
    }

    /**
     * 功能：转换实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityUUID`：实体ID。
     * - `actionType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public EntityDataProto convertTelemetryEventToEntityDataProto(TenantId tenantId,
                                                                  EntityType entityType,
                                                                  UUID entityUUID,
                                                                  EdgeEventActionType actionType,
                                                                  JsonNode body) {
        EntityId entityId;
        switch (entityType) {
            case DEVICE:
                entityId = new DeviceId(entityUUID);
                break;
            case ASSET:
                entityId = new AssetId(entityUUID);
                break;
            case ENTITY_VIEW:
                entityId = new EntityViewId(entityUUID);
                break;
            case DASHBOARD:
                entityId = new DashboardId(entityUUID);
                break;
            case TENANT:
                entityId = TenantId.fromUUID(entityUUID);
                break;
            case CUSTOMER:
                entityId = new CustomerId(entityUUID);
                break;
            case USER:
                entityId = new UserId(entityUUID);
                break;
            case EDGE:
                entityId = new EdgeId(entityUUID);
                break;
            default:
                log.warn("[{}] Unsupported edge event type [{}]", tenantId, entityType);
                return null;
        }
        String bodyJackson = JacksonUtil.toString(body);
        return bodyJackson == null ? null :
                entityDataMsgConstructor.constructEntityDataMsg(tenantId, entityId, actionType, JsonParser.parseString(bodyJackson));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseTelemetryProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
