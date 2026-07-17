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
package org.thingsboard.server.dao.sql.usagerecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.ApiUsageStateEntity;

import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
/**
 * 中文说明：
 * 1. `ApiUsageStateRepository` 是 ThingsBoard DAO 中定义用量统计能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface ApiUsageStateRepository extends JpaRepository<ApiUsageStateEntity, UUID> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Query("SELECT ur FROM ApiUsageStateEntity ur WHERE ur.tenantId = :tenantId " +
            "AND ur.entityId = :tenantId AND ur.entityType = 'TENANT' ")
    ApiUsageStateEntity findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    ApiUsageStateEntity findByEntityIdAndEntityType(UUID entityId, String entityType);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM ApiUsageStateEntity ur WHERE ur.tenantId = :tenantId")
    void deleteApiUsageStateByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：删除或清理实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM ApiUsageStateEntity e WHERE e.entityId = :entityId and e.entityType = :entityType")
    void deleteByEntityIdAndEntityType(@Param("entityId") UUID entityId, @Param("entityType") String entityType);
}
