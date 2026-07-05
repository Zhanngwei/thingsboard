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
package org.thingsboard.server.common.data.asset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasRuleEngineProfile;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. 类目的：`AssetProfile` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
@ToString(exclude = {"image"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AssetProfile extends BaseData<AssetProfileId> implements HasName, HasTenantId, HasRuleEngineProfile, ExportableEntity<AssetProfileId>, HasImage {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 6998485460273302018L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id that owns the profile.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 4, value = "Unique Asset Profile Name in scope of Tenant.", example = "Building")
    private String name;
    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @NoXss
    @ApiModelProperty(position = 11, value = "Asset Profile description. ")
    private String description;
    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @ApiModelProperty(position = 12, value = "Either URL or Base64 data of the icon. Used in the mobile application to visualize set of asset profiles in the grid view. ")
    private String image;
    private boolean isDefault;
    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 7, value = "Reference to the rule chain. " +
            "If present, the specified rule chain will be used to process all messages related to asset, including asset updates, telemetry, attribute updates, etc. " +
            "Otherwise, the root rule chain will be used to process those messages.")
    private RuleChainId defaultRuleChainId;
    /**
     * 仪表盘ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 6, value = "Reference to the dashboard. Used in the mobile application to open the default dashboard when user navigates to asset details.")
    private DashboardId defaultDashboardId;

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    @NoXss
    @ApiModelProperty(position = 8, value = "Rule engine queue name. " +
            "If present, the specified queue will be used to store all unprocessed messages related to asset, including asset updates, telemetry, attribute updates, etc. " +
            "Otherwise, the 'Main' queue will be used to store those messages.")
    private String defaultQueueName;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 13, value = "Reference to the edge rule chain. " +
            "If present, the specified edge rule chain will be used on the edge to process all messages related to asset, including asset updates, telemetry, attribute updates, etc. " +
            "Otherwise, the edge root rule chain will be used to process those messages.")
    private RuleChainId defaultEdgeRuleChainId;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    private AssetProfileId externalId;

    /**
     * 功能：创建 `AssetProfile` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AssetProfile() {
        super();
    }

    /**
     * 功能：创建 `AssetProfile` 实例，并初始化必要字段。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：新创建的对象实例。
     */
    public AssetProfile(AssetProfileId assetProfileId) {
        super(assetProfileId);
    }

    /**
     * 功能：创建 `AssetProfile` 实例，并初始化必要字段。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：新创建的对象实例。
     */
    public AssetProfile(AssetProfile assetProfile) {
        super(assetProfile);
        this.tenantId = assetProfile.getTenantId();
        this.name = assetProfile.getName();
        this.description = assetProfile.getDescription();
        this.image = assetProfile.getImage();
        this.isDefault = assetProfile.isDefault();
        this.defaultRuleChainId = assetProfile.getDefaultRuleChainId();
        this.defaultDashboardId = assetProfile.getDefaultDashboardId();
        this.defaultQueueName = assetProfile.getDefaultQueueName();
        this.defaultEdgeRuleChainId = assetProfile.getDefaultEdgeRuleChainId();
        this.externalId = assetProfile.getExternalId();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the asset profile Id. " +
            "Specify this field to update the asset profile. " +
            "Referencing non-existing asset profile Id will cause error. " +
            "Omit this field to create new asset profile.")
    @Override
    public AssetProfileId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the profile creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：判断`Default`。
     * 参数：无。
     * 返回：判断结果。
     */
    @ApiModelProperty(position = 5, value = "Used to mark the default profile. Default profile is used when the asset profile is not specified during asset creation.")
    public boolean isDefault() {
        return isDefault;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AssetProfile` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
