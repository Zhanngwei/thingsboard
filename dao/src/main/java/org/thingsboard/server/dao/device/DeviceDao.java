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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface DeviceDao.
 *
 */
/**
 * 中文说明：
 * 1. `DeviceDao` 是 ThingsBoard DAO 中定义设备能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceDao extends Dao<Device>, TenantEntityDao, ExportableEntityDao<DeviceId, Device> {

    /**
     * Find device info by id.
     *
     * @param tenantId the tenant id
     * @param deviceId the device id
     * @return the device info object
     */
    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceInfo findDeviceInfoById(TenantId tenantId, UUID deviceId);

    /**
     * Save or update device object
     *
     * @param device the device object
     * @return saved device object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    Device save(TenantId tenantId, Device device);

    /**
     * Save or update device object
     *
     * @param device the device object
     * @return saved device object
     */
    /**
     * 功能：保存或创建`And Flush`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    Device saveAndFlush(TenantId tenantId, Device device);

    /**
     * Find devices by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find devices by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(UUID tenantId,
                                                                    UUID deviceProfileId,
                                                                    OtaPackageType type,
                                                                    PageLink pageLink);

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `otaPackageType`：类型。
     * 返回：数值结果。
     */
    Long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId, OtaPackageType otaPackageType);

    /**
     * Find devices by tenantId and devices Ids.
     *
     * @param tenantId the tenantId
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds);

    /**
     * Find devices by devices Ids.
     *
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    /**
     * 功能：获取`Devices By Ids`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    List<Device> findDevicesByIds(List<UUID> deviceIds);

    /**
     * Find devices by devices Ids.
     *
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    /**
     * 功能：获取`Devices By Ids Async`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Device>> findDevicesByIdsAsync(List<UUID> deviceIds);

    /**
     * Find devices by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find devices by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find devices by tenantId, customerId and devices Ids.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    /**
     * Find devices by tenantId and device name.
     *
     * @param tenantId the tenantId
     * @param name the device name
     * @return the optional device object
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find tenants device types.
     *
     * @return the list of tenant device type objects
     */
    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId);

    /**
     * Find devices by tenantId and device id.
     * @param tenantId the tenant Id
     * @param id the device Id
     * @return the device object
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    Device findDeviceByTenantIdAndId(TenantId tenantId, UUID id);

    /**
     * Find devices by tenantId and device id.
     * @param tenantId tenantId the tenantId
     * @param id the deviceId
     * @return the device object
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Device> findDeviceByTenantIdAndIdAsync(TenantId tenantId, UUID id);

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：数值结果。
     */
    Long countDevicesByDeviceProfileId(TenantId tenantId, UUID deviceProfileId);

    /**
     * Find devices by tenantId, profileId and page link.
     *
     * @param tenantId the tenantId
     * @param profileId the profileId
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `transportType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink);

    /**
     * Find devices by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    /**
     * Find devices by tenantId, edgeId, type and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, PageLink pageLink);

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink);

    /**
     * 功能：获取设备。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink);
}
