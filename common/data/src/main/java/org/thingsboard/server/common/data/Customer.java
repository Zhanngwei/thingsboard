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
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. 类目的：`Customer` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@EqualsAndHashCode(callSuper = true)
public class Customer extends ContactBased<CustomerId> implements HasTenantId, ExportableEntity<CustomerId>, HasTitle {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -1599722990298929275L;

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @NoXss
    @Length(fieldName = "title")
    @ApiModelProperty(position = 3, required = true, value = "Title of the customer", example = "Company A")
    private String title;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 5, value = "JSON object with Tenant Id")
    private TenantId tenantId;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Getter @Setter
    private CustomerId externalId;

    /**
     * 功能：创建 `Customer` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Customer() {
        super();
    }

    /**
     * 功能：创建 `Customer` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Customer(CustomerId id) {
        super(id);
    }

    /**
     * 功能：创建 `Customer` 实例，并初始化必要字段。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：新创建的对象实例。
     */
    public Customer(Customer customer) {
        super(customer);
        this.tenantId = customer.getTenantId();
        this.title = customer.getTitle();
        this.externalId = customer.getExternalId();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
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
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the customer Id. " +
            "Specify this field to update the customer. " +
            "Referencing non-existing customer Id will cause error. " +
            "Omit this field to create new customer." )
    @Override
    public CustomerId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the customer creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取`Country`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, value = "Country", example = "US")
    @Override
    public String getCountry() {
        return super.getCountry();
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 7, value = "State", example = "NY")
    @Override
    public String getState() {
        return super.getState();
    }

    /**
     * 功能：获取`City`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 8, value = "City", example = "New York")
    @Override
    public String getCity() {
        return super.getCity();
    }

    /**
     * 功能：获取`Address`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 9, value = "Address Line 1", example = "42 Broadway Suite 12-400")
    @Override
    public String getAddress() {
        return super.getAddress();
    }

    /**
     * 功能：获取`Address2`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 10, value = "Address Line 2", example = "")
    @Override
    public String getAddress2() {
        return super.getAddress2();
    }

    /**
     * 功能：获取`Zip`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 11, value = "Zip code", example = "10004")
    @Override
    public String getZip() {
        return super.getZip();
    }

    /**
     * 功能：获取`Phone`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 12, value = "Phone number", example = "+1(415)777-7777")
    @Override
    public String getPhone() {
        return super.getPhone();
    }

    /**
     * 功能：获取邮箱。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 13, required = true, value = "Email", example = "example@company.com")
    @Override
    public String getEmail() {
        return super.getEmail();
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 14, value = "Additional parameters of the device", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    /**
     * 功能：判断`Public`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isPublic() {
        if (getAdditionalInfo() != null && getAdditionalInfo().has("isPublic")) {
            return getAdditionalInfo().get("isPublic").asBoolean();
        }

        return false;
    }

    /**
     * 功能：执行 `toShortCustomerInfo` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public ShortCustomerInfo toShortCustomerInfo() {
        return new ShortCustomerInfo(id, title, isPublic());
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    @JsonProperty(access = Access.READ_ONLY)
    @ApiModelProperty(position = 4, value = "Name of the customer. Read-only, duplicated from title for backward compatibility", example = "Company A", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getName() {
        return title;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Customer [title=");
        builder.append(title);
        builder.append(", tenantId=");
        builder.append(tenantId);
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

/*
 * 本类总结：
 * 1. 核心职责：`Customer` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
