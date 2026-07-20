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
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

/**
 * 中文说明：
 * 1. `WidgetTypeService` 是 ThingsBoard Common 中定义部件能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityDaoService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface WidgetTypeService extends EntityDaoService {

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    WidgetType findWidgetTypeById(TenantId tenantId, WidgetTypeId widgetTypeId);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    WidgetTypeDetails findWidgetTypeDetailsById(TenantId tenantId, WidgetTypeId widgetTypeId);

    /**
     * 功能：执行 `widgetTypeExistsByTenantIdAndWidgetTypeId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：判断结果。
     */
    boolean widgetTypeExistsByTenantIdAndWidgetTypeId(TenantId tenantId, WidgetTypeId widgetTypeId);

    /**
     * 功能：保存或创建部件类型。
     * 参数：
     * - `widgetType`：类型。
     * 返回：处理结果。
     */
    WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetType);

    /**
     * 功能：删除或清理部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：无。
     */
    void deleteWidgetType(TenantId tenantId, WidgetTypeId widgetTypeId);

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeInfo> findSystemWidgetTypesByPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeInfo> findAllTenantWidgetTypesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetTypeInfo> findTenantWidgetTypesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    List<WidgetType> findWidgetTypesByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    List<WidgetTypeDetails> findWidgetTypesDetailsByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId);

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
    PageData<WidgetTypeInfo> findWidgetTypesInfosByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    List<String> findWidgetFqnsByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fqn`：`fqn` 参数。
     * 返回：处理结果。
     */
    WidgetType findWidgetTypeByTenantIdAndFqn(TenantId tenantId, String fqn);

    /**
     * 功能：更新部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeIds`：类型。
     * 返回：无。
     */
    void updateWidgetsBundleWidgetTypes(TenantId tenantId, WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds);

    /**
     * 功能：更新部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * - `widgetFqns`：数据列表。
     * 返回：无。
     */
    void updateWidgetsBundleWidgetFqns(TenantId tenantId, WidgetsBundleId widgetsBundleId, List<String> widgetFqns);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteWidgetTypesByTenantId(TenantId tenantId);

}
