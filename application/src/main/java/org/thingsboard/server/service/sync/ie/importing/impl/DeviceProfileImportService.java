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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Objects;

/**
 * 中文说明：
 * 1. `DeviceProfileImportService` 是 ThingsBoard Application 中负责设备配置的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceProfileImportService extends BaseEntityImportService<DeviceProfileId, DeviceProfile, EntityExportData<DeviceProfile>> {

    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    private final DeviceProfileService deviceProfileService;
    private final OtaPackageStateService otaPackageStateService;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, DeviceProfile deviceProfile, IdProvider idProvider) {
        deviceProfile.setTenantId(tenantId);
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceProfile`：设备信息或设备标识。
     * - `old`：`old` 参数。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected DeviceProfile prepare(EntitiesImportCtx ctx, DeviceProfile deviceProfile, DeviceProfile old, EntityExportData<DeviceProfile> exportData, IdProvider idProvider) {
        deviceProfile.setDefaultRuleChainId(idProvider.getInternalId(deviceProfile.getDefaultRuleChainId()));
        deviceProfile.setDefaultEdgeRuleChainId(idProvider.getInternalId(deviceProfile.getDefaultEdgeRuleChainId()));
        deviceProfile.setDefaultDashboardId(idProvider.getInternalId(deviceProfile.getDefaultDashboardId()));
        deviceProfile.setFirmwareId(getOldEntityField(old, DeviceProfile::getFirmwareId));
        deviceProfile.setSoftwareId(getOldEntityField(old, DeviceProfile::getSoftwareId));
        return deviceProfile;
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceProfile`：设备信息或设备标识。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected DeviceProfile saveOrUpdate(EntitiesImportCtx ctx, DeviceProfile deviceProfile, EntityExportData<DeviceProfile> exportData, IdProvider idProvider) {
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `savedDeviceProfile`：设备信息或设备标识。
     * - `oldDeviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    protected void onEntitySaved(User user, DeviceProfile savedDeviceProfile, DeviceProfile oldDeviceProfile) throws ThingsboardException {
        clusterService.onDeviceProfileChange(savedDeviceProfile, null);
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedDeviceProfile.getId(),
                oldDeviceProfile == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        otaPackageStateService.update(savedDeviceProfile,
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getFirmwareId(), savedDeviceProfile.getFirmwareId()),
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getSoftwareId(), savedDeviceProfile.getSoftwareId()));
        entityNotificationService.logEntityAction(savedDeviceProfile.getTenantId(), savedDeviceProfile.getId(), savedDeviceProfile,
                null, oldDeviceProfile == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    protected DeviceProfile deepCopy(DeviceProfile deviceProfile) {
        return new DeviceProfile(deviceProfile);
    }

    /**
     * 功能：删除或清理`For Comparison`。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    protected void cleanupForComparison(DeviceProfile deviceProfile) {
        super.cleanupForComparison(deviceProfile);
        deviceProfile.setFirmwareId(null);
        deviceProfile.setSoftwareId(null);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

}
