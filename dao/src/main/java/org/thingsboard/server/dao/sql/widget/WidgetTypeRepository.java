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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeIdFqnEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`WidgetTypeRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public interface WidgetTypeRepository extends JpaRepository<WidgetTypeDetailsEntity, UUID>, ExportableEntityRepository<WidgetTypeDetailsEntity> {

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.id = :widgetTypeId")
    WidgetTypeEntity findWidgetTypeById(@Param("widgetTypeId") UUID widgetTypeId);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wtd FROM WidgetTypeDetailsEntity wtd WHERE wtd.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(wtd.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetTypeDetailsEntity> findTenantWidgetTypeDetailsByTenantId(@Param("tenantId") UUID tenantId,
                                                                        @Param("textSearch") String textSearch,
                                                                        Pageable pageable);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wt FROM WidgetTypeEntity wt, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wt.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeEntity> findWidgetTypesByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wtd FROM WidgetTypeDetailsEntity wtd, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wtd.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeDetailsEntity> findWidgetTypesDetailsByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);


    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wtd.fqn FROM WidgetTypeDetailsEntity wtd, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wtd.id ORDER BY wbw.widgetTypeOrder")
    List<String> findWidgetFqnsByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetFqns`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.WidgetTypeIdFqnEntity(wtd.id, wtd.fqn) FROM WidgetTypeDetailsEntity wtd " +
            "WHERE wtd.tenantId = :tenantId " +
            "AND wtd.fqn IN (:widgetFqns)")
    List<WidgetTypeIdFqnEntity> findWidgetTypeIdsByTenantIdAndFqns(@Param("tenantId") UUID tenantId, @Param("widgetFqns") List<String> widgetFqns);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fqn`：`fqn` 参数。
     * 返回：处理结果。
     */
    @Query("SELECT wt FROM WidgetTypeEntity wt " +
            "WHERE wt.tenantId = :tenantId AND wt.fqn = :fqn")
    WidgetTypeEntity findWidgetTypeByTenantIdAndFqn(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：匹配的数据集合。
     */
                                                    @Param("fqn") String fqn);

    @Query(value = "SELECT * FROM widget_type wt " +
            "WHERE wt.tenant_id = :tenantId AND cast(wt.descriptor as json) ->> 'resources' LIKE LOWER(CONCAT('%', :resourceId, '%'))",
            nativeQuery = true)
    List<WidgetTypeDetailsEntity> findWidgetTypesInfosByTenantIdAndResourceId(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取`External Id By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
                                                                              @Param("resourceId") UUID resourceId);

    @Query("SELECT externalId FROM WidgetTypeDetailsEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    /**
     * 功能：获取`All Ids`。
     * 参数：
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT w.id FROM WidgetTypeDetailsEntity w")
    Page<UUID> findAllIds(Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT w.id FROM WidgetTypeDetailsEntity w WHERE w.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetTypeRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
