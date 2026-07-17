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
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceProfileEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `DeviceProfileRepository` 是 ThingsBoard DAO 中定义设备配置能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceProfileRepository extends JpaRepository<DeviceProfileEntity, UUID>, ExportableEntityRepository<DeviceProfileEntity> {

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d " +
            "WHERE d.id = :deviceProfileId")
    DeviceProfileInfo findDeviceProfileInfoById(@Param("deviceProfileId") UUID deviceProfileId);

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceProfileEntity> findDeviceProfiles(@Param("tenantId") UUID tenantId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceProfileInfo> findDeviceProfileInfos(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `transportType`：类型。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId AND d.transportType = :transportType " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceProfileInfo> findDeviceProfileInfos(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   @Param("transportType") DeviceTransportType transportType,
                                                   Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Query("SELECT d FROM DeviceProfileEntity d " +
            "WHERE d.tenantId = :tenantId AND d.isDefault = true")
    DeviceProfileEntity findByDefaultTrueAndTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d " +
            "WHERE d.tenantId = :tenantId AND d.isDefault = true")
    DeviceProfileInfo findDefaultDeviceProfileInfo(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `id`：`id`ID。
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    DeviceProfileEntity findByTenantIdAndName(UUID id, String profileName);

    /**
     * 功能：获取设备。
     * 参数：
     * - `provisionDeviceKey`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceProfileEntity findByProvisionDeviceKey(@Param("provisionDeviceKey") String provisionDeviceKey);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageLink`：`imageLink` 参数。
     * - `page`：`page` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId AND d.image = :imageLink")
    List<DeviceProfileInfo> findByTenantAndImageLink(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, Pageable page);

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `imageLink`：`imageLink` 参数。
     * - `page`：`page` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.image = :imageLink")
    List<DeviceProfileInfo> findByImageLink(@Param("imageLink") String imageLink, Pageable page);

    /**
     * 功能：获取`External Id By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query("SELECT externalId FROM DeviceProfileEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<DeviceProfileEntity> findAllByImageNotNull(Pageable pageable);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(dp.id, 'DEVICE_PROFILE', dp.name) " +
            "FROM DeviceProfileEntity dp WHERE dp.tenantId = :tenantId AND EXISTS " +
            "(SELECT 1 FROM DeviceEntity dv WHERE dv.tenantId = :tenantId AND dv.deviceProfileId = dp.id)")
    List<EntityInfo> findActiveTenantDeviceProfileNames(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(d.id, 'DEVICE_PROFILE', d.name) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId")
    List<EntityInfo> findAllTenantDeviceProfileNames(@Param("tenantId") UUID tenantId);

}
