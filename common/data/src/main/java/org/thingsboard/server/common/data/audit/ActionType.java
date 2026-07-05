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
package org.thingsboard.server.common.data.audit;

import lombok.Getter;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`ActionType` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public enum ActionType {

    ADDED(false, TbMsgType.ENTITY_CREATED), // log entity
    DELETED(false, TbMsgType.ENTITY_DELETED), // log string id
    UPDATED(false, TbMsgType.ENTITY_UPDATED), // log entity
    ATTRIBUTES_UPDATED(false, TbMsgType.ATTRIBUTES_UPDATED), // log attributes/values
    ATTRIBUTES_DELETED(false, TbMsgType.ATTRIBUTES_DELETED), // log attributes
    TIMESERIES_UPDATED(false, TbMsgType.TIMESERIES_UPDATED), // log timeseries update
    TIMESERIES_DELETED(false, TbMsgType.TIMESERIES_DELETED), // log timeseries
    RPC_CALL(false, null), // log method and params
    CREDENTIALS_UPDATED(false, null), // log new credentials
    ASSIGNED_TO_CUSTOMER(false, TbMsgType.ENTITY_ASSIGNED), // log customer name
    UNASSIGNED_FROM_CUSTOMER(false, TbMsgType.ENTITY_UNASSIGNED), // log customer name
    ACTIVATED(false, null), // log string id
    SUSPENDED(false, null), // log string id
    CREDENTIALS_READ(true, null), // log device id
    ATTRIBUTES_READ(true, null), // log attributes
    RELATION_ADD_OR_UPDATE(false, TbMsgType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(false, TbMsgType.RELATION_DELETED),
    RELATIONS_DELETED(false, TbMsgType.RELATIONS_DELETED),
    ALARM_ACK(false, TbMsgType.ALARM_ACK),
    ALARM_CLEAR(false, TbMsgType.ALARM_CLEAR),
    ALARM_DELETE(false, TbMsgType.ALARM_DELETE),
    ALARM_ASSIGNED(false, TbMsgType.ALARM_ASSIGNED),
    ALARM_UNASSIGNED(false, TbMsgType.ALARM_UNASSIGNED),
    LOGIN(false, null),
    LOGOUT(false, null),
    LOCKOUT(false, null),
    ASSIGNED_FROM_TENANT(false, TbMsgType.ENTITY_ASSIGNED_FROM_TENANT),
    ASSIGNED_TO_TENANT(false, TbMsgType.ENTITY_ASSIGNED_TO_TENANT),
    PROVISION_SUCCESS(false, TbMsgType.PROVISION_SUCCESS),
    PROVISION_FAILURE(false, TbMsgType.PROVISION_FAILURE),
    ASSIGNED_TO_EDGE(false, TbMsgType.ENTITY_ASSIGNED_TO_EDGE), // log edge name
    UNASSIGNED_FROM_EDGE(false, TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE),
    ADDED_COMMENT(false, TbMsgType.COMMENT_CREATED),
    UPDATED_COMMENT(false, TbMsgType.COMMENT_UPDATED),
    DELETED_COMMENT(false, null),
    SMS_SENT(false, null);

    /**
     * 是否为`read`。
     */
    @Getter
    private final boolean isRead;

    /**
     * 规则引擎，用于区分不同处理分支。
     */
    private final TbMsgType ruleEngineMsgType;

    ActionType(boolean isRead, TbMsgType ruleEngineMsgType) {
        this.isRead = isRead;
        this.ruleEngineMsgType = ruleEngineMsgType;
    }

    /**
     * 功能：获取规则引擎。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public Optional<TbMsgType> getRuleEngineMsgType() {
        return Optional.ofNullable(ruleEngineMsgType);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ActionType` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
