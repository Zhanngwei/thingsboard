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
package org.thingsboard.server.dao.sql.audit;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.audit.AuditLogDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 中文说明：
 * 1. 类目的：`JpaAuditLogDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaAuditLogDaoTest extends AbstractJpaDaoTest {
    List<AuditLog> auditLogList = new ArrayList<>();
    /**
     * 租户ID，用于定位对应业务对象。
     */
    UUID tenantId;
    CustomerId customerId1;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    CustomerId customerId2;
    UserId userId1;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    UserId userId2;
    EntityId entityId1;
    /**
     * 实体对象，用于描述当前业务场景。
     */
    EntityId entityId2;
    AuditLog neededFoundedAuditLog;
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private AuditLogDao auditLogDao;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        setUpIds();
        for (int i = 0; i < 60; i++) {
            ActionType actionType = i % 2 == 0 ? ActionType.ADDED : ActionType.DELETED;
            CustomerId customerId = i % 4 == 0 ? customerId1 : customerId2;
            UserId userId = i % 6 == 0 ? userId1 : userId2;
            EntityId entityId = i % 10 == 0 ? entityId1 : entityId2;
            auditLogList.add(createAuditLog(i, actionType, customerId, userId, entityId));
        }
        assertEquals(auditLogList.size(), auditLogDao.find(TenantId.fromUUID(tenantId)).size());
        neededFoundedAuditLog = auditLogList.get(0);
        assertNotNull(neededFoundedAuditLog);
    }

    /**
     * 功能：更新`Up Ids`。
     * 参数：无。
     * 返回：无。
     */
    private void setUpIds() {
        tenantId = Uuids.timeBased();
        customerId1 = new CustomerId(Uuids.timeBased());
        customerId2 = new CustomerId(Uuids.timeBased());
        userId1 = new UserId(Uuids.timeBased());
        userId2 = new UserId(Uuids.timeBased());
        entityId1 = new DeviceId(Uuids.timeBased());
        entityId2 = new DeviceId(Uuids.timeBased());
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        for (AuditLog auditLog : auditLogList) {
            auditLogDao.removeById(TenantId.fromUUID(tenantId), auditLog.getUuidId());
        }
        auditLogList.clear();
    }

    /**
     * 功能：验证`Find By Id`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindById() {
        AuditLog foundedAuditLogById = auditLogDao.findById(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId());
        checkFoundedAuditLog(foundedAuditLogById);
    }
    
    /**
     * 功能：验证`Find By Id Async`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByIdAsync() throws ExecutionException, InterruptedException, TimeoutException {
        AuditLog foundedAuditLogById = auditLogDao
                .findByIdAsync(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId()).get(30, TimeUnit.SECONDS);
        checkFoundedAuditLog(foundedAuditLogById);
    }

    /**
     * 功能：校验`Founded Audit Log`。
     * 参数：
     * - `foundedAuditLogById`：`foundedAuditLogById`ID。
     * 返回：无。
     */
    private void checkFoundedAuditLog(AuditLog foundedAuditLogById) {
        assertNotNull(foundedAuditLogById);
        assertEquals(neededFoundedAuditLog, foundedAuditLogById);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAuditLogsByTenantId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantId(tenantId,
                List.of(ActionType.ADDED),
                new TimePageLink(40)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 30);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAuditLogsByTenantIdAndCustomerId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId,
                customerId1,
                List.of(ActionType.ADDED),
                new TimePageLink(20)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 15);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAuditLogsByTenantIdAndUserId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId,
                userId1,
                List.of(ActionType.ADDED),
                new TimePageLink(20)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 10);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAuditLogsByTenantIdAndEntityId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId,
                entityId1,
                List.of(ActionType.ADDED),
                new TimePageLink(10)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 6);
    }

    /**
     * 功能：校验`Founded Audit Logs List`。
     * 参数：
     * - `foundedAuditLogs`：数据列表。
     * - `neededSizeForFoundedList`：`neededSizeForFoundedList` 参数。
     * 返回：无。
     */
    private void checkFoundedAuditLogsList(List<AuditLog> foundedAuditLogs, int neededSizeForFoundedList) {
        assertNotNull(foundedAuditLogs);
        assertEquals(neededSizeForFoundedList, foundedAuditLogs.size());
    }

    /**
     * 功能：保存或创建`Audit Log`。
     * 参数：
     * - `number`：`number` 参数。
     * - `actionType`：类型。
     * - `customerId`：客户IDID。
     * - `userId`：用户ID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private AuditLog createAuditLog(int number, ActionType actionType, CustomerId customerId, UserId userId, EntityId entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(TenantId.fromUUID(tenantId));
        auditLog.setCustomerId(customerId);
        auditLog.setUserId(userId);
        auditLog.setEntityId(entityId);
        auditLog.setUserName("AUDIT_LOG_" + number);
        auditLog.setActionType(actionType);
        return auditLogDao.save(TenantId.fromUUID(tenantId), auditLog);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaAuditLogDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
