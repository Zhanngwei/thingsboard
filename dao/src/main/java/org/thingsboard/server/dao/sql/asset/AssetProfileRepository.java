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
package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AssetProfileEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AssetProfileRepository` 是 ThingsBoard DAO 中定义资产配置能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface AssetProfileRepository extends JpaRepository<AssetProfileEntity, UUID>, ExportableEntityRepository<AssetProfileEntity> {

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a " +
            "WHERE a.id = :assetProfileId")
    AssetProfileInfo findAssetProfileInfoById(@Param("assetProfileId") UUID assetProfileId);

    /**
     * 功能：获取资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetProfileEntity a WHERE " +
            "a.tenantId = :tenantId AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetProfileEntity> findAssetProfiles(@Param("tenantId") UUID tenantId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a WHERE " +
            "a.tenantId = :tenantId AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetProfileInfo> findAssetProfileInfos(@Param("tenantId") UUID tenantId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AssetProfileEntity a " +
            "WHERE a.tenantId = :tenantId AND a.isDefault = true")
    AssetProfileEntity findByDefaultTrueAndTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a " +
            "WHERE a.tenantId = :tenantId AND a.isDefault = true")
    AssetProfileInfo findDefaultAssetProfileInfo(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `id`：`id`ID。
     * - `profileName`：名称。
     * 返回：匹配的数据集合。
     */
    AssetProfileEntity findByTenantIdAndName(UUID id, String profileName);

    /**
     * 功能：获取`External Id By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query("SELECT externalId FROM AssetProfileEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageLink`：`imageLink` 参数。
     * - `page`：`page` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a WHERE a.tenantId = :tenantId AND a.image = :imageLink")
    List<AssetProfileInfo> findByTenantAndImageLink(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, Pageable page);

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `imageLink`：`imageLink` 参数。
     * - `page`：`page` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a WHERE a.image = :imageLink")
    List<AssetProfileInfo> findByImageLink(@Param("imageLink") String imageLink, Pageable page);

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<AssetProfileEntity> findAllByImageNotNull(Pageable pageable);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(ap.id, 'ASSET_PROFILE', ap.name) " +
            "FROM AssetProfileEntity ap WHERE ap.tenantId = :tenantId AND EXISTS " +
            "(SELECT 1 FROM AssetEntity a WHERE a.tenantId = :tenantId AND a.assetProfileId = ap.id)")
    List<EntityInfo> findActiveTenantAssetProfileNames(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'ASSET_PROFILE', a.name) " +
            "FROM AssetProfileEntity a WHERE a.tenantId = :tenantId")
    List<EntityInfo> findAllTenantAssetProfileNames(@Param("tenantId") UUID tenantId);

}
