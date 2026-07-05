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
package org.thingsboard.server.dao.sql.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.TbResourceEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`TbResourceRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public interface TbResourceRepository extends JpaRepository<TbResourceEntity, UUID>, ExportableEntityRepository<TbResourceEntity> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：处理结果。
     */
    TbResourceEntity findByTenantIdAndResourceTypeAndResourceKey(UUID tenantId, String resourceType, String resourceKey);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<TbResourceEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    /**
     * 功能：获取`Resources Page`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `sysAdminId`：`sysAdminId`ID。
     * - `resourceType`：类型。
     * - `searchText`：`searchText` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND (:searchText IS NULL OR ilike(tr.searchText, CONCAT('%', :searchText, '%')) = true) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    Page<TbResourceEntity> findResourcesPage(
            @Param("tenantId") UUID tenantId,
            @Param("systemAdminId") UUID sysAdminId,
            @Param("resourceType") String resourceType,
            @Param("searchText") String searchText,
            Pageable pageable);

    /**
     * 功能：获取`Resources`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `sysAdminId`：`sysAdminId`ID。
     * - `resourceType`：类型。
     * - `searchText`：`searchText` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND (:searchText IS NULL OR ilike(tr.searchText, CONCAT('%', :searchText, '%')) = true) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    List<TbResourceEntity> findResources(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取`Resources By Ids`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `sysAdminId`：`sysAdminId`ID。
     * - `resourceType`：类型。
     * - `objectIds`：`objectIds` 参数。
     * 返回：匹配的数据集合。
     */
                                         @Param("systemAdminId") UUID sysAdminId,
                                         @Param("resourceType") String resourceType,
                                         @Param("searchText") String searchText);

    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND tr.resourceKey in (:resourceIds) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    List<TbResourceEntity> findResourcesByIds(@Param("tenantId") UUID tenantId,
    /**
     * 功能：执行 `sumDataSizeByTenantId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
                                              @Param("systemAdminId") UUID sysAdminId,
                                              @Param("resourceType") String resourceType,
                                              @Param("resourceIds") String[] objectIds);

    @Query(value = "SELECT COALESCE(SUM(LENGTH(r.data)), 0) FROM resource r WHERE r.tenant_id = :tenantId", nativeQuery = true)
    Long sumDataSizeByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取数据。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query("SELECT r.data FROM TbResourceEntity r WHERE r.id = :id")
    byte[] getDataById(@Param("id") UUID id);

    /**
     * 功能：获取`Preview By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query(value = "SELECT COALESCE(preview, data) FROM resource WHERE id = :id", nativeQuery = true)
    byte[] getPreviewById(@Param("id") UUID id);

    /**
     * 功能：获取数据。
     * 参数：
     * - `id`：`id`ID。
     * 返回：数值结果。
     */
    @Query(value = "SELECT length(r.data) FROM resource r WHERE r.id = :id", nativeQuery = true)
    long getDataSizeById(@Param("id") UUID id);

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Query("SELECT externalId FROM TbResourceInfoEntity WHERE id = :id")
    UUID getExternalIdByInternal(@Param("id") UUID internalId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT r.id FROM TbResourceInfoEntity r WHERE r.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}

/*
 * 本类总结：
 * 1. 核心职责：`TbResourceRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
