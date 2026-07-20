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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityInfosToDto;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. `JpaDeviceDao` 是 ThingsBoard DAO 中负责设备存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`DeviceDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
@Slf4j
public class JpaDeviceDao extends JpaAbstractDao<DeviceEntity, Device> implements DeviceDao {

    /**
     * 设备，用于读取或保存对应领域对象。
     */
    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 设备，用于读取或保存对应领域对象。
     */
    @Autowired
    private NativeDeviceRepository nativeDeviceRepository;

    /**
     * 设备配置，用于读取或保存对应领域对象。
     */
    @Autowired
    private DeviceProfileRepository deviceProfileRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<DeviceEntity> getEntityClass() {
        return DeviceEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<DeviceEntity, UUID> getRepository() {
        return deviceRepository;
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    @Override
    public DeviceInfo findDeviceInfoById(TenantId tenantId, UUID deviceId) {
        return DaoUtil.getData(deviceRepository.findDeviceInfoById(deviceId));
    }

    /**
     * 功能：保存或创建`And Flush`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    @Transactional
    public Device saveAndFlush(TenantId tenantId, Device device) {
        Device result = this.save(tenantId, device);
        deviceRepository.flush();
        return result;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantId(UUID tenantId, PageLink pageLink) {
        if (StringUtils.isEmpty(pageLink.getTextSearch())) {
            return DaoUtil.toPageData(
                    deviceRepository.findByTenantId(
                            tenantId,
                            DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(
                    deviceRepository.findByTenantId(
                            tenantId,
                            pageLink.getTextSearch(),
                            DaoUtil.toPageable(pageLink)));
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findDeviceInfosByFilter(
                        filter.getTenantId().getId(),
                        DaoUtil.getStringId(filter.getCustomerId()),
                        DaoUtil.getStringId(filter.getEdgeId()),
                        filter.getType(),
                        DaoUtil.getStringId(filter.getDeviceProfileId()),
                        filter.getActive() != null,
                        Boolean.TRUE.equals(filter.getActive()),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(deviceRepository.findDevicesByTenantIdAndIdIn(tenantId, deviceIds)));
    }

    /**
     * 功能：获取`Devices By Ids`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Device> findDevicesByIds(List<UUID> deviceIds) {
        return DaoUtil.convertDataList(deviceRepository.findDevicesByIdIn(deviceIds));
    }

    /**
     * 功能：获取`Devices By Ids Async`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Device>> findDevicesByIdsAsync(List<UUID> deviceIds) {
        return service.submit(() -> findDevicesByIds(deviceIds));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndProfileId(
                        tenantId,
                        profileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `transportType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink) {
        return DaoUtil.pageToPageData(deviceRepository.findIdsByDeviceProfileTransportType(transportType, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, deviceIds)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(device);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(UUID tenantId,
                                                                           UUID deviceProfileId,
                                                                           OtaPackageType type,
                                                                           PageLink pageLink) {
        Pageable pageable = DaoUtil.toPageable(pageLink);
        String searchText = pageLink.getTextSearch();
        Page<DeviceEntity> page = OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.findByTenantIdAndTypeAndFirmwareIdIsNull(tenantId, deviceProfileId, searchText, pageable),
                () -> deviceRepository.findByTenantIdAndTypeAndSoftwareIdIsNull(tenantId, deviceProfileId, searchText, pageable),
                type
        );
        return DaoUtil.toPageData(page);
    }

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `type`：类型。
     * 返回：数值结果。
     */
    @Override
    public Long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId, OtaPackageType type) {
        return OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.countByTenantIdAndDeviceProfileIdAndFirmwareIdIsNull(tenantId, deviceProfileId),
                () -> deviceRepository.countByTenantIdAndDeviceProfileIdAndSoftwareIdIsNull(tenantId, deviceProfileId),
                type
        );
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityInfosToDto(tenantId, EntityType.DEVICE, deviceProfileRepository.findActiveTenantDeviceProfileNames(tenantId)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public Device findDeviceByTenantIdAndId(TenantId tenantId, UUID id) {
        return DaoUtil.getData(deviceRepository.findByTenantIdAndId(tenantId.getId(), id));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Device> findDeviceByTenantIdAndIdAsync(TenantId tenantId, UUID id) {
        return service.submit(() -> DaoUtil.getData(deviceRepository.findByTenantIdAndId(tenantId.getId(), id)));
    }

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：数值结果。
     */
    @Override
    public Long countDevicesByDeviceProfileId(TenantId tenantId, UUID deviceProfileId) {
        return deviceRepository.countByDeviceProfileId(deviceProfileId);
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public Long countByTenantId(TenantId tenantId) {
        return deviceRepository.countByTenantId(tenantId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(deviceRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, PageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], edgeId [{}], type [{}] and pageLink [{}]", tenantId, edgeId, type, pageLink);
        return DaoUtil.toPageData(deviceRepository
                .findByTenantIdAndEdgeIdAndType(
                        tenantId,
                        edgeId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink) {
        log.debug("Try to find tenant device id infos by pageLink [{}]", pageLink);
        return nativeDeviceRepository.findDeviceIdInfos(DaoUtil.toPageable(pageLink));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public Device findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(deviceRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public Device findByTenantIdAndName(UUID tenantId, String name) {
        return findDeviceByTenantIdAndName(tenantId, name).orElse(null);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Device> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findDevicesByTenantId(tenantId, pageLink);
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceId getExternalIdByInternal(DeviceId internalId) {
        return Optional.ofNullable(deviceRepository.getExternalIdById(internalId.getId()))
                .map(DeviceId::new).orElse(null);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
