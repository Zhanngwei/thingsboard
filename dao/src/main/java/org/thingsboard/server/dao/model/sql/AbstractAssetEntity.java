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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ASSET_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;

/**
 * 中文说明：
 * 1. `AbstractAssetEntity` 是 ThingsBoard DAO 中表示资产持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Asset`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractAssetEntity<T extends Asset> extends BaseSqlEntity<T> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ASSET_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Column(name = ASSET_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ASSET_NAME_PROPERTY)
    private String name;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = ASSET_TYPE_PROPERTY)
    private String type;

    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Column(name = ASSET_LABEL_PROPERTY)
    private String label;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.ASSET_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 资产配置ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ASSET_ASSET_PROFILE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID assetProfileId;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `AbstractAssetEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractAssetEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractAssetEntity` 实例，并初始化必要字段。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractAssetEntity(Asset asset) {
        if (asset.getId() != null) {
            this.setUuid(asset.getId().getId());
        }
        this.setCreatedTime(asset.getCreatedTime());
        if (asset.getTenantId() != null) {
            this.tenantId = asset.getTenantId().getId();
        }
        if (asset.getCustomerId() != null) {
            this.customerId = asset.getCustomerId().getId();
        }
        if (asset.getAssetProfileId() != null) {
            this.assetProfileId = asset.getAssetProfileId().getId();
        }
        this.name = asset.getName();
        this.type = asset.getType();
        this.label = asset.getLabel();
        this.additionalInfo = asset.getAdditionalInfo();
        if (asset.getExternalId() != null) {
            this.externalId = asset.getExternalId().getId();
        }
    }

    /**
     * 功能：创建 `AbstractAssetEntity` 实例，并初始化必要字段。
     * 参数：
     * - `assetEntity`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AbstractAssetEntity(AssetEntity assetEntity) {
        this.setId(assetEntity.getId());
        this.setCreatedTime(assetEntity.getCreatedTime());
        this.tenantId = assetEntity.getTenantId();
        this.customerId = assetEntity.getCustomerId();
        this.assetProfileId = assetEntity.getAssetProfileId();
        this.type = assetEntity.getType();
        this.name = assetEntity.getName();
        this.label = assetEntity.getLabel();
        this.additionalInfo = assetEntity.getAdditionalInfo();
        this.externalId = assetEntity.getExternalId();
    }

    /**
     * 功能：执行 `toAsset` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    protected Asset toAsset() {
        Asset asset = new Asset(new AssetId(id));
        asset.setCreatedTime(createdTime);
        if (tenantId != null) {
            asset.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            asset.setCustomerId(new CustomerId(customerId));
        }
        if (assetProfileId != null) {
            asset.setAssetProfileId(new AssetProfileId(assetProfileId));
        }
        asset.setName(name);
        asset.setType(type);
        asset.setLabel(label);
        asset.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            asset.setExternalId(new AssetId(externalId));
        }
        return asset;
    }

}
