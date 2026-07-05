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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbDeviceService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbDeviceService extends AbstractTbEntityService implements TbDeviceService {

    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ClaimDevicesService claimDevicesService;
    private final TenantService tenantService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `oldDevice`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device save(Device device, Device oldDevice, String accessToken, User user) throws Exception {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken));
            autoCommit(user, savedDevice.getId());
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, oldDevice, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `credentials`：`credentials` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, User user) throws ThingsboardException {
        boolean isCreate = device.getId() == null;
        ActionType actionType = isCreate ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device oldDevice = isCreate ? null : deviceService.findDeviceById(tenantId, device.getId());
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials));
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, oldDevice, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    @Transactional
    public void delete(Device device, User user) {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            removeAlarmsByOriginatorId(tenantId, deviceId);
            deviceService.deleteDevice(tenantId, deviceId);
            notificationEntityService.notifyDeleteDevice(tenantId, deviceId, device.getCustomerId(), device,
                    user, deviceId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), ActionType.DELETED,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignDeviceToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, customerId));
            notificationEntityService.logEntityAction(tenantId, deviceId, savedDevice, customerId, actionType, user,
                    deviceId.toString(), customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType, user,
                    e, deviceId.toString(), customerId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignDeviceFromCustomer` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device unassignDeviceFromCustomer(Device device, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(tenantId, deviceId));
            CustomerId customerId = customer.getId();

            notificationEntityService.logEntityAction(tenantId, deviceId, savedDevice, customerId, actionType, user,
                    deviceId.toString(), customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignDeviceToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, publicCustomer.getId()));

            notificationEntityService.logEntityAction(tenantId, deviceId, savedDevice, savedDevice.getCustomerId(),
                    actionType, user, deviceId.toString(), publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public DeviceCredentials getDeviceCredentialsByDeviceId(Device device, User user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId));
            notificationEntityService.logEntityAction(tenantId, deviceId, device, device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, user, deviceId.toString());
            return deviceCredentials;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.CREDENTIALS_READ, user, e, deviceId.toString());
            throw e;
        }
    }

    /**
     * 功能：更新设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials));
            notificationEntityService.notifyUpdateDeviceCredentials(tenantId, deviceId, device.getCustomerId(), device, result, user);
            return result;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.CREDENTIALS_UPDATED, user, e, deviceCredentials);
            throw e;
        }
    }

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
    @Override
    public ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, User user) {
        ListenableFuture<ClaimResult> future = claimDevicesService.claimDevice(device, customerId, secretKey);

        return Futures.transform(future, result -> {
            if (result != null && result.getResponse().equals(ClaimResponse.SUCCESS)) {
                notificationEntityService.logEntityAction(tenantId, device.getId(), result.getDevice(), customerId,
                        ActionType.ASSIGNED_TO_CUSTOMER, user, device.getId().toString(), customerId.toString(),
                        customerService.findCustomerById(tenantId, customerId).getName());
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `reclaimDevice` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, User user) {
        ListenableFuture<ReclaimResult> future = claimDevicesService.reClaimDevice(tenantId, device);

        return Futures.transform(future, result -> {
            Customer unassignedCustomer = result.getUnassignedCustomer();
            if (unassignedCustomer != null) {
                notificationEntityService.logEntityAction(tenantId, device.getId(), device, device.getCustomerId(),
                        ActionType.UNASSIGNED_FROM_CUSTOMER, user, device.getId().toString(),
                        unassignedCustomer.getId().toString(), unassignedCustomer.getName());
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `assignDeviceToTenant` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `newTenant`：租户信息或租户标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device assignDeviceToTenant(Device device, Tenant newTenant, User user) {
        TenantId tenantId = device.getTenantId();
        TenantId newTenantId = newTenant.getId();
        DeviceId deviceId = device.getId();
        try {
            Tenant tenant = tenantService.findTenantById(tenantId);
            Device assignedDevice = deviceService.assignDeviceToTenant(newTenantId, device);

            notificationEntityService.notifyAssignDeviceToTenant(tenantId, newTenantId, deviceId,
                    assignedDevice.getCustomerId(), assignedDevice, tenant, user, newTenantId.toString(), newTenant.getName());

            return assignedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.ASSIGNED_TO_TENANT, user, e, deviceId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignDeviceToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToEdge(tenantId, deviceId, edgeId));
            notificationEntityService.logEntityAction(tenantId, deviceId, savedDevice, savedDevice.getCustomerId(),
                    actionType, user, deviceId.toString(), edgeId.toString(), edge.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, deviceId.toString(), edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignDeviceFromEdge` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Device unassignDeviceFromEdge(Device device, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromEdge(tenantId, deviceId, edgeId));
            notificationEntityService.logEntityAction(tenantId, deviceId, savedDevice, savedDevice.getCustomerId(),
                    actionType, user, deviceId.toString(), edgeId.toString(), edge.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, deviceId.toString(), edgeId.toString());
            throw e;
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbDeviceService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
