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
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `DashboardEntity` 是 ThingsBoard DAO 中表示仪表盘持久化结构的实体类型。
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
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.DASHBOARD_TABLE_NAME)
public final class DashboardEntity extends BaseSqlEntity<Dashboard> {

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
     * `configuration`，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.DASHBOARD_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `DashboardEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public DashboardEntity() {
        super();
    }

    /**
     * 功能：创建 `DashboardEntity` 实例，并初始化必要字段。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：新创建的对象实例。
     */
    public DashboardEntity(Dashboard dashboard) {
        if (dashboard.getId() != null) {
            this.setUuid(dashboard.getId().getId());
        }
        this.setCreatedTime(dashboard.getCreatedTime());
        if (dashboard.getTenantId() != null) {
            this.tenantId = dashboard.getTenantId().getId();
        }
        this.title = dashboard.getTitle();
        this.image = dashboard.getImage();
        if (dashboard.getAssignedCustomers() != null) {
            try {
                this.assignedCustomers = JacksonUtil.toString(dashboard.getAssignedCustomers());
            } catch (IllegalArgumentException e) {
                log.error("Unable to serialize assigned customers to string!", e);
            }
        }
        this.mobileHide = dashboard.isMobileHide();
        this.mobileOrder = dashboard.getMobileOrder();
        this.configuration = dashboard.getConfiguration();
        if (dashboard.getExternalId() != null) {
            this.externalId = dashboard.getExternalId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Dashboard toData() {
        Dashboard dashboard = new Dashboard(new DashboardId(this.getUuid()));
        dashboard.setCreatedTime(this.getCreatedTime());
        if (tenantId != null) {
            dashboard.setTenantId(TenantId.fromUUID(tenantId));
        }
        dashboard.setTitle(title);
        dashboard.setImage(image);
        if (!StringUtils.isEmpty(assignedCustomers)) {
            try {
                dashboard.setAssignedCustomers(JacksonUtil.fromString(assignedCustomers, assignedCustomersType));
            } catch (IllegalArgumentException e) {
                log.warn("Unable to parse assigned customers!", e);
            }
        }
        dashboard.setMobileHide(mobileHide);
        dashboard.setMobileOrder(mobileOrder);
        dashboard.setConfiguration(configuration);
        if (externalId != null) {
            dashboard.setExternalId(new DashboardId(externalId));
        }
        return dashboard;
    }
}
