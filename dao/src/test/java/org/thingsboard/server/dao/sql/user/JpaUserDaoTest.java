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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/18/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaUserDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaUserDaoTest extends AbstractJpaDaoTest {

    // it comes from the DefaultSystemDataLoaderService super class
    /**
     * 用户常量，用于统一引用固定值。
     */
    final int COUNT_CREATED_USER = 1;
    final int COUNT_SYSADMIN_USER = 90;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    UUID tenantId;
    UUID customerId;
    /**
     * 用户，用于读取或保存对应领域对象。
     */
    @Autowired
    private UserDao userDao;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        tenantId = Uuids.timeBased();
        customerId = Uuids.timeBased();
        create30TenantAdminsAnd60CustomerUsers(tenantId, customerId);
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        delete30TenantAdminsAnd60CustomerUsers(tenantId, customerId);
    }

    /**
     * 功能：验证`Find All`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAll() {
        List<User> users = userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID);
        assertEquals(users.size(), COUNT_CREATED_USER + COUNT_SYSADMIN_USER);
    }

    /**
     * 功能：验证邮箱相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByEmail() throws JsonProcessingException {
        User user = new User();
        user.setId(new UserId(UUID.randomUUID()));
        user.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        user.setCustomerId(new CustomerId(UUID.randomUUID()));
        user.setEmail("user@thingsboard.org");
        user.setFirstName("Jackson");
        user.setLastName("Roberts");
        String additionalInfo = "{\"key\":\"value-100\"}";
        JsonNode jsonNode = JacksonUtil.toJsonNode(additionalInfo);
        user.setAdditionalInfo(jsonNode);
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
        assertEquals(1 + COUNT_SYSADMIN_USER + COUNT_CREATED_USER, userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());
        User savedUser = userDao.findByEmail(AbstractServiceTest.SYSTEM_TENANT_ID, "user@thingsboard.org");
        assertNotNull(savedUser);
        assertEquals(additionalInfo, savedUser.getAdditionalInfo().toString());
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantAdmins() {
        PageLink pageLink = new PageLink(20);
        PageData<User> tenantAdmins1 = userDao.findTenantAdmins(tenantId, pageLink);
        assertEquals(20, tenantAdmins1.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> tenantAdmins2 = userDao.findTenantAdmins(tenantId,
                pageLink);
        assertEquals(10, tenantAdmins2.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> tenantAdmins3 = userDao.findTenantAdmins(tenantId,
                pageLink);
        assertEquals(0, tenantAdmins3.getData().size());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomerUsers() {
        PageLink pageLink = new PageLink(40);
        PageData<User> customerUsers1 = userDao.findCustomerUsers(tenantId, customerId, pageLink);
        assertEquals(40, customerUsers1.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> customerUsers2 = userDao.findCustomerUsers(tenantId, customerId,
                pageLink);
        assertEquals(20, customerUsers2.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> customerUsers3 = userDao.findCustomerUsers(tenantId, customerId,
                pageLink);
        assertEquals(0, customerUsers3.getData().size());
    }

    /**
     * 功能：执行 `create30TenantAdminsAnd60CustomerUsers` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void create30TenantAdminsAnd60CustomerUsers(UUID tenantId, UUID customerId) {
        // Create 30 tenant admins and 60 customer users
        for (int i = 0; i < 30; i++) {
            saveUser(tenantId, NULL_UUID);
            saveUser(tenantId, customerId);
            saveUser(tenantId, customerId);
        }
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void saveUser(UUID tenantId, UUID customerId) {
        User user = new User();
        UUID id = Uuids.timeBased();
        user.setId(new UserId(id));
        user.setTenantId(TenantId.fromUUID(tenantId));
        user.setCustomerId(new CustomerId(customerId));
        if (customerId == NULL_UUID) {
            user.setAuthority(Authority.TENANT_ADMIN);
        } else {
            user.setAuthority(Authority.CUSTOMER_USER);
        }
        String idString = id.toString();
        String email = idString.substring(0, idString.indexOf('-')) + "@thingsboard.org";
        user.setEmail(email);
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }

    /**
     * 功能：执行 `delete30TenantAdminsAnd60CustomerUsers` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void delete30TenantAdminsAnd60CustomerUsers(UUID tenantId, UUID customerId) {
        List<User> data = userDao.findCustomerUsers(tenantId, customerId, new PageLink(60)).getData();
        data.addAll(userDao.findTenantAdmins(tenantId, new PageLink(30)).getData());
        for (User user : data) {
            userDao.removeById(user.getTenantId(), user.getUuidId());
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaUserDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
