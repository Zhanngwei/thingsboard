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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by ashvayka on 11.05.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`Alarm` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm extends BaseData<AlarmId> implements HasName, HasTenantId, HasCustomerId {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Customer Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private CustomerId customerId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @NoXss
    @ApiModelProperty(position = 6, required = true, value = "representing type of the Alarm", example = "High Temperature Alarm")
    @Length(fieldName = "type")
    private String type;
    /**
     * `originator` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 7, required = true, value = "JSON object with alarm originator id")
    private EntityId originator;
    /**
     * `severity` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 8, required = true, value = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    /**
     * 当前告警是否已经确认。
     */
    @ApiModelProperty(position = 9, required = true, value = "Acknowledged", example = "true")
    private boolean acknowledged;
    /**
     * 当前告警是否已经清除。
     */
    @ApiModelProperty(position = 10, required = true, value = "Cleared", example = "false")
    private boolean cleared;
    /**
     * `assigneeId`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 11, value = "Alarm assignee user id")
    private UserId assigneeId;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @ApiModelProperty(position = 12, value = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @ApiModelProperty(position = 13, value = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @ApiModelProperty(position = 14, value = "Timestamp of the alarm acknowledgement, in milliseconds", example = "1634115221948")
    private long ackTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @ApiModelProperty(position = 15, value = "Timestamp of the alarm clearing, in milliseconds", example = "1634114528465")
    private long clearTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @ApiModelProperty(position = 16, value = "Timestamp of the alarm assignment, in milliseconds", example = "1634115928465")
    private long assignTs;
    /**
     * `details` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 17, value = "JSON object with alarm details")
    private transient JsonNode details;
    /**
     * 是否向关联对象传播当前状态。
     */
    @ApiModelProperty(position = 18, value = "Propagation flag to specify if alarm should be propagated to parent entities of alarm originator", example = "true")
    private boolean propagate;
    /**
     * 是否向关联对象传播当前状态。
     */
    @ApiModelProperty(position = 19, value = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) of alarm originator", example = "true")
    private boolean propagateToOwner;
    /**
     * 是否向关联对象传播当前状态。
     */
    @ApiModelProperty(position = 20, value = "Propagation flag to specify if alarm should be propagated to the tenant entity", example = "true")
    private boolean propagateToTenant;
    /**
     * 关系列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(position = 21, value = "JSON array of relation types that should be used for propagation. " +
            "By default, 'propagateRelationTypes' array is empty which means that the alarm will be propagated based on any relation type to parent entities. " +
            "This parameter should be used only in case when 'propagate' parameter is set to true, otherwise, 'propagateRelationTypes' array will be ignored.")
    private List<String> propagateRelationTypes;

    /**
     * 功能：创建 `Alarm` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Alarm() {
        super();
    }

    /**
     * 功能：创建 `Alarm` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Alarm(AlarmId id) {
        super(id);
    }

    /**
     * 功能：创建 `Alarm` 实例，并初始化必要字段。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：新创建的对象实例。
     */
    public Alarm(Alarm alarm) {
        super(alarm.getId());
        this.createdTime = alarm.getCreatedTime();
        this.tenantId = alarm.getTenantId();
        this.customerId = alarm.getCustomerId();
        this.type = alarm.getType();
        this.originator = alarm.getOriginator();
        this.severity = alarm.getSeverity();
        this.assigneeId = alarm.getAssigneeId();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.acknowledged = alarm.isAcknowledged();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.cleared = alarm.isCleared();
        this.assignTs = alarm.getAssignTs();
        this.details = alarm.getDetails();
        this.propagate = alarm.isPropagate();
        this.propagateToOwner = alarm.isPropagateToOwner();
        this.propagateToTenant = alarm.isPropagateToTenant();
        this.propagateRelationTypes = alarm.getPropagateRelationTypes();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ApiModelProperty(position = 5, required = true, value = "representing type of the Alarm", example = "High Temperature Alarm")
    public String getName() {
        return type;
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the alarm Id. " +
            "Specify this field to update the alarm. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm.")
    @Override
    public AlarmId getId() {
        return super.getId();
    }


    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the alarm creation, in milliseconds", example = "1634058704567", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ApiModelProperty(position = 22, required = true, value = "status of the Alarm", example = "ACTIVE_UNACK", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public AlarmStatus getStatus() {
        return toStatus(cleared, acknowledged);
    }

    /**
     * 功能：执行 `toStatus` 对应的处理。
     * 参数：
     * - `cleared`：`cleared` 参数。
     * - `acknowledged`：`acknowledged` 参数。
     * 返回：处理结果。
     */
    public static AlarmStatus toStatus(boolean cleared, boolean acknowledged) {
        if (cleared) {
            return acknowledged ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
        } else {
            return acknowledged ? AlarmStatus.ACTIVE_ACK : AlarmStatus.ACTIVE_UNACK;
        }
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public DashboardId getDashboardId() {
        return Optional.ofNullable(getDetails()).map(details -> details.get("dashboardId"))
                .filter(JsonNode::isTextual).map(id -> new DashboardId(UUID.fromString(id.asText()))).orElse(null);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`Alarm` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
