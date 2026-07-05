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
package org.thingsboard.server.dao.sql.asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.model.sql.AssetProfileEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`JpaAssetProfileDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Component
public class JpaAssetProfileDao extends JpaAbstractDao<AssetProfileEntity, AssetProfile> implements AssetProfileDao {

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileRepository assetProfileRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    protected Class<AssetProfileEntity> getEntityClass() {
        return AssetProfileEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<AssetProfileEntity, UUID> getRepository() {
        return assetProfileRepository;
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, UUID assetProfileId) {
        return assetProfileRepository.findAssetProfileInfoById(assetProfileId);
    }

    /**
     * 功能：保存或创建`And Flush`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：匹配的数据集合。
     */
    @Transactional
    @Override
    public AssetProfile saveAndFlush(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfile result = save(tenantId, assetProfile);
        assetProfileRepository.flush();
        return result;
    }

    /**
     * 功能：获取资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetProfileRepository.findAssetProfiles(
                        tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                assetProfileRepository.findAssetProfileInfos(
                        tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findDefaultAssetProfile(TenantId tenantId) {
        return DaoUtil.getData(assetProfileRepository.findByDefaultTrueAndTenantId(tenantId.getId()));
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId) {
        return assetProfileRepository.findDefaultAssetProfileInfo(tenantId.getId());
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findByName(TenantId tenantId, String profileName) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId.getId(), profileName));
    }

    /**
     * 功能：获取`All With Images`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetProfile> findAllWithImages(PageLink pageLink) {
        return DaoUtil.toPageData(assetProfileRepository.findAllByImageNotNull(DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activeOnly`：`activeOnly` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityInfo> findTenantAssetProfileNames(UUID tenantId, boolean activeOnly) {
        return activeOnly ?
                assetProfileRepository.findActiveTenantAssetProfileNames(tenantId) :
                assetProfileRepository.findAllTenantAssetProfileNames(tenantId);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId, name));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAssetProfiles(TenantId.fromUUID(tenantId), pageLink);
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfileId getExternalIdByInternal(AssetProfileId internalId) {
        return Optional.ofNullable(assetProfileRepository.getExternalIdById(internalId.getId()))
                .map(AssetProfileId::new).orElse(null);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageLink`：`imageLink` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<AssetProfileInfo> findByTenantAndImageLink(TenantId tenantId, String imageLink, int limit) {
        return assetProfileRepository.findByTenantAndImageLink(tenantId.getId(), imageLink, PageRequest.of(0, limit));
    }

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `imageLink`：`imageLink` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<AssetProfileInfo> findByImageLink(String imageLink, int limit) {
        return assetProfileRepository.findByImageLink(imageLink, PageRequest.of(0, limit));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaAssetProfileDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
