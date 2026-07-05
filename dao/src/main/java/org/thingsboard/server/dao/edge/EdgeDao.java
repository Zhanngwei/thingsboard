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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface EdgeDao.
 *
 */
/**
 * 中文说明：
 * 1. 类目的：`EdgeDao` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
public interface EdgeDao extends Dao<Edge> {

    /**
     * Save or update edge object
     *
     * @param edge the edge object
     * @return saved edge object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * 返回：处理结果。
     */
    Edge save(TenantId tenantId, Edge edge);

    /**
     * Find edge info by id.
     *
     * @param tenantId the tenant id
     * @param edgeId the edge id
     * @return the edge info object
     */
    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    EdgeInfo findEdgeInfoById(TenantId tenantId, UUID edgeId);

    /**
     * Find edges by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find edges by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find edges by tenantId and edges Ids.
     *
     * @param tenantId the tenantId
     * @param edgeIds the edge Ids
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds);

    /**
     * Find edges by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find edges by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find edge infos by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of edge info objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find edge infos by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of edge info objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find edges by tenantId, customerId and edges Ids.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param edgeIds the edge Ids
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `edgeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    /**
     * Find edges by tenantId and edge name.
     *
     * @param tenantId the tenantId
     * @param name the edge name
     * @return the optional edge object
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find tenants edge types.
     *
     * @return the list of tenant edge type objects
     */
    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId);

    /**
     * Find edge by routing Key.
     *
     * @param routingKey the edge routingKey
     * @return the optional edge object
     */
    /**
     * 功能：获取键。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `routingKey`：键。
     * 返回：可能存在的结果。
     */
    Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find edges by tenantId and entityId.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param entityType the entityType
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `entityType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink);

    /**
     * Find edges by tenantProfileId.
     *
     * @param tenantProfileId the tenantProfileId
     * @return the list of edge objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantProfileId(UUID tenantProfileId, PageLink pageLink);

}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeDao` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
