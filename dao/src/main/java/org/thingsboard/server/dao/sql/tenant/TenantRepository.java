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
import org.thingsboard.server.dao.model.sql.TenantEntity;
import org.thingsboard.server.dao.model.sql.TenantInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
/**
 * 中文说明：
 * 1. `TenantRepository` 是 ThingsBoard DAO 中定义租户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.TenantInfoEntity(t, p.name) " +
            "FROM TenantEntity t " +
            "LEFT JOIN TenantProfileEntity p on p.id = t.tenantProfileId " +
            "WHERE t.id = :tenantId")
    TenantInfoEntity findTenantInfoById(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取`Tenants Next Page`。
     * 参数：
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT t FROM TenantEntity t WHERE (:textSearch IS NULL OR ilike(t.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<TenantEntity> findTenantsNextPage(@Param("textSearch") String textSearch,
                                           Pageable pageable);

    /**
     * 功能：获取租户。
     * 参数：
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.TenantInfoEntity(t, p.name) " +
            "FROM TenantEntity t " +
            "LEFT JOIN TenantProfileEntity p on p.id = t.tenantProfileId " +
            "WHERE (:textSearch IS NULL OR ilike(t.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<TenantInfoEntity> findTenantInfosNextPage(@Param("textSearch") String textSearch,
                                                          Pageable pageable);

    /**
     * 功能：获取`Tenants Ids`。
     * 参数：
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT t.id FROM TenantEntity t")
    Page<UUID> findTenantsIds(Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT t.id FROM TenantEntity t where t.tenantProfileId = :tenantProfileId")
    List<UUID> findTenantIdsByTenantProfileId(@Param("tenantProfileId") UUID tenantProfileId);
}
