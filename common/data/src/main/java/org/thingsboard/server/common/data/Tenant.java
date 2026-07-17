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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. `Tenant` 是 ThingsBoard Common Data 中承载租户信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `ContactBased`、`HasTenantId`、`HasTitle`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
public class Tenant extends ContactBased<TenantId> implements HasTenantId, HasTitle {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8057243243859922101L;

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "title")
    @NoXss
    @ApiModelProperty(position = 3, required = true, value = "Title of the tenant", example = "Company A")
    private String title;
    /**
     * `region` 字段，保存当前对象的对应属性。
     */
    @NoXss
    @Length(fieldName = "region")
    @ApiModelProperty(position = 5, value = "Geo region of the tenant", example = "North America")
    private String region;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 6, value = "JSON object with Tenant Profile Id")
    private TenantProfileId tenantProfileId;

    /**
     * 功能：创建 `Tenant` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Tenant() {
        super();
    }

    /**
     * 功能：创建 `Tenant` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Tenant(TenantId id) {
        super(id);
    }

    /**
     * 功能：创建 `Tenant` 实例，并初始化必要字段。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * 返回：新创建的对象实例。
     */
    public Tenant(Tenant tenant) {
        super(tenant);
        this.title = tenant.getTitle();
        this.region = tenant.getRegion();
        this.tenantProfileId = tenant.getTenantProfileId();
    }

    /**
     * 功能：获取`Title`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 功能：更新`Title`。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：无。
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    @JsonIgnore
    public TenantId getTenantId() {
        return getId();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    @ApiModelProperty(position = 4, value = "Name of the tenant. Read-only, duplicated from title for backward compatibility", example = "Company A", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    /**
     * 功能：获取`Region`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getRegion() {
        return region;
    }

    /**
     * 功能：更新`Region`。
     * 参数：
     * - `region`：`region` 参数。
     * 返回：无。
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantProfileId getTenantProfileId() {
        return tenantProfileId;
    }

    /**
     * 功能：更新租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：无。
     */
    public void setTenantProfileId(TenantProfileId tenantProfileId) {
        this.tenantProfileId = tenantProfileId;
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the tenant Id. " +
            "Specify this field to update the tenant. " +
            "Referencing non-existing tenant Id will cause error. " +
            "Omit this field to create new tenant." )
    @Override
    public TenantId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the tenant creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取`Country`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 7, value = "Country", example = "US")
    @Override
    public String getCountry() {
        return super.getCountry();
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 8, value = "State", example = "NY")
    @Override
    public String getState() {
        return super.getState();
    }

    /**
     * 功能：获取`City`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 9, value = "City", example = "New York")
    @Override
    public String getCity() {
        return super.getCity();
    }

    /**
     * 功能：获取`Address`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 10, value = "Address Line 1", example = "42 Broadway Suite 12-400")
    @Override
    public String getAddress() {
        return super.getAddress();
    }

    /**
     * 功能：获取`Address2`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 11, value = "Address Line 2", example = "")
    @Override
    public String getAddress2() {
        return super.getAddress2();
    }

    /**
     * 功能：获取`Zip`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 12, value = "Zip code", example = "10004")
    @Override
    public String getZip() {
        return super.getZip();
    }

    /**
     * 功能：获取`Phone`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 13, value = "Phone number", example = "+1(415)777-7777")
    @Override
    public String getPhone() {
        return super.getPhone();
    }

    /**
     * 功能：获取邮箱。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 14, required = true, value = "Email", example = "example@company.com")
    @Override
    public String getEmail() {
        return super.getEmail();
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 15, value = "Additional parameters of the device", dataType = "com.fasterxml.jackson.databind.JsonNode")
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
        builder.append("Tenant [title=");
        builder.append(title);
        builder.append(", region=");
        builder.append(region);
        builder.append(", tenantProfileId=");
        builder.append(tenantProfileId);
        builder.append(", additionalInfo=");
        builder.append(getAdditionalInfo());
        builder.append(", country=");
        builder.append(country);
        builder.append(", state=");
        builder.append(state);
        builder.append(", city=");
        builder.append(city);
        builder.append(", address=");
        builder.append(address);
        builder.append(", address2=");
        builder.append(address2);
        builder.append(", zip=");
        builder.append(zip);
        builder.append(", phone=");
        builder.append(phone);
        builder.append(", email=");
        builder.append(email);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
