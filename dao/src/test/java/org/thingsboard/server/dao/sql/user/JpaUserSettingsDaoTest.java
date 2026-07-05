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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserSettingsDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * 中文说明：
 * 1. 类目的：`JpaUserSettingsDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaUserSettingsDaoTest extends AbstractJpaDaoTest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private UUID tenantId;
    private User user;

    /**
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private UserSettingsDao userSettingsDao;

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
        user = saveUser(tenantId, Uuids.timeBased());
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        userDao.removeById(user.getTenantId(), user.getUuidId());
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindSettingsByUserId() {
        UserSettings userSettings = createUserSettings(user.getId());

        UserSettings retrievedUserSettings = userSettingsDao.findById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettingsType.GENERAL.name()));
        assertEquals(retrievedUserSettings.getSettings(), userSettings.getSettings());

        userSettingsDao.removeById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettingsType.GENERAL.name()));

        UserSettings retrievedUserSettings2 = userSettingsDao.findById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettingsType.GENERAL.name()));
        assertNull(retrievedUserSettings2);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    private UserSettings createUserSettings(UserId userId) {
        UserSettings userSettings = new UserSettings();
        userSettings.setType(UserSettingsType.GENERAL);
        userSettings.setSettings(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        userSettings.setUserId(userId);
        return userSettingsDao.save(SYSTEM_TENANT_ID, userSettings);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    private User saveUser(UUID tenantId, UUID customerId) {
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
        return  userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaUserSettingsDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
