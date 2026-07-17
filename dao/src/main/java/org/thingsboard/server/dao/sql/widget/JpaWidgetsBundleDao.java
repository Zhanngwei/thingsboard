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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
/**
 * 中文说明：
 * 1. `JpaWidgetsBundleDao` 是 ThingsBoard DAO 中负责 `Jpa Widgets Bundle` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`WidgetsBundleDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
public class JpaWidgetsBundleDao extends JpaAbstractDao<WidgetsBundleEntity, WidgetsBundle> implements WidgetsBundleDao {

    /**
     * 部件包，用于读取或保存对应领域对象。
     */
    @Autowired
    private WidgetsBundleRepository widgetsBundleRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<WidgetsBundleEntity> getEntityClass() {
        return WidgetsBundleEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<WidgetsBundleEntity, UUID> getRepository() {
        return widgetsBundleRepository;
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alias`：`alias` 参数。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias) {
        return DaoUtil.getData(widgetsBundleRepository.findWidgetsBundleByTenantIdAndAlias(tenantId, alias));
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId, boolean fullSearch, PageLink pageLink) {
        if (fullSearch) {
            return DaoUtil.toPageData(
                    widgetsBundleRepository
                            .findSystemWidgetsBundlesFullSearch(
                                    NULL_UUID,
                                    pageLink.getTextSearch(),
                                    DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(
                    widgetsBundleRepository
                            .findSystemWidgetsBundles(
                                    NULL_UUID,
                                    pageLink.getTextSearch(),
                                    DaoUtil.toPageable(pageLink)));
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                widgetsBundleRepository
                        .findTenantWidgetsBundlesByTenantId(
                                tenantId,
                                pageLink.getTextSearch(),
                                DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, boolean fullSearch, PageLink pageLink) {
        return findTenantWidgetsBundlesByTenantIds(Arrays.asList(tenantId, NULL_UUID), fullSearch, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, boolean fullSearch, PageLink pageLink) {
        return findTenantWidgetsBundlesByTenantIds(Collections.singletonList(tenantId), fullSearch, pageLink);
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetsBundle> findAllWidgetsBundles(PageLink pageLink) {
        return DaoUtil.toPageData(widgetsBundleRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantIds`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    private PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantIds(List<UUID> tenantIds, boolean fullSearch, PageLink pageLink) {
        if (fullSearch) {
            return DaoUtil.toPageData(
                    widgetsBundleRepository
                            .findAllTenantWidgetsBundlesByTenantIdsFullSearch(
                                    tenantIds,
                                    pageLink.getTextSearch(),
                                    DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(
                    widgetsBundleRepository
                            .findAllTenantWidgetsBundlesByTenantIds(
                                    tenantIds,
                                    pageLink.getTextSearch(),
                                    DaoUtil.toPageable(pageLink)));
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundle findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(widgetsBundleRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundle findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(widgetsBundleRepository.findFirstByTenantIdAndTitle(tenantId, name));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetsBundle> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundleId getExternalIdByInternal(WidgetsBundleId internalId) {
        return Optional.ofNullable(widgetsBundleRepository.getExternalIdById(internalId.getId()))
                .map(WidgetsBundleId::new).orElse(null);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageUrl`：`imageUrl` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetsBundle> findByTenantAndImageLink(TenantId tenantId, String imageUrl, int limit) {
        return DaoUtil.convertDataList(widgetsBundleRepository.findByTenantAndImageUrl(tenantId.getId(), imageUrl, limit));
    }

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `imageUrl`：`imageUrl` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetsBundle> findByImageLink(String imageUrl, int limit) {
         return DaoUtil.convertDataList(widgetsBundleRepository.findByImageUrl(imageUrl, limit));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

}
