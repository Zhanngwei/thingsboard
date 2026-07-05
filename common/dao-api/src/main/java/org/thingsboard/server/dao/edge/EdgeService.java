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
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`EdgeService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface EdgeService extends EntityDaoService {

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    Edge findEdgeById(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    EdgeInfo findEdgeInfoById(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    Edge findEdgeByTenantIdAndName(TenantId tenantId, String name);

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `routingKey`：键。
     * 返回：可能存在的结果。
     */
    Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey);

    /**
     * 功能：保存或创建边缘节点。
     * 参数：
     * - `edge`：`edge` 参数。
     * 返回：处理结果。
     */
    Edge saveEdge(Edge edge);

    /**
     * 功能：执行 `assignEdgeToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, CustomerId customerId);

    /**
     * 功能：执行 `unassignEdgeFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    Edge unassignEdgeFromCustomer(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：删除或清理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    void deleteEdge(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(TenantId tenantId, List<EdgeId> edgeIds);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteEdgesByTenantId(TenantId tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `edgeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<EdgeId> edgeIds);

    /**
     * 功能：执行 `unassignCustomerEdges` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    void unassignCustomerEdges(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Edge>> findEdgesByQuery(TenantId tenantId, EdgeSearchQuery query);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(TenantId tenantId);

    /**
     * 功能：执行 `assignDefaultRuleChainsToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantIdAndEntityId(TenantId tenantId, EntityId ruleChainId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Edge> findEdgesByTenantProfileId(TenantProfileId tenantProfileId, PageLink pageLink);

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    List<EdgeId> findAllRelatedEdgeIds(TenantId tenantId, EntityId entityId);

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    /**
     * 功能：获取`Missing To Related Rule Chains`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `tbRuleChainInputNodeClassName`：名称。
     * 返回：文本结果。
     */
    String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId, String tbRuleChainInputNodeClassName);

    /**
     * 功能：判断边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `activityState`：`activityState` 参数。
     * 返回：判断结果。
     */
    ListenableFuture<Boolean> isEdgeActiveAsync(TenantId tenantId, EdgeId edgeId, String activityState);
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
