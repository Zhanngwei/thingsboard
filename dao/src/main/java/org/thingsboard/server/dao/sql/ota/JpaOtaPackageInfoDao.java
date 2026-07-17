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
package org.thingsboard.server.dao.sql.ota;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OtaPackageInfoEntity;
import org.thingsboard.server.dao.ota.OtaPackageInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `JpaOtaPackageInfoDao` 是 ThingsBoard DAO 中负责 OTA 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`OtaPackageInfoDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Component
@SqlDao
public class JpaOtaPackageInfoDao extends JpaAbstractDao<OtaPackageInfoEntity, OtaPackageInfo> implements OtaPackageInfoDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private OtaPackageInfoRepository otaPackageInfoRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<OtaPackageInfoEntity> getEntityClass() {
        return OtaPackageInfoEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<OtaPackageInfoEntity, UUID> getRepository() {
        return otaPackageInfoRepository;
    }

    /**
     * 功能：获取`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public OtaPackageInfo findById(TenantId tenantId, UUID id) {
        return DaoUtil.getData(otaPackageInfoRepository.findOtaPackageInfoById(id));
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * 返回：处理结果。
     */
    @Transactional
    @Override
    public OtaPackageInfo save(TenantId tenantId, OtaPackageInfo otaPackageInfo) {
        OtaPackageInfo savedOtaPackage = super.save(tenantId, otaPackageInfo);
        if (otaPackageInfo.getId() == null) {
            return savedOtaPackage;
        } else {
            return findById(tenantId, savedOtaPackage.getId().getId());
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<OtaPackageInfo> findOtaPackageInfoByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(otaPackageInfoRepository
                .findAllByTenantId(
                        tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `otaPackageType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<OtaPackageInfo> findOtaPackageInfoByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType, PageLink pageLink) {
        return DaoUtil.toPageData(otaPackageInfoRepository
                .findAllByTenantIdAndTypeAndDeviceProfileIdAndHasData(
                        tenantId.getId(),
                        deviceProfileId.getId(),
                        otaPackageType,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：判断`Ota Package Used`。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * - `otaPackageType`：类型。
     * - `deviceProfileId`：设备配置ID。
     * 返回：判断结果。
     */
    @Override
    public boolean isOtaPackageUsed(OtaPackageId otaPackageId, OtaPackageType otaPackageType, DeviceProfileId deviceProfileId) {
        return otaPackageInfoRepository.isOtaPackageUsed(otaPackageId.getId(), deviceProfileId.getId(), otaPackageType.name());
    }
}
