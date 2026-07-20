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
package org.thingsboard.server.dao.widget;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.ImageContainerDao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface WidgetTypeDao.
 */
/**
 * 中文说明：
 * 1. `WidgetTypeDao` 是 ThingsBoard DAO 中定义部件能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface WidgetTypeDao extends Dao<WidgetTypeDetails>, ExportableEntityDao<WidgetTypeId, WidgetTypeDetails>, ImageContainerDao<WidgetTypeInfo> {

    /**
     * Save or update widget type object
     *
     * @param widgetTypeDetails the widget type details object
     * @return saved widget type object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeDetails`：类型。
     * 返回：处理结果。
     */
    WidgetTypeDetails save(TenantId tenantId, WidgetTypeDetails widgetTypeDetails);

    /**
     * Find widget type by tenantId and widgetTypeId.
     *
     * @param tenantId the tenantId
     * @param widgetTypeId the widget type id
     * @return the widget type object
     */
    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    WidgetType findWidgetTypeById(TenantId tenantId, UUID widgetTypeId);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndId(TenantId tenantId, UUID widgetTypeId);

    /**
     * 功能：获取部件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeInfo> findSystemWidgetTypes(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeInfo> findAllTenantWidgetTypesByTenantId(UUID tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeInfo> findTenantWidgetTypesByTenantId(UUID tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * Find widget types by widgetsBundleId.
     *
     * @param tenantId the tenantId
     * @param widgetsBundleId the widgets bundle id
     * @return the list of widget types objects
     */
    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    List<WidgetType> findWidgetTypesByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    /**
     * Find widget types details by widgetsBundleId.
     *
     * @param tenantId the tenantId
     * @param widgetsBundleId the widgets bundle id
     * @return the list of widget types details objects
     */
    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    List<WidgetTypeDetails> findWidgetTypesDetailsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeInfo> findWidgetTypesInfosByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    List<String> findWidgetFqnsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    /**
     * Find widget type by tenantId and FQN.
     *
     * @param tenantId the tenantId
     * @param fqn the FQN
     * @return the widget type object
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fqn`：`fqn` 参数。
     * 返回：处理结果。
     */
    WidgetType findByTenantIdAndFqn(UUID tenantId, String fqn);

    /**
     * Find widget types infos by tenantId and resourceId in descriptor.
     *
     * @param tenantId the tenantId
     * @param tbResourceId the resourceId
     * @return the list of widget types infos objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tbResourceId`：`tbResourceId`ID。
     * 返回：匹配的数据集合。
     */
    List<WidgetTypeDetails> findWidgetTypesInfosByTenantIdAndResourceId(UUID tenantId, UUID tbResourceId);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetFqns`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<WidgetTypeId> findWidgetTypeIdsByTenantIdAndFqns(UUID tenantId, List<String> widgetFqns);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    List<WidgetsBundleWidget> findWidgetsBundleWidgetsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    /**
     * 功能：保存或创建部件包。
     * 参数：
     * - `widgetsBundleWidget`：`widgetsBundleWidget` 参数。
     * 返回：无。
     */
    void saveWidgetsBundleWidget(WidgetsBundleWidget widgetsBundleWidget);

    /**
     * 功能：删除或清理部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：无。
     */
    void removeWidgetTypeFromWidgetsBundle(UUID widgetsBundleId, UUID widgetTypeId);

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeId> findAllWidgetTypesIds(PageLink pageLink);

}
