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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`DashboardInfo` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
public class DashboardInfo extends BaseData<DashboardId> implements HasName, HasTenantId, HasTitle, HasImage {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -9080404114760433799L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @NoXss
    @Length(fieldName = "title")
    private String title;
    private String image;
    /**
     * `assignedCustomers`集合，用于去重保存或快速判断对象是否存在。
     */
    @Valid
    private Set<ShortCustomerInfo> assignedCustomers;
    private boolean mobileHide;
    /**
     * `mobileOrder` 字段，保存当前对象的对应属性。
     */
    private Integer mobileOrder;

    /**
     * 功能：创建 `DashboardInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public DashboardInfo() {
        super();
    }

    /**
     * 功能：创建 `DashboardInfo` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public DashboardInfo(DashboardId id) {
        super(id);
    }

    /**
     * 功能：创建 `DashboardInfo` 实例，并初始化必要字段。
     * 参数：
     * - `dashboardInfo`：`dashboardInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public DashboardInfo(DashboardInfo dashboardInfo) {
        super(dashboardInfo);
        this.tenantId = dashboardInfo.getTenantId();
        this.title = dashboardInfo.getTitle();
        this.image = dashboardInfo.getImage();
        this.assignedCustomers = dashboardInfo.getAssignedCustomers();
        this.mobileHide = dashboardInfo.isMobileHide();
        this.mobileOrder = dashboardInfo.getMobileOrder();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the dashboard Id. " +
            "Specify existing dashboard Id to update the dashboard. " +
            "Referencing non-existing dashboard id will cause error. " +
            "Omit this field to create new dashboard.")
    @Override
    public DashboardId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the dashboard creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Tenant Id of the dashboard can't be changed.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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
    @ApiModelProperty(position = 4, required = true, value = "Title of the dashboard.")
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
     * 功能：获取图片资源。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 8, value = "Thumbnail picture for rendering of the dashboards in a grid view on mobile devices.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getImage() {
        return image;
    }

    /**
     * 功能：更新图片资源。
     * 参数：
     * - `image`：`image` 参数。
     * 返回：无。
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * 功能：获取`Assigned Customers`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiModelProperty(position = 5, value = "List of assigned customers with their info.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Set<ShortCustomerInfo> getAssignedCustomers() {
        return assignedCustomers;
    }

    /**
     * 功能：更新`Assigned Customers`。
     * 参数：
     * - `assignedCustomers`：`assignedCustomers` 参数。
     * 返回：无。
     */
    public void setAssignedCustomers(Set<ShortCustomerInfo> assignedCustomers) {
        this.assignedCustomers = assignedCustomers;
    }

    /**
     * 功能：判断`Mobile Hide`。
     * 参数：无。
     * 返回：判断结果。
     */
    @ApiModelProperty(position = 6, value = "Hide dashboard from mobile devices. Useful if the dashboard is not designed for small screens.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public boolean isMobileHide() {
        return mobileHide;
    }

    /**
     * 功能：更新`Mobile Hide`。
     * 参数：
     * - `mobileHide`：`mobileHide` 参数。
     * 返回：无。
     */
    public void setMobileHide(boolean mobileHide) {
        this.mobileHide = mobileHide;
    }

    /**
     * 功能：获取`Mobile Order`。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 7, value = "Order on mobile devices. Useful to adjust sorting of the dashboards for mobile applications", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Integer getMobileOrder() {
        return mobileOrder;
    }

    /**
     * 功能：更新`Mobile Order`。
     * 参数：
     * - `mobileOrder`：`mobileOrder` 参数。
     * 返回：无。
     */
    public void setMobileOrder(Integer mobileOrder) {
        this.mobileOrder = mobileOrder;
    }

    /**
     * 功能：判断客户。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：判断结果。
     */
    public boolean isAssignedToCustomer(CustomerId customerId) {
        return this.assignedCustomers != null && this.assignedCustomers.contains(new ShortCustomerInfo(customerId, null, false));
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    public ShortCustomerInfo getAssignedCustomerInfo(CustomerId customerId) {
        if (this.assignedCustomers != null) {
            for (ShortCustomerInfo customerInfo : this.assignedCustomers) {
                if (customerInfo.getCustomerId().equals(customerId)) {
                    return customerInfo;
                }
            }
        }
        return null;
    }

    /**
     * 功能：保存或创建客户。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：判断结果。
     */
    public boolean addAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            return false;
        } else {
            if (this.assignedCustomers == null) {
                this.assignedCustomers = new HashSet<>();
            }
            this.assignedCustomers.add(customerInfo);
            return true;
        }
    }

    /**
     * 功能：更新客户。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：判断结果。
     */
    public boolean updateAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            this.assignedCustomers.add(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 功能：删除或清理客户。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：判断结果。
     */
    public boolean removeAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 4, value = "Same as title of the dashboard. Read-only field. Update the 'title' to change the 'name' of the dashboard.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DashboardInfo that = (DashboardInfo) o;
        return mobileHide == that.mobileHide
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(title, that.title)
                && Objects.equals(image, that.image)
                && Objects.equals(assignedCustomers, that.assignedCustomers)
                && Objects.equals(mobileOrder, that.mobileOrder);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DashboardInfo [tenantId=");
        builder.append(tenantId);
        builder.append(", title=");
        builder.append(title);
        builder.append("]");
        return builder.toString();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardInfo` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
