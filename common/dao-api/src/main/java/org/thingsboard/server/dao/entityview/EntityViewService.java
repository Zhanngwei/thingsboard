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
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`EntityViewService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface EntityViewService extends EntityDaoService {

    /**
     * 功能：保存或创建实体视图。
     * 参数：
     * - `entityView`：实体对象。
     * 返回：处理结果。
     */
    EntityView saveEntityView(EntityView entityView);

    /**
     * 功能：保存或创建实体视图。
     * 参数：
     * - `entityView`：实体对象。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    EntityView saveEntityView(EntityView entityView, boolean doValidate);

    /**
     * 功能：执行 `assignEntityViewToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, CustomerId customerId);

    /**
     * 功能：执行 `unassignEntityViewFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：处理结果。
     */
    EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId);

    /**
     * 功能：执行 `unassignCustomerEntityViews` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    void unassignCustomerEntityViews(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：处理结果。
     */
    EntityViewInfo findEntityViewInfoById(TenantId tenantId, EntityViewId entityViewId);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：处理结果。
     */
    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `putInCache`：`putInCache` 参数。
     * 返回：处理结果。
     */
    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId, boolean putInCache);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, PageLink pageLink, String type);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, PageLink pageLink, String type);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    List<EntityView> findEntityViewsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId);

    /**
     * 功能：删除或清理实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：无。
     */
    void deleteEntityView(TenantId tenantId, EntityViewId entityViewId);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteEntityViewsByTenantId(TenantId tenantId);

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId);

    /**
     * 功能：执行 `assignEntityViewToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    EntityView assignEntityViewToEdge(TenantId tenantId, EntityViewId entityViewId, EdgeId edgeId);

    /**
     * 功能：执行 `unassignEntityViewFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    EntityView unassignEntityViewFromEdge(TenantId tenantId, EntityViewId entityViewId, EdgeId edgeId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink);
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityViewService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
