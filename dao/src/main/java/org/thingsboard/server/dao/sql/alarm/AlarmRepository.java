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

    /**
     * 功能：获取类型。
     * 参数：
     * - `originatorId`：`originatorId`ID。
     * - `alarmType`：类型。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    /**
     * 功能：获取类型。
     * 参数：
     * - `originatorId`：`originatorId`ID。
     * - `alarmType`：类型。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType AND a.cleared = FALSE ORDER BY a.createdTime DESC")
    List<AlarmEntity> findLatestActiveByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                          @Param("alarmType") String alarmType,
                                                          Pageable pageable);

    /**
     * 功能：获取`Alarms`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `affectedEntityId`：实体IDID。
     * - `affectedEntityType`：实体对象。
     * - `startTime`：开始时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
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

    /**
     * 功能：获取`Alarms V2`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `affectedEntityId`：实体IDID。
     * - `affectedEntityType`：实体对象。
     * - `startTime`：开始时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
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

    /**
     * 功能：获取`All Alarms`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * - `clearFilterEnabled`：`clearFilterEnabled` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
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

    /**
     * 功能：获取`All Alarms V2`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * - `alarmTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
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

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
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

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
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

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `affectedEntityId`：实体IDID。
     * - `affectedEntityType`：实体对象。
     * - `clearFilterEnabled`：`clearFilterEnabled` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query(value = "SELECT a.severity FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId))")
    Set<AlarmSeverity> findAlarmSeverities(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取结束时间戳。
     * 参数：
     * - `time`：`time` 参数。
     * - `tenantId`：租户IDID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
                                           @Param("affectedEntityId") UUID affectedEntityId,
                                           @Param("affectedEntityType") String affectedEntityType,
                                           @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                           @Param("clearFilter") boolean clearFilter,
                                           @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                           @Param("ackFilter") boolean ackFilter,
                                           @Param("assigneeId") String assigneeId);

    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.createdTime < :time AND a.endTs < :time")
    Page<UUID> findAlarmsIdsByEndTsBeforeAndTenantId(@Param("time") Long time, @Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    @Query(value = "SELECT a FROM AlarmInfoEntity a WHERE a.tenantId = :tenantId AND a.id = :alarmId")
    AlarmInfoEntity findAlarmInfoById(@Param("tenantId") UUID tenantId, @Param("alarmId") UUID alarmId);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assigneeId`：`assigneeId`ID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.assigneeId = :assigneeId")
    Page<UUID> findAlarmIdsByAssigneeId(@Param("tenantId") UUID tenantId, @Param("assigneeId") UUID assigneeId, Pageable pageable);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorId`：`originatorId`ID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.originatorId = :originatorId")
    Page<UUID> findAlarmIdsByOriginatorId(@Param("tenantId") UUID tenantId, @Param("originatorId") UUID originatorId, Pageable pageable);

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `alarmId`：告警IDID。
     * - `createdTime`：`createdTime` 参数。
     * - 其余参数：补充处理条件。
     * 返回：文本结果。
     */
    @Query(value = "SELECT create_or_update_active_alarm(:t_id, :c_id, :a_id, :a_created_ts, :a_o_id, :a_o_type, :a_type, :a_severity, " +
            ":a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, " +
            ":a_propagate_to_tenant, :a_propagation_types, :a_creation_enabled)", nativeQuery = true)
    String createOrUpdateActiveAlarm(@Param("t_id") UUID tenantId, @Param("c_id") UUID customerId,
    /**
     * 功能：更新告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `severity`：`severity` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：文本结果。
     */
                                     @Param("a_id") UUID alarmId, @Param("a_created_ts") long createdTime,
                                     @Param("a_o_id") UUID originatorId, @Param("a_o_type") int originatorType,
                                     @Param("a_type") String type, @Param("a_severity") String severity,
                                     @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                                     @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                                     @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes,
                                     @Param("a_creation_enabled") boolean alarmCreationEnabled);

    @Query(value = "SELECT update_alarm(:t_id, :a_id, :a_severity, :a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, " +
            ":a_propagate_to_tenant, :a_propagation_types)", nativeQuery = true)
    String updateAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_severity") String severity,
    /**
     * 功能：执行 `acknowledgeAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `ts`：时间戳。
     * 返回：文本结果。
     */
                       @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                       @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                       @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes);

    @Query(value = "SELECT acknowledge_alarm(:t_id, :a_id, :a_ts)", nativeQuery = true)
    String acknowledgeAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts);

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `ts`：时间戳。
     * - `details`：`details` 参数。
     * 返回：文本结果。
     */
    @Query(value = "SELECT clear_alarm(:t_id, :a_id, :a_ts, :a_details)", nativeQuery = true)
    String clearAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts, @Param("a_details") String details);

    /**
     * 功能：执行 `assignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `userId`：用户ID。
     * - `assignTime`：`assignTime` 参数。
     * 返回：文本结果。
     */
    @Query(value = "SELECT assign_alarm(:t_id, :a_id, :u_id, :a_ts)", nativeQuery = true)
    String assignAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("u_id") UUID userId, @Param("a_ts") long assignTime);

    /**
     * 功能：执行 `unassignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `unassignTime`：`unassignTime` 参数。
     * 返回：文本结果。
     */
    @Query(value = "SELECT unassign_alarm(:t_id, :a_id, :a_ts)", nativeQuery = true)
    String unassignAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long unassignTime);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(value = "SELECT at.type FROM alarm_types AS at WHERE at.tenant_id = :tenantId AND at.type ILIKE CONCAT('%', :searchText, '%')", nativeQuery = true)
    Page<String> findTenantAlarmTypes(@Param("tenantId") UUID tenantId, @Param("searchText") String searchText, Pageable pageable);

    /**
     * 功能：删除或清理类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `types`：类型。
     * 返回：数值结果。
     */
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM alarm_types AS at WHERE NOT EXISTS (SELECT 1 FROM alarm AS a WHERE a.tenant_id = at.tenant_id AND a.type = at.type) AND at.tenant_id = :tenantId AND at.type IN (:types)", nativeQuery = true)
    int deleteTypeIfNoAlarmsExist(@Param("tenantId") UUID tenantId, @Param("types") Set<String> types);

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
