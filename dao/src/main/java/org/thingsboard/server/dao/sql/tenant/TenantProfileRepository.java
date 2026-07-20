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
package org.thingsboard.server.dao.sql.tenant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.sql.TenantProfileEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TenantProfileRepository` 是 ThingsBoard DAO 中定义租户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TenantProfileRepository extends JpaRepository<TenantProfileEntity, UUID> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE t.id = :tenantProfileId")
    EntityInfo findTenantProfileInfoById(@Param("tenantProfileId") UUID tenantProfileId);

    /**
     * 功能：获取租户。
     * 参数：
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT t FROM TenantProfileEntity t WHERE " +
            "(:textSearch IS NULL OR ilike(t.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<TenantProfileEntity> findTenantProfiles(@Param("textSearch") String textSearch,
                                                 Pageable pageable);

    /**
     * 功能：获取租户。
     * 参数：
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE (:textSearch IS NULL OR ilike(t.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityInfo> findTenantProfileInfos(@Param("textSearch") String textSearch,
                                            Pageable pageable);

    /**
     * 功能：获取`By Default True`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Query("SELECT t FROM TenantProfileEntity t " +
            "WHERE t.isDefault = true")
    TenantProfileEntity findByDefaultTrue();

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE t.isDefault = true")
    EntityInfo findDefaultTenantProfileInfo();

    /**
     * 功能：获取`By Id In`。
     * 参数：
     * - `ids`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<TenantProfileEntity> findByIdIn(List<UUID> ids);

}
