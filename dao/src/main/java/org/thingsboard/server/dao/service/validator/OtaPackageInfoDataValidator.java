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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ota.OtaPackageInfoDao;

/**
 * 中文说明：
 * 1. `OtaPackageInfoDataValidator` 是 ThingsBoard DAO 中承载 OTA 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseOtaPackageDataValidator`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Component
public class OtaPackageInfoDataValidator extends BaseOtaPackageDataValidator<OtaPackageInfo> {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private OtaPackageInfoDao otaPackageInfoDao;

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, OtaPackageInfo otaPackageInfo) {
        validateImpl(otaPackageInfo);
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackage`：`otaPackage` 参数。
     * 返回：判断结果。
     */
    @Override
    protected OtaPackageInfo validateUpdate(TenantId tenantId, OtaPackageInfo otaPackage) {
        OtaPackageInfo otaPackageOld = otaPackageInfoDao.findById(tenantId, otaPackage.getUuidId());
        validateUpdate(otaPackage, otaPackageOld);
        return otaPackageOld;
    }
}
