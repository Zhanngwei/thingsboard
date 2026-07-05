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
package org.thingsboard.server.service.edge.rpc.processor.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

/**
 * 中文说明：
 * 1. 类目的：`BaseDeviceProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class BaseDeviceProcessor extends BaseEdgeProcessor {

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `deviceUpdateMsg`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected Pair<Boolean, Boolean> saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg) {
        boolean created = false;
        boolean deviceNameUpdated = false;
        deviceCreationLock.lock();
        try {
            Device device = constructDeviceFromUpdateMsg(tenantId, deviceId, deviceUpdateMsg);
            if (device == null) {
                throw new RuntimeException("[{" + tenantId + "}] deviceUpdateMsg {" + deviceUpdateMsg + "} cannot be converted to device");
            }
            Device deviceById = deviceService.findDeviceById(tenantId, deviceId);
            if (deviceById == null) {
                created = true;
                device.setId(null);
            } else {
                device.setId(deviceId);
            }
            String deviceName = device.getName();
            Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
            if (deviceByName != null && !deviceByName.getId().equals(deviceId)) {
                deviceName = deviceName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Device with name {} already exists. Renaming device name to {}",
                        tenantId, device.getName(), deviceName);
                deviceNameUpdated = true;
            }
            device.setName(deviceName);
            setCustomerId(tenantId, created ? null : deviceById.getCustomerId(), device, deviceUpdateMsg);

            deviceValidator.validate(device, Device::getTenantId);
            if (created) {
                device.setId(deviceId);
            }
            Device savedDevice = deviceService.saveDevice(device, false);
            tbClusterService.onDeviceUpdated(savedDevice, created ? null : device);
        } catch (Exception e) {
            log.error("[{}] Failed to process device update msg [{}]", tenantId, deviceUpdateMsg, e);
            throw e;
        } finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceNameUpdated);
    }

    /**
     * 功能：更新设备凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceCredentialsUpdateMsg`：设备信息或设备标识。
     * 返回：无。
     */
    protected void updateDeviceCredentials(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        DeviceCredentials deviceCredentials = constructDeviceCredentialsFromUpdateMsg(tenantId, deviceCredentialsUpdateMsg);
        if (deviceCredentials == null) {
            throw new RuntimeException("[{" + tenantId + "}] deviceCredentialsUpdateMsg {" + deviceCredentialsUpdateMsg + "} cannot be converted to device credentials");
        }
        Device device = deviceService.findDeviceById(tenantId, deviceCredentials.getDeviceId());
        if (device != null) {
            log.debug("[{}] Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                    tenantId, device.getName(), deviceCredentials.getCredentialsId(), deviceCredentials.getCredentialsValue());
            try {
                DeviceCredentials deviceCredentialsByDeviceId = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
                if (deviceCredentialsByDeviceId == null) {
                    deviceCredentialsByDeviceId = new DeviceCredentials();
                    deviceCredentialsByDeviceId.setDeviceId(device.getId());
                }
                deviceCredentialsByDeviceId.setCredentialsType(deviceCredentials.getCredentialsType());
                deviceCredentialsByDeviceId.setCredentialsId(deviceCredentials.getCredentialsId());
                deviceCredentialsByDeviceId.setCredentialsValue(deviceCredentials.getCredentialsValue());
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentialsByDeviceId);

            } catch (Exception e) {
                log.error("[{}] Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]",
                        tenantId, device.getName(), deviceCredentialsUpdateMsg, e);
                throw new RuntimeException(e);
            }
        } else {
            log.warn("[{}] Can't find device by id [{}], deviceCredentialsUpdateMsg [{}]", tenantId, deviceCredentials.getDeviceId(), deviceCredentialsUpdateMsg);
        }
    }

    /**
     * 功能：执行 `constructDeviceFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `deviceUpdateMsg`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected abstract Device constructDeviceFromUpdateMsg(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg);

    /**
     * 功能：更新客户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `device`：设备信息或设备标识。
     * - `deviceUpdateMsg`：设备信息或设备标识。
     * 返回：无。
     */
    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, Device device, DeviceUpdateMsg deviceUpdateMsg);

    /**
     * 功能：执行 `constructDeviceCredentialsFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceCredentialsUpdateMsg`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected abstract DeviceCredentials constructDeviceCredentialsFromUpdateMsg(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg);

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseDeviceProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
