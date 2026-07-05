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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

/**
 * 中文说明：
 * 1. 类目的：`AlarmAssignmentNotificationInfo` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmAssignmentNotificationInfo implements RuleOriginatedNotificationInfo {

    /**
     * `action` 字段，保存当前对象的对应属性。
     */
    private String action;

    /**
     * 邮箱，用于展示或标识当前对象。
     */
    private String assigneeEmail;
    private String assigneeFirstName;
    /**
     * 名称，用于标识或展示当前对象。
     */
    private String assigneeLastName;
    private UserId assigneeId;

    /**
     * 用户对象，用于描述当前业务场景。
     */
    private String userEmail;
    private String userFirstName;
    /**
     * 用户，用于标识或展示当前对象。
     */
    private String userLastName;

    /**
     * 告警，用于区分不同处理分支。
     */
    private String alarmType;
    private UUID alarmId;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private EntityId alarmOriginator;
    private String alarmOriginatorName;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private AlarmSeverity alarmSeverity;
    private AlarmStatus alarmStatus;
    /**
     * 客户ID，用于定位对应业务对象。
     */
    private CustomerId alarmCustomerId;
    private DashboardId dashboardId;

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "action", action,
                "assigneeTitle", User.getTitle(assigneeEmail, assigneeFirstName, assigneeLastName),
                "assigneeFirstName", assigneeFirstName,
                "assigneeLastName", assigneeLastName,
                "assigneeEmail", assigneeEmail,
                "assigneeId", assigneeId != null ? assigneeId.toString() : null,
                "userTitle", User.getTitle(userEmail, userFirstName, userLastName),
                "userEmail", userEmail,
                "userFirstName", userFirstName,
                "userLastName", userLastName,
                "alarmType", alarmType,
                "alarmId", alarmId.toString(),
                "alarmSeverity", alarmSeverity.name().toLowerCase(),
                "alarmStatus", alarmStatus.toString(),
                "alarmOriginatorEntityType", alarmOriginator.getEntityType().getNormalName(),
                "alarmOriginatorId", alarmOriginator.getId().toString(),
                "alarmOriginatorName", alarmOriginatorName
        );
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CustomerId getAffectedCustomerId() {
        return alarmCustomerId;
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public UserId getAffectedUserId() {
        return assigneeId;
    }

    /**
     * 功能：获取实体ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityId getStateEntityId() {
        return alarmOriginator;
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DashboardId getDashboardId() {
        return dashboardId;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmAssignmentNotificationInfo` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
