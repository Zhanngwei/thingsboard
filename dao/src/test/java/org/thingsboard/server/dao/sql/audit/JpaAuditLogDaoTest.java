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
 * 1. `JpaAuditLogDaoTest` 是 ThingsBoard DAO 中验证 `JpaAuditLogDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
