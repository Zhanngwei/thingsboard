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
package org.thingsboard.server.service.entitiy;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;

/**
 * 中文说明：
 * 1. 类目的：`TbNotificationEntityService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public interface TbNotificationEntityService {

    /**
     * 功能：执行 `logEntityAction` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `actionType`：类型。
     * - `user`：`user` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    <I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, ActionType actionType, User user,
                                              Exception e, Object... additionalInfo);

    /**
     * 功能：执行 `logEntityAction` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `entity`：实体对象。
     * - `actionType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, ActionType actionType,
                                                                 User user, Object... additionalInfo);

    /**
     * 功能：执行 `logEntityAction` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `entity`：实体对象。
     * - `actionType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, ActionType actionType,
                                                                 User user, Exception e, Object... additionalInfo);

    /**
     * 功能：执行 `logEntityAction` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `entity`：实体对象。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                 ActionType actionType, User user, Object... additionalInfo);

    /**
     * 功能：执行 `logEntityAction` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `entity`：实体对象。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                 ActionType actionType, User user, Exception e,
                                                                 Object... additionalInfo);

    /**
     * 功能：通知租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * - `event`：`event` 参数。
     * 返回：无。
     */
    void notifyCreateOrUpdateTenant(Tenant tenant, ComponentLifecycleEvent event);

    /**
     * 功能：通知租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * 返回：无。
     */
    void notifyDeleteTenant(Tenant tenant);

    /**
     * 功能：通知设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customerId`：客户IDID。
     * - `device`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void notifyCreateOrUpdateDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                    Device oldDevice, ActionType actionType, User user, Object... additionalInfo);

    /**
     * 功能：通知设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customerId`：客户IDID。
     * - `device`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void notifyDeleteDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                            User user, Object... additionalInfo);

    /**
     * 功能：通知设备凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customerId`：客户IDID。
     * - `device`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void notifyUpdateDeviceCredentials(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                       DeviceCredentials deviceCredentials, User user);

    /**
     * 功能：通知租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `newTenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void notifyAssignDeviceToTenant(TenantId tenantId, TenantId newTenantId, DeviceId deviceId, CustomerId customerId,
                                    Device device, Tenant tenant, User user, Object... additionalInfo);

    /**
     * 功能：通知边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `customerId`：客户IDID。
     * - `edge`：`edge` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void notifyCreateOrUpdateOrDeleteEdge(TenantId tenantId, EdgeId edgeId, CustomerId customerId, Edge edge, ActionType actionType,
                                          User user, Object... additionalInfo);

    /**
     * 功能：执行 `logEntityRelationAction` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `relation`：`relation` 参数。
     * - `user`：`user` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void logEntityRelationAction(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user,
                                 ActionType actionType, Exception e, Object... additionalInfo);
}

/*
 * 本类总结：
 * 1. 核心职责：`TbNotificationEntityService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
