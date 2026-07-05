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
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface AssetDao.
 *
 */
/**
 * 中文说明：
 * 1. 类目的：`AssetDao` 是 ThingsBoard DAO 模块 中的实体与关系持久化服务类型，用于维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 生命周期：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Graph Query。
 */
public interface AssetDao extends Dao<Asset>, TenantEntityDao, ExportableEntityDao<AssetId, Asset> {

    /**
     * Find asset info by id.
     *
     * @param tenantId the tenant id
     * @param assetId the asset id
     * @return the asset info object
     */
    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    AssetInfo findAssetInfoById(TenantId tenantId, UUID assetId);

    /**
     * Save or update asset object
     *
     * @param asset the asset object
     * @return saved asset object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    Asset save(TenantId tenantId, Asset asset);

    /**
     * Find assets by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find asset infos by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of asset info objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find assets by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find asset infos by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset info objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find asset infos by tenantId, assetProfileId and page link.
     *
     * @param tenantId the tenantId
     * @param assetProfileId the assetProfileId
     * @param pageLink the page link
     * @return the list of asset info objects
     */
    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink);

    /**
     * Find assets by tenantId and assets Ids.
     *
     * @param tenantId the tenantId
     * @param assetIds the asset Ids
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds);

    /**
     * Find assets by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find asset infos by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of asset info objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find assets by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find asset infos by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset info objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find asset infos by tenantId, customerId, assetProfileId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param assetProfileId the assetProfileId
     * @param pageLink the page link
     * @return the list of asset info objects
     */
    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(UUID tenantId, UUID customerId, UUID assetProfileId, PageLink pageLink);

    /**
     * Find assets by tenantId, customerId and assets Ids.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param assetIds the asset Ids
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds);

    /**
     * Find assets by tenantId and asset name.
     *
     * @param tenantId the tenantId
     * @param name the asset name
     * @return the optional asset object
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find tenants asset types.
     *
     * @return the list of tenant asset type objects
     */
    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId);

    /**
     * 功能：统计资产配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：数值结果。
     */
    Long countAssetsByAssetProfileId(TenantId tenantId, UUID assetProfileId);

    /**
     * Find assets by tenantId, profileId and page link.
     *
     * @param tenantId the tenantId
     * @param profileId the profileId
     * @param pageLink the page link
     * @return the list of device objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink);

    /**
     * Find assets by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    /**
     * Find assets by tenantId, edgeId, type and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, PageLink pageLink);

    /**
     * 功能：获取资产。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    PageData<TbPair<UUID, String>> getAllAssetTypes(PageLink pageLink);
}

/*
 * 本类总结：
 * 1. 核心职责：`AssetDao` 在 ThingsBoard DAO 模块 中承担实体与关系持久化服务类型职责，核心目的是维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 核心流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
 * 3. 关键依赖：主要依赖或协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
