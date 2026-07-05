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
package org.thingsboard.server.service.edge.rpc.processor.entityview;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructor;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`EntityViewEdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class EntityViewEdgeProcessor extends BaseEntityViewProcessor implements EntityViewProcessor {

    /**
     * 功能：处理实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `entityViewUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> processEntityViewMsgFromEdge(TenantId tenantId, Edge edge, EntityViewUpdateMsg entityViewUpdateMsg) {
        log.trace("[{}] executing processEntityViewMsgFromEdge [{}] from edge [{}]", tenantId, entityViewUpdateMsg, edge.getId());
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (entityViewUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    EntityView entityViewToDelete = entityViewService.findEntityViewById(tenantId, entityViewId);
                    if (entityViewToDelete != null) {
                        entityViewService.unassignEntityViewFromEdge(tenantId, entityViewId, edge.getId());
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed entity views violated {}", tenantId, entityViewUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    /**
     * 功能：保存或创建实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `entityViewUpdateMsg`：待处理消息。
     * - `edge`：`edge` 参数。
     * 返回：无。
     */
    private void saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), entityViewId);
            pushEntityViewCreatedEventToRuleEngine(tenantId, edge, entityViewId);
            entityViewService.assignEntityViewToEdge(tenantId, entityViewId, edge.getId());
        }
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW, EdgeEventActionType.UPDATED, entityViewId, null);
        }
    }

    /**
     * 功能：发送或提交实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `entityViewId`：实体视图ID。
     * 返回：无。
     */
    private void pushEntityViewCreatedEventToRuleEngine(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        try {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, entityViewId);
            String entityViewAsString = JacksonUtil.toString(entityView);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, entityView.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, entityViewId, entityView.getCustomerId(), TbMsgType.ENTITY_CREATED, entityViewAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push entity view action to rule engine: {}", tenantId, entityViewId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    /**
     * 功能：转换实体视图。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    public DownlinkMsg convertEntityViewEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        EntityViewId entityViewId = new EntityViewId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                EntityView entityView = entityViewService.findEntityViewById(edgeEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    EntityViewUpdateMsg entityViewUpdateMsg = ((EntityViewMsgConstructor)
                            entityViewMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructEntityViewUpdatedMsg(msgType, entityView);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addEntityViewUpdateMsg(entityViewUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                EntityViewUpdateMsg entityViewUpdateMsg = ((EntityViewMsgConstructor)
                        entityViewMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructEntityViewDeleteMsg(entityViewId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addEntityViewUpdateMsg(entityViewUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityViewEdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
