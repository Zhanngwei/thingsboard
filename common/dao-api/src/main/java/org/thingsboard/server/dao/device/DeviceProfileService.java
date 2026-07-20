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
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

/**
 * 中文说明：
 * 1. `DeviceProfileService` 是 ThingsBoard Common 中定义设备配置能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityDaoService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceProfileService extends EntityDaoService {

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `putInCache`：`putInCache` 参数。
     * 返回：处理结果。
     */
    DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId, boolean putInCache);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * - `putInCache`：`putInCache` 参数。
     * 返回：处理结果。
     */
    DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName, boolean putInCache);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile, boolean doValidate);

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile);

    /**
     * 功能：删除或清理设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    void deleteDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `transportType`：类型。
     * 返回：匹配的数据集合。
     */
    PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `provisionDeviceKey`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceProfile findDeviceProfileByProvisionDeviceKey(String provisionDeviceKey);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String profileName);

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    DeviceProfile createDefaultDeviceProfile(TenantId tenantId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    DeviceProfile findDefaultDeviceProfile(TenantId tenantId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId);

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：判断结果。
     */
    boolean setDefaultDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteDeviceProfilesByTenantId(TenantId tenantId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activeOnly`：`activeOnly` 参数。
     * 返回：匹配的数据集合。
     */
    List<EntityInfo> findDeviceProfileNamesByTenantId(TenantId tenantId, boolean activeOnly);

}
