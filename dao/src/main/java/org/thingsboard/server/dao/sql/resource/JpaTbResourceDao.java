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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TbResourceEntity;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`JpaTbResourceDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Slf4j
@Component
@SqlDao
public class JpaTbResourceDao extends JpaAbstractDao<TbResourceEntity, TbResource> implements TbResourceDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final TbResourceRepository resourceRepository;

    /**
     * 功能：创建 `JpaTbResourceDao` 实例，并初始化必要字段。
     * 参数：
     * - `resourceRepository`：`resourceRepository` 参数。
     * 返回：新创建的对象实例。
     */
    public JpaTbResourceDao(TbResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<TbResourceEntity> getEntityClass() {
        return TbResourceEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<TbResourceEntity, UUID> getRepository() {
        return resourceRepository;
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
    public TbResource findResourceByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        return DaoUtil.getData(resourceRepository.findByTenantIdAndResourceTypeAndResourceKey(tenantId.getId(), resourceType.name(), resourceKey));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResource> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                                       ResourceType resourceType,
                                                                       PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findResourcesPage(
                tenantId.getId(),
                TenantId.SYS_TENANT_ID.getId(),
                resourceType.name(),
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)
        ));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `objectIds`：`objectIds` 参数。
     * - `searchText`：`searchText` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId, ResourceType resourceType,
                                                                   String[] objectIds,
                                                                   String searchText) {
        return objectIds == null ?
                DaoUtil.convertDataList(resourceRepository.findResources(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(),
                        searchText)) :
                DaoUtil.convertDataList(resourceRepository.findResourcesByIds(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(), objectIds));
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    @Override
    public byte[] getResourceData(TenantId tenantId, TbResourceId resourceId) {
        return resourceRepository.getDataById(resourceId.getId());
    }

    /**
     * 功能：获取`Resource Preview`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    @Override
    public byte[] getResourcePreview(TenantId tenantId, TbResourceId resourceId) {
        return resourceRepository.getPreviewById(resourceId.getId());
    }

    /**
     * 功能：获取`Resource Size`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：数值结果。
     */
    @Override
    public long getResourceSize(TenantId tenantId, TbResourceId resourceId) {
        return resourceRepository.getDataSizeById(resourceId.getId());
    }

    /**
     * 功能：执行 `sumDataSizeByTenantId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public Long sumDataSizeByTenantId(TenantId tenantId) {
        return resourceRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public TbResource findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(resourceRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResource> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAllByTenantId(TenantId.fromUUID(tenantId), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResourceId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(resourceRepository.findIdsByTenantId(tenantId, DaoUtil.toPageable(pageLink))
                .map(TbResourceId::new));
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public TbResourceId getExternalIdByInternal(TbResourceId internalId) {
        return DaoUtil.toEntityId(resourceRepository.getExternalIdByInternal(internalId.getId()), TbResourceId::new);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaTbResourceDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
