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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AbstractTenantEntity` 是 ThingsBoard DAO 中表示租户持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Tenant`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractTenantEntity<T extends Tenant> extends BaseSqlEntity<T> {

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.TENANT_TITLE_PROPERTY)
    private String title;

    /**
     * `region` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.TENANT_REGION_PROPERTY)
    private String region;

    /**
     * `country` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.COUNTRY_PROPERTY)
    private String country;

    /**
     * 状态，表示当前对象所处状态。
     */
    @Column(name = ModelConstants.STATE_PROPERTY)
    private String state;

    /**
     * `city` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.CITY_PROPERTY)
    private String city;

    /**
     * `address` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.ADDRESS_PROPERTY)
    private String address;

    /**
     * `address2` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.ADDRESS2_PROPERTY)
    private String address2;

    /**
     * `zip` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.ZIP_PROPERTY)
    private String zip;

    /**
     * `phone` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.PHONE_PROPERTY)
    private String phone;

    /**
     * 邮箱，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.EMAIL_PROPERTY)
    private String email;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.TENANT_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.TENANT_TENANT_PROFILE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantProfileId;

    /**
     * 功能：创建 `AbstractTenantEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractTenantEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractTenantEntity` 实例，并初始化必要字段。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * 返回：新创建的对象实例。
     */
    public AbstractTenantEntity(Tenant tenant) {
        if (tenant.getId() != null) {
            this.setUuid(tenant.getId().getId());
        }
        this.setCreatedTime(tenant.getCreatedTime());
        this.title = tenant.getTitle();
        this.region = tenant.getRegion();
        this.country = tenant.getCountry();
        this.state = tenant.getState();
        this.city = tenant.getCity();
        this.address = tenant.getAddress();
        this.address2 = tenant.getAddress2();
        this.zip = tenant.getZip();
        this.phone = tenant.getPhone();
        this.email = tenant.getEmail();
        this.additionalInfo = tenant.getAdditionalInfo();
        if (tenant.getTenantProfileId() != null) {
            this.tenantProfileId = tenant.getTenantProfileId().getId();
        }
    }

    /**
     * 功能：创建 `AbstractTenantEntity` 实例，并初始化必要字段。
     * 参数：
     * - `tenantEntity`：租户信息或租户标识。
     * 返回：新创建的对象实例。
     */
    public AbstractTenantEntity(TenantEntity tenantEntity) {
        this.setId(tenantEntity.getId());
        this.setCreatedTime(tenantEntity.getCreatedTime());
        this.title = tenantEntity.getTitle();
        this.region = tenantEntity.getRegion();
        this.country = tenantEntity.getCountry();
        this.state = tenantEntity.getState();
        this.city = tenantEntity.getCity();
        this.address = tenantEntity.getAddress();
        this.address2 = tenantEntity.getAddress2();
        this.zip = tenantEntity.getZip();
        this.phone = tenantEntity.getPhone();
        this.email = tenantEntity.getEmail();
        this.additionalInfo = tenantEntity.getAdditionalInfo();
        this.tenantProfileId = tenantEntity.getTenantProfileId();
    }

    /**
     * 功能：执行 `toTenant` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Tenant toTenant() {
        Tenant tenant = new Tenant(TenantId.fromUUID(this.getUuid()));
        tenant.setCreatedTime(createdTime);
        tenant.setTitle(title);
        tenant.setRegion(region);
        tenant.setCountry(country);
        tenant.setState(state);
        tenant.setCity(city);
        tenant.setAddress(address);
        tenant.setAddress2(address2);
        tenant.setZip(zip);
        tenant.setPhone(phone);
        tenant.setEmail(email);
        tenant.setAdditionalInfo(additionalInfo);
        if (tenantProfileId != null) {
            tenant.setTenantProfileId(new TenantProfileId(tenantProfileId));
        }
        return tenant;
    }


}
