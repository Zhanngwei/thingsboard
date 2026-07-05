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
import org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`WidgetTypeInfoRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public interface WidgetTypeInfoRepository extends JpaRepository<WidgetTypeInfoEntity, UUID>  {

    /**
     * 功能：获取部件。
     * 参数：
     * - `systemTenantId`：租户IDID。
     * - `searchText`：`searchText` 参数。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilterEnabled`：`deprecatedFilterEnabled` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.tenant_id = :systemTenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti WHERE wti.tenant_id = :systemTenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findSystemWidgetTypes(@Param("systemTenantId") UUID systemTenantId,
                                                          @Param("searchText") String searchText,
                                                          @Param("fullSearch") boolean fullSearch,
                                                          @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                          @Param("deprecatedFilter") boolean deprecatedFilter,
                                                          @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                          @Param("widgetTypes") List<String> widgetTypes,
                                                          Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `nullTenantId`：租户IDID。
     * - `searchText`：`searchText` 参数。
     * - `fullSearch`：`fullSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.tenant_id IN (:tenantId, :nullTenantId) " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti WHERE wti.tenant_id IN (:tenantId, :nullTenantId) " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findAllTenantWidgetTypesByTenantId(@Param("tenantId") UUID tenantId,
                                                                  @Param("nullTenantId") UUID nullTenantId,
                                                                  @Param("searchText") String searchText,
                                                                  @Param("fullSearch") boolean fullSearch,
                                                                  @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                                  @Param("deprecatedFilter") boolean deprecatedFilter,
                                                                  @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                                  @Param("widgetTypes") List<String> widgetTypes,
                                                                  Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `searchText`：`searchText` 参数。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilterEnabled`：`deprecatedFilterEnabled` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.tenant_id = :tenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti WHERE wti.tenant_id = :tenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findTenantWidgetTypesByTenantId(@Param("tenantId") UUID tenantId,
                                                               @Param("searchText") String searchText,
                                                               @Param("fullSearch") boolean fullSearch,
                                                               @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                               @Param("deprecatedFilter") boolean deprecatedFilter,
                                                               @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                               @Param("widgetTypes") List<String> widgetTypes,
                                                               Pageable pageable);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wti FROM WidgetTypeInfoEntity wti, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wti.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeInfoEntity> findWidgetTypesInfosByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `searchText`：`searchText` 参数。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilterEnabled`：`deprecatedFilterEnabled` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti, widgets_bundle_widget wbw " +
                    "WHERE wbw.widgets_bundle_id = :widgetsBundleId " +
                    "AND wbw.widget_type_id = wti.id " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    ")))) " +
                    "ORDER BY wbw.widget_type_order",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti, widgets_bundle_widget wbw " +
                    "WHERE wbw.widgets_bundle_id = :widgetsBundleId " +
                    "AND wbw.widget_type_id = wti.id " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findWidgetTypesInfosByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId,
                                                                     @Param("searchText") String searchText,
                                                                     @Param("fullSearch") boolean fullSearch,
                                                                     @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                                     @Param("deprecatedFilter") boolean deprecatedFilter,
                                                                     @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                                     @Param("widgetTypes") List<String> widgetTypes,
                                                                     Pageable pageable);


    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageLink`：`imageLink` 参数。
     * - `lmt`：`lmt` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.id IN " +
                    "(select id from widget_type where tenant_id = :tenantId " +
                    "and (image = :imageLink or descriptor ILIKE CONCAT('%\"', :imageLink, '\"%')) limit :lmt)"
    )
    List<WidgetTypeInfoEntity> findByTenantAndImageUrl(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, @Param("lmt") int lmt);

    /**
     * 功能：获取URL 地址。
     * 参数：
     * - `imageLink`：`imageLink` 参数。
     * - `lmt`：`lmt` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.id IN " +
                    "(select id from widget_type where image = :imageLink or descriptor ILIKE CONCAT('%', :imageLink, '%') limit :lmt)"
    )
    List<WidgetTypeInfoEntity> findByImageUrl(@Param("imageLink") String imageLink, @Param("lmt") int lmt);
}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetTypeInfoRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
