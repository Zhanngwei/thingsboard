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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TbResourceInfoEntity;
import org.thingsboard.server.dao.resource.TbResourceInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `JpaTbResourceInfoDao` 是 ThingsBoard DAO 中负责资源存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`TbResourceInfoDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Component
@SqlDao
public class JpaTbResourceInfoDao extends JpaAbstractDao<TbResourceInfoEntity, TbResourceInfo> implements TbResourceInfoDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private TbResourceInfoRepository resourceInfoRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<TbResourceInfoEntity> getEntityClass() {
        return TbResourceInfoEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<TbResourceInfoEntity, UUID> getRepository() {
        return resourceInfoRepository;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink) {
        Set<ResourceType> resourceTypes = filter.getResourceTypes();
        if (CollectionsUtil.isEmpty(resourceTypes)) {
            resourceTypes = EnumSet.allOf(ResourceType.class);
        }
        return DaoUtil.toPageData(resourceInfoRepository
                .findAllTenantResourcesByTenantId(
                        filter.getTenantId().getId(), TenantId.NULL_UUID,
                        resourceTypes.stream().map(Enum::name).collect(Collectors.toList()),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResourceInfo> findTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink) {
        Set<ResourceType> resourceTypes = filter.getResourceTypes();
        if (CollectionsUtil.isEmpty(resourceTypes)) {
            resourceTypes = EnumSet.allOf(ResourceType.class);
        }
        return DaoUtil.toPageData(resourceInfoRepository
                .findTenantResourcesByTenantId(
                        filter.getTenantId().getId(),
                        resourceTypes.stream().map(Enum::name).collect(Collectors.toList()),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：处理结果。
     */
    @Override
    public TbResourceInfo findByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        return DaoUtil.getData(resourceInfoRepository.findByTenantIdAndResourceTypeAndResourceKey(tenantId.getId(), resourceType.name(), resourceKey));
    }

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：判断结果。
     */
    @Override
    public boolean existsByTenantIdAndResourceTypeAndResourceKey(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        return resourceInfoRepository.existsByTenantIdAndResourceTypeAndResourceKey(tenantId.getId(), resourceType.name(), resourceKey);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `prefix`：`prefix` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<String> findKeysByTenantIdAndResourceTypeAndResourceKeyPrefix(TenantId tenantId, ResourceType resourceType, String prefix) {
        return resourceInfoRepository.findKeysByTenantIdAndResourceTypeAndResourceKeyStartingWith(tenantId.getId(), resourceType.name(), prefix);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `etag`：`etag` 参数。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TbResourceInfo> findByTenantIdAndEtagAndKeyStartingWith(TenantId tenantId, String etag, String query) {
        return DaoUtil.convertDataList(resourceInfoRepository.findByTenantIdAndEtagAndResourceKeyStartingWith(tenantId.getId(), etag, query));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `etag`：`etag` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbResourceInfo findSystemOrTenantImageByEtag(TenantId tenantId, ResourceType resourceType, String etag) {
        return DaoUtil.getData(resourceInfoRepository.findSystemOrTenantImageByEtag(tenantId.getId(), resourceType.name(), etag));
    }

    /**
     * 功能：判断公钥是否存在。
     * 参数：
     * - `resourceType`：类型。
     * - `publicResourceKey`：键。
     * 返回：判断结果。
     */
    @Override
    public boolean existsByPublicResourceKey(ResourceType resourceType, String publicResourceKey) {
        return resourceInfoRepository.existsByResourceTypeAndPublicResourceKey(resourceType.name(), publicResourceKey);
    }

    /**
     * 功能：获取公钥。
     * 参数：
     * - `resourceType`：类型。
     * - `publicResourceKey`：键。
     * 返回：处理结果。
     */
    @Override
    public TbResourceInfo findPublicResourceByKey(ResourceType resourceType, String publicResourceKey) {
        return DaoUtil.getData(resourceInfoRepository.findByResourceTypeAndPublicResourceKeyAndIsPublicTrue(resourceType.name(), publicResourceKey));
    }
}
