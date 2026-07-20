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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.ota.OtaPackageDao;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import static org.thingsboard.server.common.data.EntityType.OTA_PACKAGE;

/**
 * 中文说明：
 * 1. `OtaPackageDataValidator` 是 ThingsBoard DAO 中负责 OTA 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `BaseOtaPackageDataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
public class OtaPackageDataValidator extends BaseOtaPackageDataValidator<OtaPackage> {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private OtaPackageDao otaPackageDao;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private OtaPackageService otaPackageService;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackage`：`otaPackage` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, OtaPackage otaPackage) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxOtaPackagesInBytes = profileConfiguration.getMaxOtaPackagesInBytes();
        validateMaxSumDataSizePerTenant(tenantId, otaPackageDao, maxOtaPackagesInBytes, otaPackage.getDataSize(), OTA_PACKAGE);
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackage`：`otaPackage` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, OtaPackage otaPackage) {
        validateImpl(otaPackage);

        if (!otaPackage.hasUrl()) {
            if (StringUtils.isEmpty(otaPackage.getFileName())) {
                throw new DataValidationException("OtaPackage file name should be specified!");
            }

            if (StringUtils.isEmpty(otaPackage.getContentType())) {
                throw new DataValidationException("OtaPackage content type should be specified!");
            }

            if (otaPackage.getChecksumAlgorithm() == null) {
                throw new DataValidationException("OtaPackage checksum algorithm should be specified!");
            }
            if (StringUtils.isEmpty(otaPackage.getChecksum())) {
                throw new DataValidationException("OtaPackage checksum should be specified!");
            }

            String currentChecksum;

            currentChecksum = otaPackageService.generateChecksum(otaPackage.getChecksumAlgorithm(), otaPackage.getData());

            if (!currentChecksum.equals(otaPackage.getChecksum())) {
                throw new DataValidationException("Wrong otaPackage file!");
            }
        } else {
            if (otaPackage.getData() != null) {
                throw new DataValidationException("File can't be saved if URL present!");
            }
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackage`：`otaPackage` 参数。
     * 返回：判断结果。
     */
    @Override
    protected OtaPackage validateUpdate(TenantId tenantId, OtaPackage otaPackage) {
        OtaPackage otaPackageOld = otaPackageDao.findById(tenantId, otaPackage.getUuidId());

        validateUpdate(otaPackage, otaPackageOld);

        if (otaPackageOld.getData() != null && !otaPackageOld.getData().equals(otaPackage.getData())) {
            throw new DataValidationException("Updating otaPackage data is prohibited!");
        }

        if (otaPackageOld.getData() == null && otaPackage.getData() != null) {
            DefaultTenantProfileConfiguration profileConfiguration =
                    (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
            long maxOtaPackagesInBytes = profileConfiguration.getMaxOtaPackagesInBytes();
            validateMaxSumDataSizePerTenant(tenantId, otaPackageDao, maxOtaPackagesInBytes, otaPackage.getDataSize(), OTA_PACKAGE);
        }
        return otaPackageOld;
    }
}
