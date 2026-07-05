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
package org.thingsboard.server.dao.sql.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * @author Valerii Sosliuk
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaUserDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Component
@SqlDao
public class JpaUserDao extends JpaAbstractDao<UserEntity, User> implements UserDao {

    /**
     * 用户，用于读取或保存对应领域对象。
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<UserEntity, UUID> getRepository() {
        return userRepository;
    }

    /**
     * 功能：获取邮箱。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    @Override
    public User findByEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByEmail(email));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    @Override
    public User findByTenantIdAndEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByTenantIdAndEmail(tenantId.getId(), email));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findByTenantId(
                                tenantId,
                                pageLink.getTextSearch(),
                                DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                NULL_UUID,
                                pageLink.getTextSearch(),
                                Authority.TENANT_ADMIN,
                                DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                customerId,
                                pageLink.getTextSearch(),
                                Authority.CUSTOMER_USER,
                                DaoUtil.toPageable(pageLink)));

    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerIds`：数据列表。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findUsersByCustomerIds(UUID tenantId, List<CustomerId> customerIds, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findTenantAndCustomerUsers(
                                tenantId,
                                DaoUtil.toUUIDs(customerIds),
                                pageLink.getTextSearch(),
                                DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取`All`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findAll(PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取`All By Authority`。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findAllByAuthority(Authority authority, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findAllByAuthority(authority, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取`By Authority And Tenants Ids`。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `tenantsIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findByAuthorityAndTenantsIds(Authority authority, List<TenantId> tenantsIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByAuthorityAndTenantIdIn(authority, DaoUtil.toUUIDs(tenantsIds), DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `tenantProfilesIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findByAuthorityAndTenantProfilesIds(Authority authority, List<TenantProfileId> tenantProfilesIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByAuthorityAndTenantProfilesIds(authority, DaoUtil.toUUIDs(tenantProfilesIds),
                DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public Long countByTenantId(TenantId tenantId) {
        return userRepository.countByTenantId(tenantId.getId());
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaUserDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
