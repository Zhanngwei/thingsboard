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
package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.model.sql.AssetInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`AssetRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public interface AssetRepository extends JpaRepository<AssetEntity, UUID>, ExportableEntityRepository<AssetEntity> {

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.AssetInfoEntity(a, c.title, c.additionalInfo, p.name) " +
            "FROM AssetEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "LEFT JOIN AssetProfileEntity p on p.id = a.assetProfileId " +
            "WHERE a.id = :assetId")
    AssetInfoEntity findAssetInfoById(@Param("assetId") UUID assetId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                     @Param("textSearch") String textSearch,
                                     Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.AssetInfoEntity(a, c.title, c.additionalInfo, p.name) " +
            "FROM AssetEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "LEFT JOIN AssetProfileEntity p on p.id = a.assetProfileId " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true  " +
            "  OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
            "  OR ilike(p.name, CONCAT('%', :textSearch, '%')) = true " +
            "  OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetInfoEntity> findAssetInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                  @Param("customerId") UUID customerId,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND a.assetProfileId = :profileId " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                 @Param("profileId") UUID profileId,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.AssetInfoEntity(a, c.title, c.additionalInfo, p.name) " +
            "FROM AssetEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "LEFT JOIN AssetProfileEntity p on p.id = a.assetProfileId " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true)")
    Page<AssetInfoEntity> findAssetInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                @Param("customerId") UUID customerId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<AssetEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> assetIds);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<AssetEntity> findByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> assetIds);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    AssetEntity findByTenantIdAndName(UUID tenantId, String name);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND a.type = :type " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                            @Param("type") String type,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.AssetInfoEntity(a, c.title, c.additionalInfo, p.name) " +
            "FROM AssetEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "LEFT JOIN AssetProfileEntity p on p.id = a.assetProfileId " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.type = :type " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true  " +
            "  OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
            "  OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetInfoEntity> findAssetInfosByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("type") String type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.AssetInfoEntity(a, c.title, c.additionalInfo, p.name) " +
            "FROM AssetEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "LEFT JOIN AssetProfileEntity p on p.id = a.assetProfileId " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.assetProfileId = :assetProfileId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true  " +
            "  OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
            "  OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetInfoEntity> findAssetInfosByTenantIdAndAssetProfileId(@Param("tenantId") UUID tenantId,
                                                                    @Param("assetProfileId") UUID assetProfileId,
                                                                    @Param("textSearch") String textSearch,
                                                                    Pageable pageable);


    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId AND a.type = :type " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                         @Param("customerId") UUID customerId,
                                                         @Param("type") String type,
                                                         @Param("textSearch") String textSearch,
                                                         Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.AssetInfoEntity(a, c.title, c.additionalInfo, p.name) " +
            "FROM AssetEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "LEFT JOIN AssetProfileEntity p on p.id = a.assetProfileId " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND a.type = :type " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetInfoEntity> findAssetInfosByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                       @Param("customerId") UUID customerId,
                                                                       @Param("type") String type,
                                                                       @Param("textSearch") String textSearch,
                                                                       Pageable pageable);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.AssetInfoEntity(a, c.title, c.additionalInfo, p.name) " +
            "FROM AssetEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "LEFT JOIN AssetProfileEntity p on p.id = a.assetProfileId " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND a.assetProfileId = :assetProfileId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetInfoEntity> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(@Param("tenantId") UUID tenantId,
                                                                                 @Param("customerId") UUID customerId,
                                                                                 @Param("assetProfileId") UUID assetProfileId,
                                                                                 @Param("textSearch") String textSearch,
                                                                                 Pageable pageable);

    /**
     * 功能：统计资产配置数量。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：数值结果。
     */
    Long countByAssetProfileId(UUID assetProfileId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetEntity a, RelationEntity re WHERE a.tenantId = :tenantId " +
            "AND a.id = re.toId AND re.toType = 'ASSET' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                              @Param("edgeId") UUID edgeId,
                                              @Param("searchText") String searchText,
                                              Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `searchText`：`searchText` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetEntity a, RelationEntity re WHERE a.tenantId = :tenantId " +
            "AND a.id = re.toId AND re.toType = 'ASSET' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND a.type = :type " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndEdgeIdAndType(@Param("tenantId") UUID tenantId,
                                                     @Param("edgeId") UUID edgeId,
                                                     @Param("type") String type,
                                                     @Param("searchText") String searchText,
                                                     Pageable pageable);

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * 返回：数值结果。
     */
    Long countByTenantIdAndTypeIsNot(UUID tenantId, String type);

    /**
     * 功能：获取`External Id By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query("SELECT externalId FROM AssetEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    /**
     * 功能：获取资产。
     * 参数：
     * - `pageable`：`pageable` 参数。
     * 返回：处理结果。
     */
    @Query(value = "SELECT DISTINCT new org.thingsboard.server.common.data.util.TbPair(a.tenantId , a.type) FROM  AssetEntity a")
    Page<TbPair<UUID, String>> getAllAssetTypes(Pageable pageable);

}

/*
 * 本类总结：
 * 1. 核心职责：`AssetRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
