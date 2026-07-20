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
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.model.sql.DashboardEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. `JpaDashboardDao` 是 ThingsBoard DAO 中负责仪表盘存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`DashboardDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
public class JpaDashboardDao extends JpaAbstractDao<DashboardEntity, Dashboard> implements DashboardDao {

    /**
     * 仪表盘，用于读取或保存对应领域对象。
     */
    @Autowired
    DashboardRepository dashboardRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<DashboardEntity> getEntityClass() {
        return DashboardEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<DashboardEntity, UUID> getRepository() {
        return dashboardRepository;
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public Long countByTenantId(TenantId tenantId) {
        return dashboardRepository.countByTenantId(tenantId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public Dashboard findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(dashboardRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Dashboard> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public DashboardId getExternalIdByInternal(DashboardId internalId) {
        return Optional.ofNullable(dashboardRepository.getExternalIdById(internalId.getId()))
                .map(DashboardId::new).orElse(null);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `title`：`title` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Dashboard> findByTenantIdAndTitle(UUID tenantId, String title) {
        return DaoUtil.convertDataList(dashboardRepository.findByTenantIdAndTitle(tenantId, title));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardId> findIdsByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(dashboardRepository.findIdsByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)).map(DashboardId::new));
    }

    /**
     * 功能：获取`All Ids`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardId> findAllIds(PageLink pageLink) {
        return DaoUtil.pageToPageData(dashboardRepository.findAllIds(DaoUtil.toPageable(pageLink)).map(DashboardId::new));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
