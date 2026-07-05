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
package org.thingsboard.server.dao.sql.notification;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.dao.model.sql.NotificationRequestEntity;
import org.thingsboard.server.dao.model.sql.NotificationRequestInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`NotificationRequestRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequestEntity, UUID> {

    String REQUEST_INFO_QUERY = "SELECT new org.thingsboard.server.dao.model.sql.NotificationRequestInfoEntity(r, t.name, t.configuration) " +
            "FROM NotificationRequestEntity r LEFT JOIN NotificationTemplateEntity t ON r.templateId = t.id";

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<NotificationRequestEntity> findByTenantIdAndOriginatorEntityType(UUID tenantId, EntityType originatorType, Pageable pageable);

    /**
     * 功能：获取搜索文本。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(REQUEST_INFO_QUERY + " WHERE r.tenantId = :tenantId AND r.originatorEntityType = :originatorType " +
            "AND (:searchText is NULL OR (t.name IS NOT NULL AND ilike(t.name, concat('%', :searchText, '%')) = true))")
    Page<NotificationRequestInfoEntity> findInfosByTenantIdAndOriginatorEntityTypeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                                @Param("originatorType") EntityType originatorType,
                                                                                                @Param("searchText") String searchText,
                                                                                                Pageable pageable);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query(REQUEST_INFO_QUERY + " WHERE r.id = :id")
    NotificationRequestInfoEntity findInfoById(@Param("id") UUID id);

    /**
     * 功能：获取状态。
     * 参数：
     * - `status`：`status` 参数。
     * - `ruleId`：`ruleId`ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT r.id FROM NotificationRequestEntity r WHERE r.status = :status AND r.ruleId = :ruleId")
    List<UUID> findAllIdsByStatusAndRuleId(@Param("status") NotificationRequestStatus status,
    /**
     * 功能：获取实体ID。
     * 参数：
     * - `ruleId`：`ruleId`ID。
     * - `originatorEntityId`：实体IDID。
     * - `originatorEntityType`：实体对象。
     * 返回：匹配的数据集合。
     */
                                           @Param("ruleId") UUID ruleId);
    List<NotificationRequestEntity> findAllByRuleIdAndOriginatorEntityIdAndOriginatorEntityType(UUID ruleId, UUID originatorEntityId, EntityType originatorEntityType);

    /**
     * 功能：获取状态。
     * 参数：
     * - `status`：`status` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<NotificationRequestEntity> findAllByStatus(NotificationRequestStatus status, Pageable pageable);

    /**
     * 功能：更新状态。
     * 参数：
     * - `id`：`id`ID。
     * - `status`：`status` 参数。
     * - `stats`：`stats` 参数。
     * 返回：无。
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationRequestEntity r SET r.status = :status, r.stats = :stats WHERE r.id = :id")
    void updateStatusAndStatsById(@Param("id") UUID id,
    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `targetIdStr`：`targetIdStr` 参数。
     * 返回：判断结果。
     */
                                  @Param("status") NotificationRequestStatus status,
                                  @Param("stats") JsonNode stats);
    boolean existsByTenantIdAndStatusAndTargetsContaining(UUID tenantId, NotificationRequestStatus status, String targetIdStr);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `templateId`：`templateId`ID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndStatusAndTemplateId(UUID tenantId, NotificationRequestStatus status, UUID templateId);

    /**
     * 功能：删除或清理创建时间。
     * 参数：
     * - `ts`：时间戳。
     * 返回：数值结果。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationRequestEntity r WHERE r.createdTime < :ts")
    int deleteAllByCreatedTimeBefore(@Param("ts") long ts);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationRequestEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationRequestRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
