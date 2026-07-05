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

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. 类目的：`AlarmEdgeProcessorV1` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Component
@TbCoreComponent
public class AlarmEdgeProcessorV1 extends AlarmEdgeProcessor {

    /**
     * 功能：执行 `constructAlarmFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `originatorId`：`originatorId`ID。
     * - `alarmUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected Alarm constructAlarmFromUpdateMsg(TenantId tenantId, AlarmId alarmId, EntityId originatorId, AlarmUpdateMsg alarmUpdateMsg) {
        Alarm alarm = new Alarm();
        alarm.setId(alarmId);
        alarm.setTenantId(tenantId);
        alarm.setOriginator(originatorId);
        alarm.setType(alarmUpdateMsg.getName());
        alarm.setSeverity(AlarmSeverity.valueOf(alarmUpdateMsg.getSeverity()));
        alarm.setStartTs(alarmUpdateMsg.getStartTs());
        AlarmStatus alarmStatus = AlarmStatus.valueOf(alarmUpdateMsg.getStatus());
        alarm.setClearTs(alarmUpdateMsg.getClearTs());
        alarm.setPropagate(alarmUpdateMsg.getPropagate());
        alarm.setCleared(alarmStatus.isCleared());
        alarm.setAcknowledged(alarmStatus.isAck());
        alarm.setAckTs(alarmUpdateMsg.getAckTs());
        alarm.setEndTs(alarmUpdateMsg.getEndTs());
        alarm.setDetails(JacksonUtil.toJsonNode(alarmUpdateMsg.getDetails()));
        return alarm;
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected EntityId getAlarmOriginatorFromMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        return getAlarmOriginator(tenantId, alarmUpdateMsg.getOriginatorName(),
                EntityType.valueOf(alarmUpdateMsg.getOriginatorType()));
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityName`：实体对象。
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    private EntityId getAlarmOriginator(TenantId tenantId, String entityName, EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return deviceService.findDeviceByTenantIdAndName(tenantId, entityName).getId();
            case ASSET:
                return assetService.findAssetByTenantIdAndName(tenantId, entityName).getId();
            case ENTITY_VIEW:
                return entityViewService.findEntityViewByTenantIdAndName(tenantId, entityName).getId();
            default:
                return null;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmEdgeProcessorV1` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
