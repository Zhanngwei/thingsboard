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
package org.thingsboard.server.dao.sql.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.TbResourceInfoEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TbResourceInfoRepository` 是 ThingsBoard DAO 中定义资源能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbResourceInfoRepository extends JpaRepository<TbResourceInfoEntity, UUID> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `systemTenantId`：租户IDID。
     * - `resourceTypes`：类型。
     * - `searchText`：`searchText` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT tr FROM TbResourceInfoEntity tr WHERE " +
            "(:searchText IS NULL OR ilike(tr.title, CONCAT('%', :searchText, '%')) = true) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemTenantId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND tr.resourceType = sr.resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))" +
            "AND tr.resourceType IN :resourceTypes")
    Page<TbResourceInfoEntity> findAllTenantResourcesByTenantId(@Param("tenantId") UUID tenantId,
                                                                @Param("systemTenantId") UUID systemTenantId,
                                                                @Param("resourceTypes") List<String> resourceTypes,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceTypes`：类型。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT ri FROM TbResourceInfoEntity ri WHERE " +
            "ri.tenantId = :tenantId " +
            "AND ri.resourceType IN :resourceTypes " +
            "AND (:searchText IS NULL OR ilike(ri.title, CONCAT('%', :searchText, '%')) = true)")
    Page<TbResourceInfoEntity> findTenantResourcesByTenantId(@Param("tenantId") UUID tenantId,
                                                             @Param("resourceTypes") List<String> resourceTypes,
                                                             @Param("searchText") String searchText,
                                                             Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：处理结果。
     */
    TbResourceInfoEntity findByTenantIdAndResourceTypeAndResourceKey(UUID tenantId, String resourceType, String resourceKey);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndResourceTypeAndResourceKey(UUID tenantId, String resourceType, String resourceKey);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `prefix`：`prefix` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(value = "SELECT r.resource_key FROM resource r WHERE r.tenant_id = :tenantId AND r.resource_type = :resourceType " +
            "AND starts_with(r.resource_key, :prefix)", nativeQuery = true)
    Set<String> findKeysByTenantIdAndResourceTypeAndResourceKeyStartingWith(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `etag`：`etag` 参数。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
                                                                            @Param("resourceType") String resourceType,
                                                                            @Param("prefix") String prefix);
    List<TbResourceInfoEntity> findByTenantIdAndEtagAndResourceKeyStartingWith(UUID tenantId, String etag, String query);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `etag`：`etag` 参数。
     * 返回：处理结果。
     */
    @Query(value = "SELECT * FROM resource r WHERE (r.tenant_id = '13814000-1dd2-11b2-8080-808080808080' OR r.tenant_id = :tenantId) " +
            "AND r.resource_type = :resourceType AND r.etag = :etag ORDER BY created_time, id LIMIT 1", nativeQuery = true)
    TbResourceInfoEntity findSystemOrTenantImageByEtag(@Param("tenantId") UUID tenantId,
    /**
     * 功能：判断公钥是否存在。
     * 参数：
     * - `resourceType`：类型。
     * - `publicResourceKey`：键。
     * 返回：判断结果。
     */
                                                       @Param("resourceType") String resourceType,
                                                       @Param("etag") String etag);
    boolean existsByResourceTypeAndPublicResourceKey(String resourceType, String publicResourceKey);

    /**
     * 功能：获取公钥。
     * 参数：
     * - `resourceType`：类型。
     * - `publicResourceKey`：键。
     * 返回：处理结果。
     */
    TbResourceInfoEntity findByResourceTypeAndPublicResourceKeyAndIsPublicTrue(String resourceType, String publicResourceKey);

}
