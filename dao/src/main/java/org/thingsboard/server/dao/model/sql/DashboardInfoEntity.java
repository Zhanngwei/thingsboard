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

import com.fasterxml.jackson.databind.JavaType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `DashboardInfoEntity` 是 ThingsBoard DAO 中表示仪表盘持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.DASHBOARD_TABLE_NAME)
public class DashboardInfoEntity extends BaseSqlEntity<DashboardInfo> {

    private static final JavaType assignedCustomersType =
            JacksonUtil.constructCollectionType(HashSet.class, ShortCustomerInfo.class);

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DASHBOARD_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.DASHBOARD_TITLE_PROPERTY)
    private String title;

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @Column(name = ModelConstants.DASHBOARD_IMAGE_PROPERTY)
    private String image;

    /**
     * `assignedCustomers` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY)
    private String assignedCustomers;

    /**
     * 当前内容是否隐藏。
     */
    @Column(name = ModelConstants.DASHBOARD_MOBILE_HIDE_PROPERTY)
    private boolean mobileHide;

    /**
     * `mobileOrder` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.DASHBOARD_MOBILE_ORDER_PROPERTY)
    private Integer mobileOrder;

    /**
     * 功能：创建 `DashboardInfoEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public DashboardInfoEntity() {
        super();
    }

    /**
     * 功能：创建 `DashboardInfoEntity` 实例，并初始化必要字段。
     * 参数：
     * - `dashboardInfo`：`dashboardInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public DashboardInfoEntity(DashboardInfo dashboardInfo) {
        if (dashboardInfo.getId() != null) {
            this.setUuid(dashboardInfo.getId().getId());
        }
        this.setCreatedTime(dashboardInfo.getCreatedTime());
        if (dashboardInfo.getTenantId() != null) {
            this.tenantId = dashboardInfo.getTenantId().getId();
        }
        this.title = dashboardInfo.getTitle();
        this.image = dashboardInfo.getImage();
        if (dashboardInfo.getAssignedCustomers() != null) {
            try {
                this.assignedCustomers = JacksonUtil.toString(dashboardInfo.getAssignedCustomers());
            } catch (IllegalArgumentException e) {
                log.error("Unable to serialize assigned customers to string!", e);
            }
        }
        this.mobileHide = dashboardInfo.isMobileHide();
        this.mobileOrder = dashboardInfo.getMobileOrder();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DashboardInfo toData() {
        DashboardInfo dashboardInfo = new DashboardInfo(new DashboardId(this.getUuid()));
        dashboardInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            dashboardInfo.setTenantId(TenantId.fromUUID(tenantId));
        }
        dashboardInfo.setTitle(title);
        dashboardInfo.setImage(image);
        if (!StringUtils.isEmpty(assignedCustomers)) {
            try {
                dashboardInfo.setAssignedCustomers(JacksonUtil.fromString(assignedCustomers, assignedCustomersType));
            } catch (IllegalArgumentException e) {
                log.warn("Unable to parse assigned customers!", e);
            }
        }
        dashboardInfo.setMobileHide(mobileHide);
        dashboardInfo.setMobileOrder(mobileOrder);
        return dashboardInfo;
    }

}
