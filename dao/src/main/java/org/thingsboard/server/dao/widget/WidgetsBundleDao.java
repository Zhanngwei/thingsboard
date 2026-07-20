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

import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.ImageContainerDao;

import java.util.UUID;

/**
 * The Interface WidgetsBundleDao.
 */
/**
 * 中文说明：
 * 1. `WidgetsBundleDao` 是 ThingsBoard DAO 中定义 `Widgets Bundle` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface WidgetsBundleDao extends Dao<WidgetsBundle>, ExportableEntityDao<WidgetsBundleId, WidgetsBundle>, ImageContainerDao<WidgetsBundle> {

    /**
     * Save or update widgets bundle object
     *
     * @param tenantId the tenantId
     * @param widgetsBundle the widgets bundle object
     * @return saved widgets bundle object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：处理结果。
     */
    WidgetsBundle save(TenantId tenantId, WidgetsBundle widgetsBundle);

    /**
     * Find widgets bundle by tenantId and alias.
     *
     * @param tenantId the tenantId
     * @param alias the alias
     * @return the widgets bundle object
     */
    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alias`：`alias` 参数。
     * 返回：处理结果。
     */
    WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    /**
     * Find system widgets bundles by page link.
     *
     * @param pageLink the page link
     * @return the list of widgets bundles objects
     */
    /**
     * 功能：获取部件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId, boolean fullSearch, PageLink pageLink);

    /**
     * Find tenant widgets bundles by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of widgets bundles objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find all tenant widgets bundles (including system) by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of widgets bundles objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, boolean fullSearch, PageLink pageLink);

    /**
     * Find all tenant widgets bundles (does not include system) by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of widgets bundles objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, boolean fullSearch, PageLink pageLink);

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<WidgetsBundle> findAllWidgetsBundles(PageLink pageLink);

}
