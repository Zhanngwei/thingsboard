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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `CustomerEntity` 是 ThingsBoard DAO 中表示客户持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.CUSTOMER_TABLE_NAME)
public final class CustomerEntity extends BaseSqlEntity<Customer> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.CUSTOMER_TENANT_ID_PROPERTY)
    private UUID tenantId;
    
    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.CUSTOMER_TITLE_PROPERTY)
    private String title;

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
    @Column(name = ModelConstants.CUSTOMER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `CustomerEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public CustomerEntity() {
        super();
    }

    /**
     * 功能：创建 `CustomerEntity` 实例，并初始化必要字段。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：新创建的对象实例。
     */
    public CustomerEntity(Customer customer) {
        if (customer.getId() != null) {
            this.setUuid(customer.getId().getId());
        }
        this.setCreatedTime(customer.getCreatedTime());
        this.tenantId = customer.getTenantId().getId();
        this.title = customer.getTitle();
        this.country = customer.getCountry();
        this.state = customer.getState();
        this.city = customer.getCity();
        this.address = customer.getAddress();
        this.address2 = customer.getAddress2();
        this.zip = customer.getZip();
        this.phone = customer.getPhone();
        this.email = customer.getEmail();
        this.additionalInfo = customer.getAdditionalInfo();
        if (customer.getExternalId() != null) {
            this.externalId = customer.getExternalId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Customer toData() {
        Customer customer = new Customer(new CustomerId(this.getUuid()));
        customer.setCreatedTime(createdTime);
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle(title);
        customer.setCountry(country);
        customer.setState(state);
        customer.setCity(city);
        customer.setAddress(address);
        customer.setAddress2(address2);
        customer.setZip(zip);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            customer.setExternalId(new CustomerId(externalId));
        }
        return customer;
    }

}
