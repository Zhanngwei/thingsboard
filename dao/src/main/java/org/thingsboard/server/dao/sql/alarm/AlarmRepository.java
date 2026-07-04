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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AlarmInfoEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`AlarmRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public interface AlarmRepository extends JpaRepository<AlarmEntity, UUID> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    /**
     * 方法说明：
     * 1. 职责：执行 `findLatestByOriginatorAndType` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType AND a.cleared = FALSE ORDER BY a.createdTime DESC")
    /**
     * 方法说明：
     * 1. 职责：执行 `findLatestActiveByOriginatorAndType` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    List<AlarmEntity> findLatestActiveByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                          @Param("alarmType") String alarmType,
                                                          Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmInfoEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarms` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") UUID tenantId,
                                     @Param("affectedEntityId") UUID affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime,
                                     @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                     @Param("clearFilter") boolean clearFilter,
                                     @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                     @Param("ackFilter") boolean ackFilter,
                                     @Param("assigneeId") String assigneeId,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmInfoEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarmsV2` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<AlarmInfoEntity> findAlarmsV2(@Param("tenantId") UUID tenantId,
                                       @Param("affectedEntityId") UUID affectedEntityId,
                                       @Param("affectedEntityType") String affectedEntityType,
                                       @Param("startTime") Long startTime,
                                       @Param("endTime") Long endTime,
                                       @Param("alarmTypes") List<String> alarmTypes,
                                       @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                       @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                       @Param("clearFilter") boolean clearFilter,
                                       @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                       @Param("ackFilter") boolean ackFilter,
                                       @Param("assigneeId") String assigneeId,
                                       @Param("searchText") String searchText,
                                       Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAllAlarms` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<AlarmInfoEntity> findAllAlarms(@Param("tenantId") UUID tenantId,
                                        @Param("startTime") Long startTime,
                                        @Param("endTime") Long endTime,
                                        @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                        @Param("clearFilter") boolean clearFilter,
                                        @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                        @Param("ackFilter") boolean ackFilter,
                                        @Param("assigneeId") String assigneeId,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAllAlarmsV2` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<AlarmInfoEntity> findAllAlarmsV2(@Param("tenantId") UUID tenantId,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTime,
                                          @Param("alarmTypes") List<String> alarmTypes,
                                          @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                          @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                          @Param("clearFilter") boolean clearFilter,
                                          @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                          @Param("ackFilter") boolean ackFilter,
                                          @Param("assigneeId") String assigneeId,
                                          @Param("searchText") String searchText,
                                          Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    /**
     * 方法说明：
     * 1. 职责：执行 `findCustomerAlarms` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<AlarmInfoEntity> findCustomerAlarms(@Param("tenantId") UUID tenantId,
                                             @Param("customerId") UUID customerId,
                                             @Param("startTime") Long startTime,
                                             @Param("endTime") Long endTime,
                                             @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                             @Param("clearFilter") boolean clearFilter,
                                             @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                             @Param("ackFilter") boolean ackFilter,
                                             @Param("assigneeId") String assigneeId,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    /**
     * 方法说明：
     * 1. 职责：执行 `findCustomerAlarmsV2` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<AlarmInfoEntity> findCustomerAlarmsV2(@Param("tenantId") UUID tenantId,
                                               @Param("customerId") UUID customerId,
                                               @Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime,
                                               @Param("alarmTypes") List<String> alarmTypes,
                                               @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                               @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                               @Param("clearFilter") boolean clearFilter,
                                               @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                               @Param("ackFilter") boolean ackFilter,
                                               @Param("assigneeId") String assigneeId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query(value = "SELECT a.severity FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId))")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarmSeverities` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Set<AlarmSeverity> findAlarmSeverities(@Param("tenantId") UUID tenantId,
                                           @Param("affectedEntityId") UUID affectedEntityId,
                                           @Param("affectedEntityType") String affectedEntityType,
                                           @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                           @Param("clearFilter") boolean clearFilter,
                                           @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                           @Param("ackFilter") boolean ackFilter,
                                           @Param("assigneeId") String assigneeId);

    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.createdTime < :time AND a.endTs < :time")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarmsIdsByEndTsBeforeAndTenantId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<UUID> findAlarmsIdsByEndTsBeforeAndTenantId(@Param("time") Long time, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(value = "SELECT a FROM AlarmInfoEntity a WHERE a.tenantId = :tenantId AND a.id = :alarmId")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarmInfoById` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    AlarmInfoEntity findAlarmInfoById(@Param("tenantId") UUID tenantId, @Param("alarmId") UUID alarmId);

    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.assigneeId = :assigneeId")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarmIdsByAssigneeId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<UUID> findAlarmIdsByAssigneeId(@Param("tenantId") UUID tenantId, @Param("assigneeId") UUID assigneeId, Pageable pageable);

    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.originatorId = :originatorId")
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarmIdsByOriginatorId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<UUID> findAlarmIdsByOriginatorId(@Param("tenantId") UUID tenantId, @Param("originatorId") UUID originatorId, Pageable pageable);

    @Query(value = "SELECT create_or_update_active_alarm(:t_id, :c_id, :a_id, :a_created_ts, :a_o_id, :a_o_type, :a_type, :a_severity, " +
            ":a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, " +
            ":a_propagate_to_tenant, :a_propagation_types, :a_creation_enabled)", nativeQuery = true)
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
    String createOrUpdateActiveAlarm(@Param("t_id") UUID tenantId, @Param("c_id") UUID customerId,
                                     @Param("a_id") UUID alarmId, @Param("a_created_ts") long createdTime,
                                     @Param("a_o_id") UUID originatorId, @Param("a_o_type") int originatorType,
                                     @Param("a_type") String type, @Param("a_severity") String severity,
                                     @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                                     @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                                     @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes,
                                     @Param("a_creation_enabled") boolean alarmCreationEnabled);

    @Query(value = "SELECT update_alarm(:t_id, :a_id, :a_severity, :a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, " +
            ":a_propagate_to_tenant, :a_propagation_types)", nativeQuery = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `updateAlarm` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    String updateAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_severity") String severity,
                       @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                       @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                       @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes);

    @Query(value = "SELECT acknowledge_alarm(:t_id, :a_id, :a_ts)", nativeQuery = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `acknowledgeAlarm` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    String acknowledgeAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts);

    @Query(value = "SELECT clear_alarm(:t_id, :a_id, :a_ts, :a_details)", nativeQuery = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `clearAlarm` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    String clearAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts, @Param("a_details") String details);

    @Query(value = "SELECT assign_alarm(:t_id, :a_id, :u_id, :a_ts)", nativeQuery = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `assignAlarm` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    String assignAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("u_id") UUID userId, @Param("a_ts") long assignTime);

    @Query(value = "SELECT unassign_alarm(:t_id, :a_id, :a_ts)", nativeQuery = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignAlarm` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    String unassignAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long unassignTime);

    @Query(value = "SELECT at.type FROM alarm_types AS at WHERE at.tenant_id = :tenantId AND at.type ILIKE CONCAT('%', :searchText, '%')", nativeQuery = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `findTenantAlarmTypes` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Page<String> findTenantAlarmTypes(@Param("tenantId") UUID tenantId, @Param("searchText") String searchText, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM alarm_types AS at WHERE NOT EXISTS (SELECT 1 FROM alarm AS a WHERE a.tenant_id = at.tenant_id AND a.type = at.type) AND at.tenant_id = :tenantId AND at.type IN (:types)", nativeQuery = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTypeIfNoAlarmsExist` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    int deleteTypeIfNoAlarmsExist(@Param("tenantId") UUID tenantId, @Param("types") Set<String> types);

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
