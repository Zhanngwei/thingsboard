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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserService;

/**
 * 中文说明：
 * 1. 类目的：`UserDataValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@Component
public class UserDataValidator extends DataValidator<User> {

    /**
     * 用户，用于读取或保存对应领域对象。
     */
    @Autowired
    private UserDao userDao;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private UserService userService;

    /**
     * 客户，用于读取或保存对应领域对象。
     */
    @Autowired
    private CustomerDao customerDao;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private TenantService tenantService;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, User user) {
        if (!user.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            validateNumberOfEntitiesPerTenant(tenantId, EntityType.USER);
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：判断结果。
     */
    @Override
    protected User validateUpdate(TenantId tenantId, User user) {
        User old = userDao.findById(user.getTenantId(), user.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing user!");
        }
        if (!old.getTenantId().equals(user.getTenantId())) {
            throw new DataValidationException("Can't update user tenant id!");
        }
        if (!old.getAuthority().equals(user.getAuthority())) {
            throw new DataValidationException("Can't update user authority!");
        }
        if (!old.getCustomerId().equals(user.getCustomerId())) {
            throw new DataValidationException("Can't update user customer id!");
        }
        return old;
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `requestTenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId requestTenantId, User user) {
        if (StringUtils.isEmpty(user.getEmail())) {
            throw new DataValidationException("User email should be specified!");
        }

        validateEmail(user.getEmail());

        Authority authority = user.getAuthority();
        if (authority == null) {
            throw new DataValidationException("User authority isn't defined!");
        }
        TenantId tenantId = user.getTenantId();
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);
            user.setTenantId(tenantId);
        }
        CustomerId customerId = user.getCustomerId();
        if (customerId == null) {
            customerId = new CustomerId(ModelConstants.NULL_UUID);
            user.setCustomerId(customerId);
        }

        switch (authority) {
            case SYS_ADMIN:
                if (!tenantId.getId().equals(ModelConstants.NULL_UUID)
                        || !customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("System administrator can't be assigned neither to tenant nor to customer!");
                }
                break;
            case TENANT_ADMIN:
                if (tenantId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Tenant administrator should be assigned to tenant!");
                } else if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Tenant administrator can't be assigned to customer!");
                }
                break;
            case CUSTOMER_USER:
                if (tenantId.getId().equals(ModelConstants.NULL_UUID)
                        || customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Customer user should be assigned to customer!");
                }
                break;
            default:
                break;
        }

        User existentUserWithEmail = userService.findUserByEmail(tenantId, user.getEmail());
        if (existentUserWithEmail != null && !isSameData(existentUserWithEmail, user)) {
            throw new DataValidationException("User with email '" + user.getEmail() + "' "
                    + " already present in database!");
        }
        if (!tenantId.getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(user.getTenantId())) {
                throw new DataValidationException("User is referencing to non-existent tenant!");
            }
        }
        if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, user.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("User is referencing to non-existent customer!");
            } else if (!customer.getTenantId().getId().equals(tenantId.getId())) {
                throw new DataValidationException("User can't be assigned to customer from different tenant!");
            }
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`UserDataValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
