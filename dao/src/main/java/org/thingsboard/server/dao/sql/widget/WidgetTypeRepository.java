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
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeIdFqnEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `WidgetTypeRepository` 是 ThingsBoard DAO 中定义部件能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface WidgetTypeRepository extends JpaRepository<WidgetTypeDetailsEntity, UUID>, ExportableEntityRepository<WidgetTypeDetailsEntity> {

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.id = :widgetTypeId")
    WidgetTypeEntity findWidgetTypeById(@Param("widgetTypeId") UUID widgetTypeId);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wtd FROM WidgetTypeDetailsEntity wtd WHERE wtd.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(wtd.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetTypeDetailsEntity> findTenantWidgetTypeDetailsByTenantId(@Param("tenantId") UUID tenantId,
                                                                        @Param("textSearch") String textSearch,
                                                                        Pageable pageable);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wt FROM WidgetTypeEntity wt, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wt.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeEntity> findWidgetTypesByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wtd FROM WidgetTypeDetailsEntity wtd, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wtd.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeDetailsEntity> findWidgetTypesDetailsByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);


    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT wtd.fqn FROM WidgetTypeDetailsEntity wtd, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wtd.id ORDER BY wbw.widgetTypeOrder")
    List<String> findWidgetFqnsByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetFqns`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.WidgetTypeIdFqnEntity(wtd.id, wtd.fqn) FROM WidgetTypeDetailsEntity wtd " +
            "WHERE wtd.tenantId = :tenantId " +
            "AND wtd.fqn IN (:widgetFqns)")
    List<WidgetTypeIdFqnEntity> findWidgetTypeIdsByTenantIdAndFqns(@Param("tenantId") UUID tenantId, @Param("widgetFqns") List<String> widgetFqns);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fqn`：`fqn` 参数。
     * 返回：处理结果。
     */
    @Query("SELECT wt FROM WidgetTypeEntity wt " +
            "WHERE wt.tenantId = :tenantId AND wt.fqn = :fqn")
    WidgetTypeEntity findWidgetTypeByTenantIdAndFqn(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：匹配的数据集合。
     */
                                                    @Param("fqn") String fqn);

    @Query(value = "SELECT * FROM widget_type wt " +
            "WHERE wt.tenant_id = :tenantId AND cast(wt.descriptor as json) ->> 'resources' LIKE LOWER(CONCAT('%', :resourceId, '%'))",
            nativeQuery = true)
    List<WidgetTypeDetailsEntity> findWidgetTypesInfosByTenantIdAndResourceId(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取`External Id By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
                                                                              @Param("resourceId") UUID resourceId);

    @Query("SELECT externalId FROM WidgetTypeDetailsEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    /**
     * 功能：获取`All Ids`。
     * 参数：
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT w.id FROM WidgetTypeDetailsEntity w")
    Page<UUID> findAllIds(Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT w.id FROM WidgetTypeDetailsEntity w WHERE w.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}
