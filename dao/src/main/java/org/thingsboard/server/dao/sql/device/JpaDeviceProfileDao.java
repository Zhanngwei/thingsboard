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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.model.sql.DeviceProfileEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `JpaDeviceProfileDao` 是 ThingsBoard DAO 中负责设备配置存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`DeviceProfileDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
public class JpaDeviceProfileDao extends JpaAbstractDao<DeviceProfileEntity, DeviceProfile> implements DeviceProfileDao {

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
    protected Class<DeviceProfileEntity> getEntityClass() {
        return DeviceProfileEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<DeviceProfileEntity, UUID> getRepository() {
        return deviceProfileRepository;
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, UUID deviceProfileId) {
        return deviceProfileRepository.findDeviceProfileInfoById(deviceProfileId);
    }

    /**
     * 功能：保存或创建`And Flush`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Transactional
    @Override
    public DeviceProfile saveAndFlush(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfile result = save(tenantId, deviceProfile);
        deviceProfileRepository.flush();
        return result;
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceProfileRepository.findDeviceProfiles(
                        tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `transportType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType) {
        if (StringUtils.isNotEmpty(transportType)) {
            return DaoUtil.pageToPageData(
                    deviceProfileRepository.findDeviceProfileInfos(
                            tenantId.getId(),
                            pageLink.getTextSearch(),
                            DeviceTransportType.valueOf(transportType),
                            DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.pageToPageData(
                    deviceProfileRepository.findDeviceProfileInfos(
                            tenantId.getId(),
                            pageLink.getTextSearch(),
                            DaoUtil.toPageable(pageLink)));
        }
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findDefaultDeviceProfile(TenantId tenantId) {
        return DaoUtil.getData(deviceProfileRepository.findByDefaultTrueAndTenantId(tenantId.getId()));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId) {
        return deviceProfileRepository.findDefaultDeviceProfileInfo(tenantId.getId());
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `provisionDeviceKey`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findByProvisionDeviceKey(String provisionDeviceKey) {
        return DaoUtil.getData(deviceProfileRepository.findByProvisionDeviceKey(provisionDeviceKey));
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findByName(TenantId tenantId, String profileName) {
        return DaoUtil.getData(deviceProfileRepository.findByTenantIdAndName(tenantId.getId(), profileName));
    }

    /**
     * 功能：获取`All With Images`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceProfile> findAllWithImages(PageLink pageLink) {
        return DaoUtil.toPageData(deviceProfileRepository.findAllByImageNotNull(DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activeOnly`：`activeOnly` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityInfo> findTenantDeviceProfileNames(UUID tenantId, boolean activeOnly) {
        return activeOnly ?
                deviceProfileRepository.findActiveTenantDeviceProfileNames(tenantId) :
                deviceProfileRepository.findAllTenantDeviceProfileNames(tenantId);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(deviceProfileRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(deviceProfileRepository.findByTenantIdAndName(tenantId, name));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findDeviceProfiles(TenantId.fromUUID(tenantId), pageLink);
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfileId getExternalIdByInternal(DeviceProfileId internalId) {
        return Optional.ofNullable(deviceProfileRepository.getExternalIdById(internalId.getId()))
                .map(DeviceProfileId::new).orElse(null);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageLink`：`imageLink` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<DeviceProfileInfo> findByTenantAndImageLink(TenantId tenantId, String imageLink, int limit) {
        return deviceProfileRepository.findByTenantAndImageLink(tenantId.getId(), imageLink, PageRequest.of(0, limit));
    }

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `imageLink`：`imageLink` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<DeviceProfileInfo> findByImageLink(String imageLink, int limit) {
        return deviceProfileRepository.findByImageLink(imageLink, PageRequest.of(0, limit));
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
