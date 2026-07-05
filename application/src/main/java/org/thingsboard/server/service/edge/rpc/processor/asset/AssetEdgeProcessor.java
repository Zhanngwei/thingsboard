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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.BaseAssetService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructor;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`AssetEdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class AssetEdgeProcessor extends BaseAssetProcessor implements AssetProcessor {

    /**
     * 功能：处理资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `assetUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> processAssetMsgFromEdge(TenantId tenantId, Edge edge, AssetUpdateMsg assetUpdateMsg) {
        log.trace("[{}] executing processAssetMsgFromEdge [{}] from edge [{}]", tenantId, assetUpdateMsg, edge.getId());
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (assetUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Asset assetToDelete = assetService.findAssetById(tenantId, assetId);
                    if (assetToDelete != null) {
                        assetService.unassignAssetFromEdge(tenantId, assetId, edge.getId());
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed asset violated {}", tenantId, assetUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `assetUpdateMsg`：待处理消息。
     * - `edge`：`edge` 参数。
     * 返回：无。
     */
    private void saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), assetId);
            pushAssetCreatedEventToRuleEngine(tenantId, edge, assetId);
            assetService.assignAssetToEdge(tenantId, assetId, edge.getId());
        }
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET, EdgeEventActionType.UPDATED, assetId, null);
        }
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    private void pushAssetCreatedEventToRuleEngine(TenantId tenantId, Edge edge, AssetId assetId) {
        try {
            Asset asset = assetService.findAssetById(tenantId, assetId);
            String assetAsString = JacksonUtil.toString(asset);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, asset.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, assetId, asset.getCustomerId(), TbMsgType.ENTITY_CREATED, assetAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push asset action to rule engine: {}", tenantId, assetId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    /**
     * 功能：转换资产。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * - `edgeId`：边缘节点ID。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public DownlinkMsg convertAssetEventToDownlink(EdgeEvent edgeEvent, EdgeId edgeId, EdgeVersion edgeVersion) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Asset asset = assetService.findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null && !BaseAssetService.TB_SERVICE_QUEUE.equals(asset.getType())) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AssetUpdateMsg assetUpdateMsg = ((AssetMsgConstructor)
                            assetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructAssetUpdatedMsg(msgType, asset);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetUpdateMsg(assetUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        AssetProfile assetProfile = assetProfileService.findAssetProfileById(edgeEvent.getTenantId(), asset.getAssetProfileId());
                        assetProfile = checkIfAssetProfileDefaultFieldsAssignedToEdge(edgeEvent.getTenantId(), edgeId, assetProfile, edgeVersion);
                        builder.addAssetProfileUpdateMsg(((AssetMsgConstructor) assetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                                .constructAssetProfileUpdatedMsg(msgType, assetProfile));
                    }
                    downlinkMsg = builder.build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                AssetUpdateMsg assetUpdateMsg = ((AssetMsgConstructor)
                        assetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructAssetDeleteMsg(assetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(assetUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AssetEdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
