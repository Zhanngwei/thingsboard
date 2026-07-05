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
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * 中文说明：
 * 1. 类目的：`User` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
public class User extends BaseDataWithAdditionalInfo<UserId> implements HasName, HasTenantId, HasCustomerId, NotificationRecipient {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8250339805336035966L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    private CustomerId customerId;
    /**
     * 邮箱，用于展示或标识当前对象。
     */
    private String email;
    private Authority authority;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "first name")
    private String firstName;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "last name")
    private String lastName;
    /**
     * `phone` 字段，保存当前对象的对应属性。
     */
    @NoXss
    private String phone;

    /**
     * 功能：创建 `User` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public User() {
        super();
    }

    /**
     * 功能：创建 `User` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public User(UserId id) {
        super(id);
    }

    /**
     * 功能：创建 `User` 实例，并初始化必要字段。
     * 参数：
     * - `user`：`user` 参数。
     * 返回：新创建的对象实例。
     */
    public User(User user) {
        super(user);
        this.tenantId = user.getTenantId();
        this.customerId = user.getCustomerId();
        this.email = user.getEmail();
        this.authority = user.getAuthority();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = user.getPhone();
    }


    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the User Id. " +
            "Specify this field to update the device. " +
            "Referencing non-existing User Id will cause error. " +
            "Omit this field to create new customer.")
    @Override
    public UserId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the user creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "JSON object with the Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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
    @ApiModelProperty(position = 4, value = "JSON object with the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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
     * 功能：获取邮箱。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 5, required = true, value = "Email of the user", example = "user@example.com")
    public String getEmail() {
        return email;
    }

    /**
     * 功能：更新邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：无。
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, accessMode = ApiModelProperty.AccessMode.READ_ONLY, value = "Duplicates the email of the user, readonly", example = "user@example.com")
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return email;
    }

    /**
     * 功能：获取`Authority`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 7, required = true, value = "Authority", example = "SYS_ADMIN, TENANT_ADMIN or CUSTOMER_USER")
    public Authority getAuthority() {
        return authority;
    }

    /**
     * 功能：更新`Authority`。
     * 参数：
     * - `authority`：`authority` 参数。
     * 返回：无。
     */
    public void setAuthority(Authority authority) {
        this.authority = authority;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 8, required = false, value = "First name of the user", example = "John")
    public String getFirstName() {
        return firstName;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `firstName`：名称。
     * 返回：无。
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 9, required = false, value = "Last name of the user", example = "Doe")
    public String getLastName() {
        return lastName;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `lastName`：名称。
     * 返回：无。
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * 功能：获取`Phone`。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 10, required = false, value = "Phone number of the user", example = "38012345123")
    public String getPhone() {
        return phone;
    }

    /**
     * 功能：更新`Phone`。
     * 参数：
     * - `phone`：`phone` 参数。
     * 返回：无。
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 11, value = "Additional parameters of the user", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    /**
     * 功能：获取`Title`。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    public String getTitle() {
        return getTitle(email, firstName, lastName);
    }

    /**
     * 功能：获取`Title`。
     * 参数：
     * - `email`：`email` 参数。
     * - `firstName`：名称。
     * - `lastName`：名称。
     * 返回：文本结果。
     */
    public static String getTitle(String email, String firstName, String lastName) {
        String title = "";
        if (isNotEmpty(firstName)) {
            title += firstName;
        }
        if (isNotEmpty(lastName)) {
            if (!title.isEmpty()) {
                title += " ";
            }
            title += lastName;
        }
        if (title.isEmpty()) {
            title = email;
        }
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
        builder.append("User [tenantId=");
        builder.append(tenantId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", email=");
        builder.append(email);
        builder.append(", authority=");
        builder.append(authority);
        builder.append(", firstName=");
        builder.append(firstName);
        builder.append(", lastName=");
        builder.append(lastName);
        builder.append(", additionalInfo=");
        builder.append(getAdditionalInfo());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

    /**
     * 功能：判断Actor 系统。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isSystemAdmin() {
        return tenantId == null || EntityId.NULL_UUID.equals(tenantId.getId());
    }

    /**
     * 功能：判断租户。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isTenantAdmin() {
        return !isSystemAdmin() && (customerId == null || EntityId.NULL_UUID.equals(customerId.getId()));
    }

    /**
     * 功能：判断客户。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isCustomerUser() {
        return !isSystemAdmin() && !isTenantAdmin();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`User` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
