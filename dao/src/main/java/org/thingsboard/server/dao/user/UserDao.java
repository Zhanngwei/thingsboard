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
package org.thingsboard.server.dao.user;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`UserDao` 是 ThingsBoard DAO 模块 中的租户、客户或用户持久化服务类型，用于管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 生命周期：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository。
 */
public interface UserDao extends Dao<User>, TenantEntityDao {

    /**
     * Save or update user object
     *
     * @param user the user object
     * @return saved user entity
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    User save(TenantId tenantId, User user);

    /**
     * Find user by email.
     *
     * @param email the email
     * @return the user entity
     */
    /**
     * 功能：获取邮箱。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    User findByEmail(TenantId tenantId, String email);

    /**
     * Find user by tenant id and email.
     *
     * @param tenantId the tenant id
     * @param email the email
     * @return the user entity
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    User findByTenantIdAndEmail(TenantId tenantId, String email);

    /**
     * Find users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find tenant admin users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink);

    /**
     * Find customer users by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of user entities
     */
    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find users for alarm assignment by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of user entities
     */
    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerIds`：数据列表。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findUsersByCustomerIds(UUID tenantId, List<CustomerId> customerIds, PageLink pageLink);

    /**
     * 功能：获取`All`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findAll(PageLink pageLink);

    /**
     * 功能：获取`All By Authority`。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findAllByAuthority(Authority authority, PageLink pageLink);

    /**
     * 功能：获取`By Authority And Tenants Ids`。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `tenantsIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findByAuthorityAndTenantsIds(Authority authority, List<TenantId> tenantsIds, PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `tenantProfilesIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findByAuthorityAndTenantProfilesIds(Authority authority, List<TenantProfileId> tenantProfilesIds, PageLink pageLink);

}

/*
 * 本类总结：
 * 1. 核心职责：`UserDao` 在 ThingsBoard DAO 模块 中承担租户、客户或用户持久化服务类型职责，核心目的是管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 核心流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
 * 3. 关键依赖：主要依赖或协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
