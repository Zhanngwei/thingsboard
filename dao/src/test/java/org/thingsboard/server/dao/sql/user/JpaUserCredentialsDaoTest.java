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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.user.UserCredentialsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaUserCredentialsDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaUserCredentialsDaoTest extends AbstractJpaDaoTest {

    /**
     * 令牌常量，用于统一引用固定值。
     */
    public static final String ACTIVATE_TOKEN = "ACTIVATE_TOKEN_0";
    public static final String RESET_TOKEN = "RESET_TOKEN_0";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final int COUNT_USER_CREDENTIALS = 2;
    List<UserCredentials> userCredentialsList;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    UserCredentials neededUserCredentials;

    /**
     * 用户，用于读取或保存对应领域对象。
     */
    @Autowired
    private UserCredentialsDao userCredentialsDao;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        userCredentialsList = new ArrayList<>();
        for (int i=0; i<COUNT_USER_CREDENTIALS; i++) {
            userCredentialsList.add(createUserCredentials(i));
        }
        neededUserCredentials = userCredentialsList.get(0);
        assertNotNull(neededUserCredentials);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：处理结果。
     */
    UserCredentials createUserCredentials(int number) {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        userCredentials.setUserId(new UserId(UUID.randomUUID()));
        userCredentials.setPassword("password");
        userCredentials.setActivateToken("ACTIVATE_TOKEN_" + number);
        userCredentials.setResetToken("RESET_TOKEN_" + number);
        return userCredentialsDao.save(SYSTEM_TENANT_ID, userCredentials);
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
        for (UserCredentials userCredentials : userCredentialsList) {
            userCredentialsDao.removeById(TenantId.SYS_TENANT_ID, userCredentials.getUuidId());
        }
    }

    /**
     * 功能：验证`Find All`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAll() {
        List<UserCredentials> userCredentials = userCredentialsDao.find(SYSTEM_TENANT_ID);
        assertEquals(COUNT_USER_CREDENTIALS + 1, userCredentials.size());
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByUserId() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByUserId(SYSTEM_TENANT_ID, neededUserCredentials.getUserId().getId());
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials, foundedUserCredentials);
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByActivateToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByActivateToken(SYSTEM_TENANT_ID, ACTIVATE_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByResetToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByResetToken(SYSTEM_TENANT_ID, RESET_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaUserCredentialsDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
