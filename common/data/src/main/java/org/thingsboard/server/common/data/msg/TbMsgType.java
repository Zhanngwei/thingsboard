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
package org.thingsboard.server.common.data.msg;

import lombok.Getter;
import org.thingsboard.server.common.data.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`TbMsgType` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public enum TbMsgType {

    POST_ATTRIBUTES_REQUEST("Post attributes"),
    POST_TELEMETRY_REQUEST("Post telemetry"),
    TO_SERVER_RPC_REQUEST("RPC Request from Device"),
    ACTIVITY_EVENT("Activity Event"),
    INACTIVITY_EVENT("Inactivity Event"),
    CONNECT_EVENT("Connect Event"),
    DISCONNECT_EVENT("Disconnect Event"),
    ENTITY_CREATED("Entity Created"),
    ENTITY_UPDATED("Entity Updated"),
    ENTITY_DELETED("Entity Deleted"),
    ENTITY_ASSIGNED("Entity Assigned"),
    ENTITY_UNASSIGNED("Entity Unassigned"),
    ATTRIBUTES_UPDATED("Attributes Updated"),
    ATTRIBUTES_DELETED("Attributes Deleted"),
    ALARM,
    ALARM_ACK("Alarm Acknowledged"),
    ALARM_CLEAR("Alarm Cleared"),
    ALARM_DELETE,
    ALARM_ASSIGNED("Alarm Assigned"),
    ALARM_UNASSIGNED("Alarm Unassigned"),
    COMMENT_CREATED("Comment Created"),
    COMMENT_UPDATED("Comment Updated"),
    RPC_CALL_FROM_SERVER_TO_DEVICE("RPC Request to Device"),
    ENTITY_ASSIGNED_FROM_TENANT("Entity Assigned From Tenant"),
    ENTITY_ASSIGNED_TO_TENANT("Entity Assigned To Tenant"),
    ENTITY_ASSIGNED_TO_EDGE,
    ENTITY_UNASSIGNED_FROM_EDGE,
    TIMESERIES_UPDATED("Timeseries Updated"),
    TIMESERIES_DELETED("Timeseries Deleted"),
    RPC_QUEUED("RPC Queued"),
    RPC_SENT("RPC Sent"),
    RPC_DELIVERED("RPC Delivered"),
    RPC_SUCCESSFUL("RPC Successful"),
    RPC_TIMEOUT("RPC Timeout"),
    RPC_EXPIRED("RPC Expired"),
    RPC_FAILED("RPC Failed"),
    RPC_DELETED("RPC Deleted"),
    RELATION_ADD_OR_UPDATE("Relation Added or Updated"),
    RELATION_DELETED("Relation Deleted"),
    RELATIONS_DELETED("All Relations Deleted"),
    PROVISION_SUCCESS,
    PROVISION_FAILURE,
    SEND_EMAIL,

    // tellSelfOnly types
    GENERATOR_NODE_SELF_MSG(null, true),
    DEVICE_PROFILE_PERIODIC_SELF_MSG(null, true),
    DEVICE_PROFILE_UPDATE_SELF_MSG(null, true),
    DEVICE_UPDATE_SELF_MSG(null, true),
    DEDUPLICATION_TIMEOUT_SELF_MSG(null, true),
    DELAY_TIMEOUT_SELF_MSG(null, true),
    MSG_COUNT_SELF_MSG(null, true),

    // Custom or N/A type:
    NA;

    public static final List<String> NODE_CONNECTIONS = EnumSet.allOf(TbMsgType.class).stream()
            .filter(tbMsgType -> !tbMsgType.isTellSelfOnly())
            .map(TbMsgType::getRuleNodeConnection)
            .filter(connection -> !TbNodeConnectionType.OTHER.equals(connection))
            .collect(Collectors.toUnmodifiableList());

    /**
     * 规则节点对象，用于描述当前业务场景。
     */
    @Getter
    private final String ruleNodeConnection;

    /**
     * 是否满足`tellSelfOnly`条件。
     */
    @Getter
    private final boolean tellSelfOnly;

    TbMsgType(String ruleNodeConnection, boolean tellSelfOnly) {
        this.ruleNodeConnection = StringUtils.isNotEmpty(ruleNodeConnection) ? ruleNodeConnection : TbNodeConnectionType.OTHER;
        this.tellSelfOnly = tellSelfOnly;
    }

    TbMsgType(String ruleNodeConnection) {
        this(ruleNodeConnection, false);
    }

    TbMsgType() {
        this(null, false);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgType` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
