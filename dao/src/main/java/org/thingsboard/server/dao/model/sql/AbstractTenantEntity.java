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
 * 1. 类目的：`AbstractTenantEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
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

/*
 * 本类总结：
 * 1. 核心职责：`AbstractTenantEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
