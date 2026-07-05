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
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`AssetService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface AssetService extends EntityDaoService {

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    AssetInfo findAssetInfoById(TenantId tenantId, AssetId assetId);

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    Asset findAssetById(TenantId tenantId, AssetId assetId);

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    Asset findAssetByTenantIdAndName(TenantId tenantId, String name);

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：匹配的数据集合。
     */
    Asset saveAsset(Asset asset, boolean doValidate);

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    Asset saveAsset(Asset asset);

    /**
     * 功能：执行 `assignAssetToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `customerId`：客户IDID。
     * 返回：匹配的数据集合。
     */
    Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, CustomerId customerId);

    /**
     * 功能：执行 `unassignAssetFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    Asset unassignAssetFromCustomer(TenantId tenantId, AssetId assetId);

    /**
     * 功能：删除或清理资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    void deleteAsset(TenantId tenantId, AssetId assetId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteAssetsByTenantId(TenantId tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds);

    /**
     * 功能：执行 `unassignCustomerAssets` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    void unassignCustomerAssets(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, AssetSearchQuery query);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId);

    /**
     * 功能：执行 `assignAssetToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId);

    /**
     * 功能：执行 `unassignAssetFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    Asset unassignAssetFromEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink);
}

/*
 * 本类总结：
 * 1. 核心职责：`AssetService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
