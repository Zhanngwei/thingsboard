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
package org.thingsboard.server.service.entitiy.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleListener;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`TbEntityViewService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public interface TbEntityViewService extends ComponentLifecycleListener {

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `entityView`：实体对象。
     * - `existingEntityView`：实体对象。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    EntityView save(EntityView entityView, EntityView existingEntityView, User user) throws Exception;

    /**
     * 功能：更新实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `savedEntityView`：实体对象。
     * - `oldEntityView`：实体对象。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void updateEntityViewAttributes(TenantId tenantId, EntityView savedEntityView, EntityView oldEntityView, User user) throws ThingsboardException;

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void delete(EntityView entity, User user) throws ThingsboardException;

    /**
     * 功能：执行 `assignEntityViewToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：执行 `assignEntityViewToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    EntityView assignEntityViewToPublicCustomer(TenantId tenantId, EntityViewId entityViewId, User user) throws ThingsboardException;

    /**
     * 功能：执行 `assignEntityViewToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityViewId`：实体视图ID。
     * - `edge`：`edge` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    EntityView assignEntityViewToEdge(TenantId tenantId, CustomerId customerId, EntityViewId entityViewId, Edge edge, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignEntityViewFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityView`：实体对象。
     * - `edge`：`edge` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    EntityView unassignEntityViewFromEdge(TenantId tenantId, CustomerId customerId, EntityView entityView, Edge edge, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignEntityViewFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);
}

/*
 * 本类总结：
 * 1. 核心职责：`TbEntityViewService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
