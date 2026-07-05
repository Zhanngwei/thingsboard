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
package org.thingsboard.server.service.edge.rpc.processor.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`AlarmEdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class AlarmEdgeProcessor extends BaseAlarmProcessor implements AlarmProcessor {

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `alarmUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> processAlarmMsgFromEdge(TenantId tenantId, EdgeId edgeId, AlarmUpdateMsg alarmUpdateMsg) {
        log.trace("[{}] processAlarmMsgFromEdge [{}]", tenantId, alarmUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edgeId);
            return processAlarmMsg(tenantId, alarmUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    /**
     * 功能：转换告警。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public DownlinkMsg convertAlarmEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AlarmUpdateMsg alarmUpdateMsg = convertAlarmEventToAlarmMsg(edgeEvent.getTenantId(), edgeEvent.getEntityId(),
                edgeEvent.getAction(), edgeEvent.getBody(), edgeVersion);
        if (alarmUpdateMsg != null) {
            return DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addAlarmUpdateMsg(alarmUpdateMsg)
                    .build();
        }
        return null;
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `alarmCommentUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> processAlarmCommentMsgFromEdge(TenantId tenantId, EdgeId edgeId, AlarmCommentUpdateMsg alarmCommentUpdateMsg) {
        log.trace("[{}] processAlarmCommentMsgFromEdge [{}]", tenantId, alarmCommentUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edgeId);
            return processAlarmCommentMsg(tenantId, alarmCommentUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    /**
     * 功能：转换告警。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public DownlinkMsg convertAlarmCommentEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (edgeEvent.getAction()) {
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
            case DELETED_COMMENT:
                AlarmComment alarmComment = JacksonUtil.convertValue(edgeEvent.getBody(), AlarmComment.class);
                if (alarmComment != null) {
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAlarmCommentUpdateMsg(((AlarmMsgConstructor) alarmMsgConstructorFactory
                                    .getMsgConstructorByEdgeVersion(edgeVersion)).constructAlarmCommentUpdatedMsg(msgType, alarmComment))
                            .build();
                }
            default:
                return null;
        }
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeNotificationMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<Void> processAlarmNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        if (EdgeEventActionType.DELETED.equals(actionType) || EdgeEventActionType.ALARM_DELETE.equals(actionType)) {
            Alarm deletedAlarm = JacksonUtil.fromString(edgeNotificationMsg.getBody(), Alarm.class);
            if (deletedAlarm == null) {
                return Futures.immediateFuture(null);
            }
            List<ListenableFuture<Void>> delFutures = pushEventToAllRelatedEdges(tenantId, deletedAlarm.getOriginator(),
                    alarmId, actionType, JacksonUtil.valueToTree(deletedAlarm), originatorEdgeId, EdgeEventType.ALARM);
            return Futures.transform(Futures.allAsList(delFutures), voids -> null, dbCallbackExecutorService);
        }
        ListenableFuture<Alarm> alarmFuture = alarmService.findAlarmByIdAsync(tenantId, alarmId);
        return Futures.transformAsync(alarmFuture, alarm -> {
            if (alarm == null) {
                return Futures.immediateFuture(null);
            }
            EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(alarm.getOriginator().getEntityType());
            if (type == null) {
                return Futures.immediateFuture(null);
            }
            List<ListenableFuture<Void>> futures = pushEventToAllRelatedEdges(tenantId, alarm.getOriginator(),
                    alarmId, actionType, null, originatorEdgeId, EdgeEventType.ALARM);
            return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
        }, dbCallbackExecutorService);
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeNotificationMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<Void> processAlarmCommentNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        AlarmComment alarmComment = JacksonUtil.fromString(edgeNotificationMsg.getBody(), AlarmComment.class);
        if (alarmComment == null) {
            return Futures.immediateFuture(null);
        }
        Alarm alarmById = alarmService.findAlarmById(tenantId, new AlarmId(alarmComment.getAlarmId().getId()));
        List<ListenableFuture<Void>> delFutures = pushEventToAllRelatedEdges(tenantId, alarmById.getOriginator(),
                alarmId, actionType, JacksonUtil.valueToTree(alarmComment), originatorEdgeId, EdgeEventType.ALARM_COMMENT);
        return Futures.transform(Futures.allAsList(delFutures), voids -> null, dbCallbackExecutorService);
    }

    /**
     * 功能：发送或提交事件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorId`：`originatorId`ID。
     * - `alarmId`：告警IDID。
     * - `actionType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private List<ListenableFuture<Void>> pushEventToAllRelatedEdges(TenantId tenantId, EntityId originatorId, AlarmId alarmId,
                                                                    EdgeEventActionType actionType, JsonNode body, EdgeId sourceEdgeId,
                                                                    EdgeEventType edgeEventType) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterableByTenantIdEntityId<EdgeId> edgeIds =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, originatorId, DEFAULT_PAGE_SIZE);
        for (EdgeId relatedEdgeId : edgeIds) {
            if (!relatedEdgeId.equals(sourceEdgeId)) {
                futures.add(saveEdgeEvent(tenantId, relatedEdgeId, edgeEventType, actionType, alarmId, body));
            }
        }
        return futures;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmEdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
