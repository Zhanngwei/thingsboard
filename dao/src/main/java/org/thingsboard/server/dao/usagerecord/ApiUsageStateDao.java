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
package org.thingsboard.server.dao.usagerecord;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `ApiUsageStateDao` 是 ThingsBoard DAO 中定义用量统计能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface ApiUsageStateDao extends Dao<ApiUsageState> {

    /**
     * Save or update usage record object
     *
     * @param apiUsageState the usage record
     * @return saved usage record entity
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `apiUsageState`：`apiUsageState` 参数。
     * 返回：处理结果。
     */
    ApiUsageState save(TenantId tenantId, ApiUsageState apiUsageState);

    /**
     * Find usage record by tenantId.
     *
     * @param tenantId the tenantId
     * @return the corresponding usage record
     */
    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    ApiUsageState findTenantApiUsageState(UUID tenantId);

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    ApiUsageState findApiUsageStateByEntityId(EntityId entityId);

    /**
     * Delete usage record by tenantId.
     *
     * @param tenantId the tenantId
     */
    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteApiUsageStateByTenantId(TenantId tenantId);

    /**
     * 功能：删除或清理实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    void deleteApiUsageStateByEntityId(EntityId entityId);
}
