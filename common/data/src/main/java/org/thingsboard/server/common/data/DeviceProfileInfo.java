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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`DeviceProfileInfo` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "image")
public class DeviceProfileInfo extends EntityInfo {

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @ApiModelProperty(position = 3, value = "Either URL or Base64 data of the icon. Used in the mobile application to visualize set of device profiles in the grid view. ")
    private final String image;
    /**
     * 仪表盘ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 4, value = "Reference to the dashboard. Used in the mobile application to open the default dashboard when user navigates to device details.")
    private final DashboardId defaultDashboardId;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 5, value = "Type of the profile. Always 'DEFAULT' for now. Reserved for future use.")
    private final DeviceProfileType type;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 6, value = "Type of the transport used to connect the device. Default transport supports HTTP, CoAP and MQTT.")
    private final DeviceTransportType transportType;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 7, value = "Tenant id.")
    private final TenantId tenantId;

    /**
     * 功能：创建 `DeviceProfileInfo` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `image`：`image` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public DeviceProfileInfo(@JsonProperty("id") EntityId id,
                             @JsonProperty("tenantId") TenantId tenantId,
                             @JsonProperty("name") String name,
                             @JsonProperty("image") String image,
                             @JsonProperty("defaultDashboardId") DashboardId defaultDashboardId,
                             @JsonProperty("type") DeviceProfileType type,
                             @JsonProperty("transportType") DeviceTransportType transportType) {
        super(id, name);
        this.tenantId = tenantId;
        this.image = image;
        this.defaultDashboardId = defaultDashboardId;
        this.type = type;
        this.transportType = transportType;
    }

    /**
     * 功能：创建 `DeviceProfileInfo` 实例，并初始化必要字段。
     * 参数：
     * - `uuid`：`uuid`ID。
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `image`：`image` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public DeviceProfileInfo(UUID uuid, UUID tenantId, String name, String image, UUID defaultDashboardId, DeviceProfileType type, DeviceTransportType transportType) {
        super(EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE_PROFILE, uuid), name);
        this.tenantId = new TenantId(tenantId);
        this.image = image;
        this.defaultDashboardId = defaultDashboardId != null ? new DashboardId(defaultDashboardId) : null;
        this.type = type;
        this.transportType = transportType;
    }

    /**
     * 功能：创建 `DeviceProfileInfo` 实例，并初始化必要字段。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：新创建的对象实例。
     */
    public DeviceProfileInfo(DeviceProfile profile) {
        this(profile.getId(), profile.getTenantId(), profile.getName(), profile.getImage(), profile.getDefaultDashboardId(),
                profile.getType(), profile.getTransportType());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceProfileInfo` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
