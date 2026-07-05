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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.customer.CustomerServiceImpl;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`CustomerDataValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@Component
public class CustomerDataValidator extends DataValidator<Customer> {

    /**
     * 客户，用于读取或保存对应领域对象。
     */
    @Autowired
    private CustomerDao customerDao;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantService tenantService;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customer`：`customer` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, Customer customer) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.CUSTOMER);
        customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                c -> {
                    throw new DataValidationException("Customer with such title already exists!");
                }
        );
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customer`：`customer` 参数。
     * 返回：判断结果。
     */
    @Override
    protected Customer validateUpdate(TenantId tenantId, Customer customer) {
        Optional<Customer> customerOpt = customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle());
        customerOpt.ifPresent(
                c -> {
                    if (!c.getId().equals(customer.getId())) {
                        throw new DataValidationException("Customer with such title already exists!");
                    }
                }
        );
        return customerOpt.orElse(null);
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customer`：`customer` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, Customer customer) {
        validateString("Customer title", customer.getTitle());
        if (customer.getTitle().equals(CustomerServiceImpl.PUBLIC_CUSTOMER_TITLE)) {
            throw new DataValidationException("'Public' title for customer is system reserved!");
        }
        if (!StringUtils.isEmpty(customer.getEmail())) {
            validateEmail(customer.getEmail());
        }
        if (customer.getTenantId() == null) {
            throw new DataValidationException("Customer should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(customer.getTenantId())) {
                throw new DataValidationException("Customer is referencing to non-existent tenant!");
            }
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CustomerDataValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
