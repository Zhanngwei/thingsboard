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

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Objects;

/**
 * 中文说明：
 * 1. `BaseOtaPackageDataValidator` 是 ThingsBoard DAO 中负责 OTA 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `BaseData`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
public abstract class BaseOtaPackageDataValidator<D extends BaseData<?>> extends DataValidator<D> {

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    @Lazy
    private TenantService tenantService;

    /**
     * 设备配置，用于读取或保存对应领域对象。
     */
    @Autowired
    @Getter
    private DeviceProfileDao deviceProfileDao;

    /**
     * 功能：校验`Impl`。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * 返回：无。
     */
    protected void validateImpl(OtaPackageInfo otaPackageInfo) {
        validateString("OtaPackage title", otaPackageInfo.getTitle());

        if (otaPackageInfo.getTenantId() == null) {
            throw new DataValidationException("OtaPackage should be assigned to tenant!");
        } else {
            if (!getTenantService().tenantExists(otaPackageInfo.getTenantId())) {
                throw new DataValidationException("OtaPackage is referencing to non-existent tenant!");
            }
        }

        if (otaPackageInfo.getDeviceProfileId() != null) {
            DeviceProfile deviceProfile = getDeviceProfileDao().findById(otaPackageInfo.getTenantId(), otaPackageInfo.getDeviceProfileId().getId());
            if (deviceProfile == null) {
                throw new DataValidationException("OtaPackage is referencing to non-existent device profile!");
            }
        }

        if (otaPackageInfo.getType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (StringUtils.isEmpty(otaPackageInfo.getVersion())) {
            throw new DataValidationException("OtaPackage version should be specified!");
        }

        if (otaPackageInfo.getTitle().length() > 255) {
            throw new DataValidationException("The length of title should be equal or shorter than 255");
        }

        if (otaPackageInfo.getVersion().length() > 255) {
            throw new DataValidationException("The length of version should be equal or shorter than 255");
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `otaPackage`：`otaPackage` 参数。
     * - `otaPackageOld`：`otaPackageOld` 参数。
     * 返回：无。
     */
    protected void validateUpdate(OtaPackageInfo otaPackage, OtaPackageInfo otaPackageOld) {
        if (!otaPackageOld.getType().equals(otaPackage.getType())) {
            throw new DataValidationException("Updating type is prohibited!");
        }

        if (!otaPackageOld.getTitle().equals(otaPackage.getTitle())) {
            throw new DataValidationException("Updating otaPackage title is prohibited!");
        }

        if (!otaPackageOld.getVersion().equals(otaPackage.getVersion())) {
            throw new DataValidationException("Updating otaPackage version is prohibited!");
        }

        if (!Objects.equals(otaPackage.getTag(), otaPackageOld.getTag())) {
            throw new DataValidationException("Updating otaPackage tag is prohibited!");
        }

        if (!otaPackageOld.getDeviceProfileId().equals(otaPackage.getDeviceProfileId())) {
            throw new DataValidationException("Updating otaPackage deviceProfile is prohibited!");
        }

        if (otaPackageOld.getFileName() != null && !otaPackageOld.getFileName().equals(otaPackage.getFileName())) {
            throw new DataValidationException("Updating otaPackage file name is prohibited!");
        }

        if (otaPackageOld.getContentType() != null && !otaPackageOld.getContentType().equals(otaPackage.getContentType())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksumAlgorithm() != null && !otaPackageOld.getChecksumAlgorithm().equals(otaPackage.getChecksumAlgorithm())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksum() != null && !otaPackageOld.getChecksum().equals(otaPackage.getChecksum())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getDataSize() != null && !otaPackageOld.getDataSize().equals(otaPackage.getDataSize())) {
            throw new DataValidationException("Updating otaPackage data size is prohibited!");
        }

        if (otaPackageOld.getUrl() != null && !otaPackageOld.getUrl().equals(otaPackage.getUrl())) {
            throw new DataValidationException("Updating otaPackage URL is prohibited!");
        }
    }

}
