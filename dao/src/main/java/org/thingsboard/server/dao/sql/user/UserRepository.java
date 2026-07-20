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
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.sql.UserEntity;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
/**
 * 中文说明：
 * 1. `UserRepository` 是 ThingsBoard DAO 中定义用户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * 功能：获取邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    UserEntity findByEmail(String email);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    UserEntity findByTenantIdAndEmail(UUID tenantId, String email);

    /**
     * 功能：获取`Users By Authority`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `searchText`：`searchText` 参数。
     * - `authority`：`authority` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId = :customerId AND u.authority = :authority " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findUsersByAuthority(@Param("tenantId") UUID tenantId,
                                          @Param("customerId") UUID customerId,
                                          @Param("searchText") String searchText,
                                          @Param("authority") Authority authority,
                                          Pageable pageable);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerIds`：数据列表。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId IN (:customerIds) " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findTenantAndCustomerUsers(@Param("tenantId") UUID tenantId,
                                                @Param("customerIds") Collection<UUID> customerIds,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    /**
     * 功能：获取`All By Authority`。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<UserEntity> findAllByAuthority(Authority authority, Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `tenantsIds`：租户信息或租户标识。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<UserEntity> findByAuthorityAndTenantIdIn(Authority authority, Collection<UUID> tenantsIds, Pageable pageable);

    /**
     * 功能：获取租户。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `tenantProfilesIds`：租户信息或租户标识。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT u FROM UserEntity u INNER JOIN TenantEntity t ON u.tenantId = t.id AND u.authority = :authority " +
            "INNER JOIN TenantProfileEntity p ON t.tenantProfileId = p.id " +
            "WHERE p.id IN :profiles")
    Page<UserEntity> findByAuthorityAndTenantProfilesIds(@Param("authority") Authority authority,
                                                         @Param("profiles") Collection<UUID> tenantProfilesIds,
                                                         Pageable pageable);

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    Long countByTenantId(UUID tenantId);

}
