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
package org.thingsboard.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`JpaAlarmDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaAlarmDaoTest extends AbstractJpaDaoTest {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `alarmDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private AlarmDao alarmDao;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantProfileDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    protected TenantProfileDao tenantProfileDao;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    protected TenantDao tenantDao;

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindLatestByOriginatorAndType` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindLatestByOriginatorAndType() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID originator2Id = UUID.fromString("d4b68f42-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        UUID alarm2Id = UUID.fromString("d4b68f44-3e96-11e7-a884-898080180d6b");
        UUID alarm3Id = UUID.fromString("d4b68f45-3e96-11e7-a884-898080180d6b");
        // The find method does not filter by tenant. It is just using the tenantId for rate limits if any.
        var alarmsBeforeSave = alarmDao.find(tenantId).stream().filter(a -> a.getTenantId().equals(tenantId)).collect(Collectors.toList());
        int alarmCountBeforeSave = alarmsBeforeSave.size();
        saveAlarm(alarm1Id, tenantId.getId(), originator1Id, "TEST_ALARM");
        //The timestamp of the startTime should be different in order for test to always work
        Thread.sleep(1);
        saveAlarm(alarm2Id, tenantId.getId(), originator1Id, "TEST_ALARM");
        saveAlarm(alarm3Id, tenantId.getId(), originator2Id, "TEST_ALARM");
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        var alarmsAfterSave = alarmDao.find(tenantId).stream().filter(a -> a.getTenantId().equals(tenantId)).collect(Collectors.toList());
        int alarmCountAfterSave = alarmsAfterSave.size();
        int diff = alarmCountAfterSave - alarmCountBeforeSave;
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (diff != 3) {
            System.out.println("test");
        }
        assertEquals(3, diff);
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        ListenableFuture<Alarm> future = alarmDao
                .findLatestByOriginatorAndTypeAsync(tenantId, new DeviceId(originator1Id), "TEST_ALARM");
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        Alarm alarm = future.get(30, TimeUnit.SECONDS);
        assertNotNull(alarm);
        assertEquals(alarm2Id, alarm.getId().getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `createOrUpdateActiveAlarm` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void createOrUpdateActiveAlarm() {
        Tenant tenant = createTenant();
        TenantId tenantId = tenant.getId();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        AlarmCreateOrUpdateActiveRequest request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.MAJOR)
                .build();
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        AlarmApiCallResult result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        UUID newAlarmId = result.getAlarm().getUuidId();
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(tenantId, newAlarmId);
        assertEquals(afterSave, result.getAlarm());

        request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.CRITICAL)
                .build();
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertFalse(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(newAlarmId, result.getAlarm().getUuidId());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        afterSave = alarmDao.findAlarmInfoById(tenantId, newAlarmId);
        assertEquals(afterSave, result.getAlarm());

        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        alarmDao.clearAlarm(tenantId, result.getAlarm().getId(), System.currentTimeMillis(), result.getAlarm().getDetails());

        request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.CRITICAL)
                .build();
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertNotEquals(newAlarmId, result.getAlarm().getUuidId());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        afterSave = alarmDao.findAlarmInfoById(tenantId, result.getAlarm().getUuidId());
        assertEquals(afterSave, result.getAlarm());

        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        alarmDao.clearAlarm(tenantId, result.getAlarm().getId(), System.currentTimeMillis(), result.getAlarm().getDetails());

        request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE2")
                .severity(AlarmSeverity.CRITICAL)
                .build();
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertNotEquals(newAlarmId, result.getAlarm().getUuidId());

        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tenantDao.removeById(TenantId.SYS_TENANT_ID, tenant.getUuidId());
        tenantProfileDao.removeById(TenantId.SYS_TENANT_ID, tenant.getTenantProfileId().getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCantCreateAlarmIfCreateIsDisabled` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testCantCreateAlarmIfCreateIsDisabled() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        AlarmCreateOrUpdateActiveRequest request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.MAJOR)
                .build();
        AlarmApiCallResult result = alarmDao.createOrUpdateActiveAlarm(request, false);
        assertFalse(result.isSuccessful());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAckAlarmProcedure` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testAckAlarmProcedure() {
        UUID tenantId = UUID.randomUUID();
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        Alarm alarm = saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        long ackTs = System.currentTimeMillis();
        AlarmApiCallResult result = alarmDao.acknowledgeAlarm(alarm.getTenantId(), alarm.getId(), ackTs);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(ackTs, result.getAlarm().getAckTs());
        assertTrue(result.getAlarm().isAcknowledged());
        result = alarmDao.acknowledgeAlarm(alarm.getTenantId(), alarm.getId(), ackTs + 1);
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isModified());
        assertEquals(ackTs, result.getAlarm().getAckTs());
        assertTrue(result.getAlarm().isAcknowledged());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClearAlarmProcedure` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testClearAlarmProcedure() {
        UUID tenantId = UUID.randomUUID();
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        Alarm alarm = saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        long clearTs = System.currentTimeMillis();
        var details = JacksonUtil.newObjectNode().put("test", 123);
        AlarmApiCallResult result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs, details);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCleared());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
        assertEquals(details, result.getAlarm().getDetails());
        result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs + 1, JacksonUtil.newObjectNode());
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isCleared());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClearAlarmWithoutDetailsProcedure` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testClearAlarmWithoutDetailsProcedure() {
        UUID tenantId = UUID.randomUUID();
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        Alarm alarm = saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        long clearTs = System.currentTimeMillis();
        AlarmApiCallResult result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs, null);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCleared());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
        assertEquals(alarm.getDetails(), result.getAlarm().getDetails());
        result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs + 1, JacksonUtil.newObjectNode());
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isCleared());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAlarmProcedure` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testAssignAlarmProcedure() {
        UUID tenantId = UUID.randomUUID();
        ;
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarmId = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        UserId userId1 = new UserId(UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d7b"));
        UserId userId2 = new UserId(UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d8b"));
        Alarm alarm = saveAlarm(alarmId, tenantId, originator1Id, "TEST_ALARM");
        long assignTs = System.currentTimeMillis();
        AlarmApiCallResult result = alarmDao.assignAlarm(alarm.getTenantId(), alarm.getId(), userId1, assignTs);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(assignTs, result.getAlarm().getAssignTs());
        assertNotNull(result.getAlarm().getAssigneeId());
        assertEquals(userId1, result.getAlarm().getAssigneeId());
        result = alarmDao.assignAlarm(alarm.getTenantId(), alarm.getId(), userId1, assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isModified());
        assertEquals(assignTs, result.getAlarm().getAssignTs());
        assertNotNull(result.getAlarm().getAssigneeId());
        assertEquals(userId1, result.getAlarm().getAssigneeId());
        result = alarmDao.assignAlarm(alarm.getTenantId(), alarm.getId(), userId2, assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(assignTs + 1, result.getAlarm().getAssignTs());
        assertNotNull(result.getAlarm().getAssigneeId());
        assertEquals(userId2, result.getAlarm().getAssigneeId());

        result = alarmDao.unassignAlarm(alarm.getTenantId(), alarm.getId(), assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertNull(result.getAlarm().getAssigneeId());

        result = alarmDao.unassignAlarm(alarm.getTenantId(), alarm.getId(), assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertFalse(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertNull(result.getAlarm().getAssigneeId());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveAlarm` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private Alarm saveAlarm(UUID id, UUID tenantId, UUID deviceId, String type) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new DeviceId(deviceId));
        alarm.setType(type);
        alarm.setPropagate(true);
        alarm.setStartTs(System.currentTimeMillis());
        alarm.setEndTs(System.currentTimeMillis());
        alarm.setAcknowledged(false);
        alarm.setCleared(false);
        alarm.setDetails(JacksonUtil.newObjectNode().put("a", UUID.randomUUID().toString()).set("b", JacksonUtil.newObjectNode().put("a", "[}/.`1321421!@@$$(%&&$")));
        return alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTenant` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private Tenant createTenant() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("My tenant profile " + UUID.randomUUID());
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(profileData);
        var savedTenantProfile = tenantProfileDao.save(TenantId.SYS_TENANT_ID, tenantProfile);
        assertNotNull(savedTenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant " + UUID.randomUUID());
        tenant.setTenantProfileId(savedTenantProfile.getId());
        Tenant savedTenant = tenantDao.save(TenantId.SYS_TENANT_ID, tenant);

        assertNotNull(savedTenant);

        return savedTenant;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaAlarmDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
