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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AssetProfileEntity` 是 ThingsBoard DAO 中表示资产配置持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.ASSET_PROFILE_TABLE_NAME)
public final class AssetProfileEntity extends BaseSqlEntity<AssetProfile> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_NAME_PROPERTY)
    private String name;

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_IMAGE_PROPERTY)
    private String image;

    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    /**
     * 当前对象是否为默认项。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultRuleChainId;

    /**
     * 仪表盘ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY)
    private UUID defaultDashboardId;

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY)
    private String defaultQueueName;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultEdgeRuleChainId;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `AssetProfileEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AssetProfileEntity() {
        super();
    }

    /**
     * 功能：创建 `AssetProfileEntity` 实例，并初始化必要字段。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：新创建的对象实例。
     */
    public AssetProfileEntity(AssetProfile assetProfile) {
        if (assetProfile.getId() != null) {
            this.setUuid(assetProfile.getId().getId());
        }
        if (assetProfile.getTenantId() != null) {
            this.tenantId = assetProfile.getTenantId().getId();
        }
        this.setCreatedTime(assetProfile.getCreatedTime());
        this.name = assetProfile.getName();
        this.image = assetProfile.getImage();
        this.description = assetProfile.getDescription();
        this.isDefault = assetProfile.isDefault();
        if (assetProfile.getDefaultRuleChainId() != null) {
            this.defaultRuleChainId = assetProfile.getDefaultRuleChainId().getId();
        }
        if (assetProfile.getDefaultDashboardId() != null) {
            this.defaultDashboardId = assetProfile.getDefaultDashboardId().getId();
        }
        this.defaultQueueName = assetProfile.getDefaultQueueName();
        if (assetProfile.getDefaultEdgeRuleChainId() != null) {
            this.defaultEdgeRuleChainId = assetProfile.getDefaultEdgeRuleChainId().getId();
        }
        if (assetProfile.getExternalId() != null) {
            this.externalId = assetProfile.getExternalId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile toData() {
        AssetProfile assetProfile = new AssetProfile(new AssetProfileId(this.getUuid()));
        assetProfile.setCreatedTime(createdTime);
        if (tenantId != null) {
            assetProfile.setTenantId(TenantId.fromUUID(tenantId));
        }
        assetProfile.setName(name);
        assetProfile.setImage(image);
        assetProfile.setDescription(description);
        assetProfile.setDefault(isDefault);
        assetProfile.setDefaultQueueName(defaultQueueName);
        if (defaultRuleChainId != null) {
            assetProfile.setDefaultRuleChainId(new RuleChainId(defaultRuleChainId));
        }
        if (defaultDashboardId != null) {
            assetProfile.setDefaultDashboardId(new DashboardId(defaultDashboardId));
        }
        if (defaultEdgeRuleChainId != null) {
            assetProfile.setDefaultEdgeRuleChainId(new RuleChainId(defaultEdgeRuleChainId));
        }
        if (externalId != null) {
            assetProfile.setExternalId(new AssetProfileId(externalId));
        }

        return assetProfile;
    }
}
