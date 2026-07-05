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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Created by Victor Basanets on 8/27/2017.
 */

/**
 * 中文说明：
 * 1. 类目的：`EntityView` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EntityView extends BaseDataWithAdditionalInfo<EntityViewId>
        implements HasName, HasTenantId, HasCustomerId, ExportableEntity<EntityViewId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 5582010124562018986L;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 7, value = "JSON object with the referenced Entity Id (Device or Asset).")
    private EntityId entityId;
    private TenantId tenantId;
    /**
     * 客户ID，用于定位对应业务对象。
     */
    private CustomerId customerId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 5, required = true, value = "Entity View name", example = "A4B72CCDFF33")
    private String name;
    /**
     * 类型，用于区分不同处理分支。
     */
    @NoXss
    @Length(fieldName = "type")
    @ApiModelProperty(position = 6, required = true, value = "Device Profile Name", example = "Temperature Sensor")
    private String type;
    /**
     * `keys` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 8, value = "Set of telemetry and attribute keys to expose via Entity View.")
    private TelemetryEntityView keys;
    /**
     * 开始时间戳，用于限定查询或统计的起点。
     */
    @ApiModelProperty(position = 9, value = "Represents the start time of the interval that is used to limit access to target device telemetry. Customer will not be able to see entity telemetry that is outside the specified interval;")
    private long startTimeMs;
    /**
     * 结束时间戳，用于限定查询或统计的终点。
     */
    @ApiModelProperty(position = 10, value = "Represents the end time of the interval that is used to limit access to target device telemetry. Customer will not be able to see entity telemetry that is outside the specified interval;")
    private long endTimeMs;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    private EntityViewId externalId;

    /**
     * 功能：创建 `EntityView` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EntityView() {
        super();
    }

    /**
     * 功能：创建 `EntityView` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public EntityView(EntityViewId id) {
        super(id);
    }

    /**
     * 功能：创建 `EntityView` 实例，并初始化必要字段。
     * 参数：
     * - `entityView`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityView(EntityView entityView) {
        super(entityView);
        this.entityId = entityView.getEntityId();
        this.tenantId = entityView.getTenantId();
        this.customerId = entityView.getCustomerId();
        this.name = entityView.getName();
        this.type = entityView.getType();
        this.keys = entityView.getKeys();
        this.startTimeMs = entityView.getStartTimeMs();
        this.endTimeMs = entityView.getEndTimeMs();
        this.externalId = entityView.getExternalId();
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignEntityViewToCustomer' to change the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public CustomerId getCustomerId() {
        return customerId;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the Entity View Id. " +
            "Specify this field to update the Entity View. " +
            "Referencing non-existing Entity View Id will cause error. " +
            "Omit this field to create new Entity View." )
    @Override
    public EntityViewId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the Entity View creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 11, value = "Additional parameters of the device", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EntityView` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
