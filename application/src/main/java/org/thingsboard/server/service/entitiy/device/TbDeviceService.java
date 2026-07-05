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
package org.thingsboard.server.service.entitiy.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;

/**
 * 中文说明：
 * 1. 类目的：`TbDeviceService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public interface TbDeviceService {

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `oldDevice`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device save(Device device, Device oldDevice, String accessToken, User user) throws Exception;

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void delete(Device device, User user);

    /**
     * 功能：执行 `assignDeviceToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignDeviceFromCustomer` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device unassignDeviceFromCustomer(Device device, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：执行 `assignDeviceToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, User user) throws ThingsboardException;

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    DeviceCredentials getDeviceCredentialsByDeviceId(Device device, User user) throws ThingsboardException;

    /**
     * 功能：更新设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;

    /**
     * 功能：执行 `claimDevice` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `customerId`：客户IDID。
     * - `secretKey`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, User user);

    /**
     * 功能：执行 `reclaimDevice` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, User user);

    /**
     * 功能：执行 `assignDeviceToTenant` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `newTenant`：租户信息或租户标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToTenant(Device device, Tenant newTenant, User user);

    /**
     * 功能：执行 `assignDeviceToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignDeviceFromEdge` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device unassignDeviceFromEdge(Device device, Edge edge, User user) throws ThingsboardException;
}

/*
 * 本类总结：
 * 1. 核心职责：`TbDeviceService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
