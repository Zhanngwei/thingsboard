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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. 类目的：`AuditLog` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
@Data
public class AuditLog extends BaseData<AuditLogId> {

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
     * 实体ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 5, value = "JSON object with Entity id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId entityId;
    /**
     * 实体，用于标识或展示当前对象。
     */
    @NoXss
    @ApiModelProperty(position = 6, value = "Name of the logged entity", example = "Thermometer", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String entityName;
    /**
     * 用户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 7, value = "JSON object with User id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private UserId userId;
    /**
     * 用户，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 8, value = "Unique user name(email) of the user that performed some action on logged entity", example = "tenant@thingsboard.org", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String userName;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 9, value = "String represented Action type", example = "ADDED", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private ActionType actionType;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @ApiModelProperty(position = 10, value = "JsonNode represented action data", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private JsonNode actionData;
    /**
     * 状态，表示当前对象所处状态。
     */
    @ApiModelProperty(position = 11, value = "String represented Action status", example = "SUCCESS", allowableValues = "SUCCESS,FAILURE", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private ActionStatus actionStatus;
    /**
     * 失败信息，表示当前对象的对应属性。
     */
    @ApiModelProperty(position = 12, value = "Failure action details info. An empty string in case of action status type 'SUCCESS', otherwise includes stack trace of the caused exception.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String actionFailureDetails;

    /**
     * 功能：创建 `AuditLog` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AuditLog() {
        super();
    }

    /**
     * 功能：创建 `AuditLog` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public AuditLog(AuditLogId id) {
        super(id);
    }

    /**
     * 功能：创建 `AuditLog` 实例，并初始化必要字段。
     * 参数：
     * - `auditLog`：`auditLog` 参数。
     * 返回：新创建的对象实例。
     */
    public AuditLog(AuditLog auditLog) {
        super(auditLog);
        this.tenantId = auditLog.getTenantId();
        this.customerId = auditLog.getCustomerId();
        this.entityId = auditLog.getEntityId();
        this.entityName = auditLog.getEntityName();
        this.userId = auditLog.getUserId();
        this.userName = auditLog.getUserName();
        this.actionType = auditLog.getActionType();
        this.actionData = auditLog.getActionData();
        this.actionStatus = auditLog.getActionStatus();
        this.actionFailureDetails = auditLog.getActionFailureDetails();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the auditLog creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the auditLog Id")
    @Override
    public AuditLogId getId() {
        return super.getId();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AuditLog` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
