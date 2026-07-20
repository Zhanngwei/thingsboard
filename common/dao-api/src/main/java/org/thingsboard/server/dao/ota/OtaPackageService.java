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
package org.thingsboard.server.dao.ota;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.nio.ByteBuffer;

/**
 * 中文说明：
 * 1. `OtaPackageService` 是 ThingsBoard Common 中定义 OTA 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityDaoService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface OtaPackageService extends EntityDaoService {

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * - `isUrl`：`isUrl` 参数。
     * 返回：处理结果。
     */
    OtaPackageInfo saveOtaPackageInfo(OtaPackageInfo otaPackageInfo, boolean isUrl);

    /**
     * 功能：保存或创建`Ota Package`。
     * 参数：
     * - `otaPackage`：`otaPackage` 参数。
     * 返回：处理结果。
     */
    OtaPackage saveOtaPackage(OtaPackage otaPackage);

    /**
     * 功能：执行 `generateChecksum` 对应的处理。
     * 参数：
     * - `checksumAlgorithm`：`checksumAlgorithm` 参数。
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    String generateChecksum(ChecksumAlgorithm checksumAlgorithm, ByteBuffer data);

    /**
     * 功能：获取`Ota Package By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：处理结果。
     */
    OtaPackage findOtaPackageById(TenantId tenantId, OtaPackageId otaPackageId);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：处理结果。
     */
    OtaPackageInfo findOtaPackageInfoById(TenantId tenantId, OtaPackageId otaPackageId);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<OtaPackageInfo> findOtaPackageInfoByIdAsync(TenantId tenantId, OtaPackageId otaPackageId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<OtaPackageInfo> findTenantOtaPackagesByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `otaPackageType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<OtaPackageInfo> findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType, PageLink pageLink);

    /**
     * 功能：删除或清理`Ota Package`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：无。
     */
    void deleteOtaPackage(TenantId tenantId, OtaPackageId otaPackageId);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteOtaPackagesByTenantId(TenantId tenantId);

    /**
     * 功能：执行 `sumDataSizeByTenantId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    long sumDataSizeByTenantId(TenantId tenantId);
}
