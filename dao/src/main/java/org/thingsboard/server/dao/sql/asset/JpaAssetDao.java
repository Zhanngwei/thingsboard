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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.model.sql.AssetInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityInfosToDto;
import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaAssetDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Component
@SqlDao
@Slf4j
public class JpaAssetDao extends JpaAbstractDao<AssetEntity, Asset> implements AssetDao {

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetRepository assetRepository;

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
    protected Class<AssetEntity> getEntityClass() {
        return AssetEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<AssetEntity, UUID> getRepository() {
        return assetRepository;
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetInfo findAssetInfoById(TenantId tenantId, UUID assetId) {
        return DaoUtil.getData(assetRepository.findAssetInfoById(assetId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndIdIn(tenantId, assetIds)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, assetIds)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name) {
        Asset asset = DaoUtil.getData(assetRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(asset);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndAssetProfileId(
                        tenantId,
                        assetProfileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(UUID tenantId, UUID customerId, UUID assetProfileId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(
                        tenantId,
                        customerId,
                        assetProfileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityInfosToDto(tenantId, EntityType.ASSET, assetProfileRepository.findActiveTenantAssetProfileNames(tenantId)));
    }

    /**
     * 功能：统计资产配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：数值结果。
     */
    @Override
    public Long countAssetsByAssetProfileId(TenantId tenantId, UUID assetProfileId) {
        return assetRepository.countByAssetProfileId(assetProfileId);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findByTenantIdAndProfileId(
                        tenantId,
                        profileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, PageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}], edgeId [{}], type [{}] and pageLink [{}]", tenantId, edgeId, type, pageLink);
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndEdgeIdAndType(
                        tenantId,
                        edgeId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取资产。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    public PageData<TbPair<UUID, String>> getAllAssetTypes(PageLink pageLink) {
        log.debug("Try to find all asset types and pageLink [{}]", pageLink);
        return DaoUtil.pageToPageData(assetRepository.getAllAssetTypes(
                DaoUtil.toPageable(pageLink, Arrays.asList(new SortOrder("tenantId"), new SortOrder("type")))));
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public Long countByTenantId(TenantId tenantId) {
        return assetRepository.countByTenantIdAndTypeIsNot(tenantId.getId(), TB_SERVICE_QUEUE);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(assetRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset findByTenantIdAndName(UUID tenantId, String name) {
        return findAssetsByTenantIdAndName(tenantId, name).orElse(null);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAssetsByTenantId(tenantId, pageLink);
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetId getExternalIdByInternal(AssetId internalId) {
        return Optional.ofNullable(assetRepository.getExternalIdById(internalId.getId()))
                .map(AssetId::new).orElse(null);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaAssetDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
