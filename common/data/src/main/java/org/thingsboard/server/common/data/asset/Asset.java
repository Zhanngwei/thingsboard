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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `Asset` 是 ThingsBoard Common Data 中承载资产信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseDataWithAdditionalInfo`、`HasLabel`、`HasTenantId`、`HasCustomerId`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
public class Asset extends BaseDataWithAdditionalInfo<AssetId> implements HasLabel, HasTenantId, HasCustomerId, ExportableEntity<AssetId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2807343040519543363L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    private CustomerId customerId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    private String name;
    /**
     * 类型，用于区分不同处理分支。
     */
    @NoXss
    @Length(fieldName = "type")
    private String type;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @NoXss
    @Length(fieldName = "label")
    private String label;

    /**
     * 资产配置ID，用于定位对应业务对象。
     */
    private AssetProfileId assetProfileId;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Getter @Setter
    private AssetId externalId;

    /**
     * 功能：创建 `Asset` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Asset() {
        super();
    }

    /**
     * 功能：创建 `Asset` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Asset(AssetId id) {
        super(id);
    }

    /**
     * 功能：创建 `Asset` 实例，并初始化必要字段。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：新创建的对象实例。
     */
    public Asset(Asset asset) {
        super(asset);
        this.tenantId = asset.getTenantId();
        this.customerId = asset.getCustomerId();
        this.name = asset.getName();
        this.type = asset.getType();
        this.label = asset.getLabel();
        this.assetProfileId = asset.getAssetProfileId();
        this.externalId = asset.getExternalId();
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：无。
     */
    public void update(Asset asset) {
        this.tenantId = asset.getTenantId();
        this.customerId = asset.getCustomerId();
        this.name = asset.getName();
        this.type = asset.getType();
        this.label = asset.getLabel();
        this.assetProfileId = asset.getAssetProfileId();
        Optional.ofNullable(asset.getAdditionalInfo()).ifPresent(this::setAdditionalInfo);
        this.externalId = asset.getExternalId();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the asset Id. " +
            "Specify this field to update the asset. " +
            "Referencing non-existing asset Id will cause error. " +
            "Omit this field to create new asset.")
    @Override
    public AssetId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the asset creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * 功能：更新租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignAssetToCustomer' to change the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public CustomerId getCustomerId() {
        return customerId;
    }

    /**
     * 功能：更新客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 5, required = true, value = "Unique Asset Name in scope of Tenant", example = "Empire State Building")
    @Override
    public String getName() {
        return name;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, value = "Asset type", example = "Building")
    public String getType() {
        return type;
    }

    /**
     * 功能：更新类型。
     * 参数：
     * - `type`：类型。
     * 返回：无。
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 功能：获取显示标签。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 7, value = "Label that may be used in widgets", example = "NY Building")
    public String getLabel() {
        return label;
    }

    /**
     * 功能：更新显示标签。
     * 参数：
     * - `label`：`label` 参数。
     * 返回：无。
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * 功能：获取资产配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiModelProperty(position = 8, value = "JSON object with Asset Profile Id.")
    public AssetProfileId getAssetProfileId() {
        return assetProfileId;
    }

    /**
     * 功能：更新资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：无。
     */
    public void setAssetProfileId(AssetProfileId assetProfileId) {
        this.assetProfileId = assetProfileId;
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 9, value = "Additional parameters of the asset", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Asset [tenantId=");
        builder.append(tenantId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", label=");
        builder.append(label);
        builder.append(", assetProfileId=");
        builder.append(assetProfileId);
        builder.append(", additionalInfo=");
        builder.append(getAdditionalInfo());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
