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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
/**
 * 中文说明：
 * 1. `WidgetsBundleRepository` 是 ThingsBoard DAO 中定义 `Widgets Bundle` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface WidgetsBundleRepository extends JpaRepository<WidgetsBundleEntity, UUID>, ExportableEntityRepository<WidgetsBundleEntity> {

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alias`：`alias` 参数。
     * 返回：处理结果。
     */
    WidgetsBundleEntity findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    /**
     * 功能：获取部件。
     * 参数：
     * - `systemTenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :systemTenantId " +
            "AND (:textSearch is NULL OR ilike(wb.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetsBundleEntity> findSystemWidgetsBundles(@Param("systemTenantId") UUID systemTenantId,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    /**
     * 功能：获取部件。
     * 参数：
     * - `systemTenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id = :systemTenantId " +
                "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                "WHERE wtd.id = wbw.widget_type_id " +
                "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR EXISTS (" +
                    "SELECT 1 " +
                    "FROM unnest(wtd.tags) AS currentTag " +
                    "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                        "AND (length(:textSearch) = length(currentTag) " +
                        "OR :textSearch ILIKE currentTag || ' %' " +
                        "OR :textSearch ILIKE '% ' || currentTag " +
                        "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                "))))",
            countQuery = "SELECT count(*) FROM widgets_bundle wb WHERE wb.tenant_id = :systemTenantId " +
                "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                "WHERE wtd.id = wbw.widget_type_id " +
                "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR EXISTS (" +
                    "SELECT 1 " +
                    "FROM unnest(wtd.tags) AS currentTag " +
                    "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                        "AND (length(:textSearch) = length(currentTag) " +
                        "OR :textSearch ILIKE currentTag || ' %' " +
                        "OR :textSearch ILIKE '% ' || currentTag " +
                        "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                "))))"
    )
    Page<WidgetsBundleEntity> findSystemWidgetsBundlesFullSearch(@Param("systemTenantId") UUID systemTenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(wb.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantIds`：租户信息或租户标识。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId IN (:tenantIds) " +
            "AND (:textSearch IS NULL OR ilike(wb.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIds(@Param("tenantIds") List<UUID> tenantIds,
                                                                    @Param("textSearch") String textSearch,
                                                                    Pageable pageable);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantIds`：租户信息或租户标识。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id IN (:tenantIds) " +
                    "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                    "WHERE wtd.id = wbw.widget_type_id " +
                    "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wtd.tags) AS currentTag " +
                        "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                            "AND (length(:textSearch) = length(currentTag) " +
                            "OR :textSearch ILIKE currentTag || ' %' " +
                            "OR :textSearch ILIKE '% ' || currentTag " +
                            "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                    ")))) " +
                    "ORDER BY wb.widgets_bundle_order ASC NULLS LAST",
            countQuery = "SELECT count(*) FROM widgets_bundle wb WHERE wb.tenant_id IN (:tenantIds) " +
                    "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                    "WHERE wtd.id = wbw.widget_type_id " +
                    "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wtd.tags) AS currentTag " +
                        "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                            "AND (length(:textSearch) = length(currentTag) " +
                            "OR :textSearch ILIKE currentTag || ' %' " +
                            "OR :textSearch ILIKE '% ' || currentTag " +
                            "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIdsFullSearch(@Param("tenantIds") List<UUID> tenantIds,
                                                                              @Param("textSearch") String textSearch,
                                                                              Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    WidgetsBundleEntity findFirstByTenantIdAndTitle(UUID tenantId, String title);

    /**
     * 功能：获取`External Id By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query("SELECT externalId FROM WidgetsBundleEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageLink`：`imageLink` 参数。
     * - `lmt`：`lmt` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true, value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id = :tenantId and wb.image = :imageLink limit :lmt")
    List<WidgetsBundleEntity> findByTenantAndImageUrl(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, @Param("lmt") int lmt);

    /**
     * 功能：获取URL 地址。
     * 参数：
     * - `imageLink`：`imageLink` 参数。
     * - `lmt`：`lmt` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true, value = "SELECT * FROM widgets_bundle wb WHERE wb.image = :imageLink limit :lmt")
    List<WidgetsBundleEntity> findByImageUrl(@Param("imageLink") String imageLink, @Param("lmt") int lmt);
}
