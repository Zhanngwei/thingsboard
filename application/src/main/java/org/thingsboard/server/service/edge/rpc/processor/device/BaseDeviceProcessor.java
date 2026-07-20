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
 * 1. `BaseDeviceProcessor` 是 ThingsBoard Application 中处理设备的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
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
