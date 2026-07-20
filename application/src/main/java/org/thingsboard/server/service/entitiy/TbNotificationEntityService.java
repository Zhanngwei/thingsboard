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
 * 1. `TbNotificationEntityService` 是 ThingsBoard Application 中定义通知能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
