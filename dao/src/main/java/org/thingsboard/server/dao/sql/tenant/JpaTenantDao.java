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
package org.thingsboard.server.dao.sql.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TenantEntity;
import org.thingsboard.server.dao.model.sql.TenantInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
/**
 * 中文说明：
 * 1. `JpaTenantDao` 是 ThingsBoard DAO 中负责租户存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`TenantDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
public class JpaTenantDao extends JpaAbstractDao<TenantEntity, Tenant> implements TenantDao {

    /**
     * 租户，用于读取或保存对应领域对象。
     */
    @Autowired
    private TenantRepository tenantRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<TenantEntity> getEntityClass() {
        return TenantEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<TenantEntity, UUID> getRepository() {
        return tenantRepository;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public TenantInfo findTenantInfoById(TenantId tenantId, UUID id) {
        return DaoUtil.getData(tenantRepository.findTenantInfoById(id));
    }

    /**
     * 功能：获取`Tenants`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Tenant> findTenants(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findTenantsNextPage(
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TenantInfo> findTenantInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findTenantInfosNextPage(
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, TenantInfoEntity.tenantInfoColumnMap)));
    }

    /**
     * 功能：获取`Tenants Ids`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TenantId> findTenantsIds(PageLink pageLink) {
        return DaoUtil.pageToPageData(tenantRepository.findTenantsIds(DaoUtil.toPageable(pageLink))).mapData(TenantId::fromUUID);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId) {
        return tenantRepository.findTenantIdsByTenantProfileId(tenantProfileId.getId()).stream()
                .map(TenantId::fromUUID)
                .collect(Collectors.toList());
    }
}
