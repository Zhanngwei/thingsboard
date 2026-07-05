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


import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * 中文说明：
 * 1. 类目的：`BaseAssetService` 是 ThingsBoard DAO 模块 中的实体与关系持久化服务类型，用于维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 生命周期：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Graph Query。
 */
@Service("AssetDaoService")
@Slf4j
public class BaseAssetService extends AbstractCachedEntityService<AssetCacheKey, Asset, AssetCacheEvictEvent> implements AssetService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String INCORRECT_ASSET_PROFILE_ID = "Incorrect assetProfileId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    /**
     * 资产ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_ASSET_ID = "Incorrect assetId ";
    public static final String TB_SERVICE_QUEUE = "TbServiceQueue";

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetDao assetDao;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileService assetProfileService;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private DataValidator<Asset> assetValidator;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityCountService countService;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = AssetCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(AssetCacheEvictEvent event) {
        List<AssetCacheKey> keys = new ArrayList<>(2);
        keys.add(new AssetCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new AssetCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetInfo findAssetInfoById(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetInfoById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findAssetInfoById(tenantId, assetId.getId());
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset findAssetById(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findById(tenantId, assetId.getId());
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findByIdAsync(tenantId, assetId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset findAssetByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findAssetByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(new AssetCacheKey(tenantId, name),
                () -> assetDao.findAssetsByTenantIdAndName(tenantId.getId(), name)
                        .orElse(null), true);
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset saveAsset(Asset asset, boolean doValidate) {
        return doSaveAsset(asset, doValidate);
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset saveAsset(Asset asset) {
        return doSaveAsset(asset, true);
    }

    /**
     * 功能：执行 `doSaveAsset` 对应的处理。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：匹配的数据集合。
     */
    private Asset doSaveAsset(Asset asset, boolean doValidate) {
        log.trace("Executing saveAsset [{}]", asset);
        Asset oldAsset = null;
        if (doValidate) {
            oldAsset = assetValidator.validate(asset, Asset::getTenantId);
        } else if (asset.getId() != null) {
            oldAsset = findAssetById(asset.getTenantId(), asset.getId());
        }
        AssetCacheEvictEvent evictEvent = new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), oldAsset != null ? oldAsset.getName() : null);
        Asset savedAsset;
        try {
            AssetProfile assetProfile;
            if (asset.getAssetProfileId() == null) {
                if (!StringUtils.isEmpty(asset.getType())) {
                    assetProfile = this.assetProfileService.findOrCreateAssetProfile(asset.getTenantId(), asset.getType());
                } else {
                    assetProfile = this.assetProfileService.findDefaultAssetProfile(asset.getTenantId());
                }
                asset.setAssetProfileId(new AssetProfileId(assetProfile.getId().getId()));
            } else {
                assetProfile = this.assetProfileService.findAssetProfileById(asset.getTenantId(), asset.getAssetProfileId());
                if (assetProfile == null) {
                    throw new DataValidationException("Asset is referencing non existing asset profile!");
                }
                if (!assetProfile.getTenantId().equals(asset.getTenantId())) {
                    throw new DataValidationException("Asset can`t be referencing to asset profile from different tenant!");
                }
            }
            asset.setType(assetProfile.getName());
            savedAsset = assetDao.saveAndFlush(asset.getTenantId(), asset);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedAsset.getTenantId())
                    .entityId(savedAsset.getId()).created(asset.getId() == null).build());
            if (asset.getId() == null) {
                countService.publishCountEntityEvictEvent(savedAsset.getTenantId(), EntityType.ASSET);
            }
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t,
                    "asset_name_unq_key", "Asset with such name already exists!",
                    "asset_external_id_unq_key", "Asset with such external id already exists!");
            throw t;
        }
        return savedAsset;
    }

    /**
     * 功能：执行 `assignAssetToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `customerId`：客户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, CustomerId customerId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(customerId);
        return saveAsset(asset);
    }

    /**
     * 功能：执行 `unassignAssetFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset unassignAssetFromCustomer(TenantId tenantId, AssetId assetId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(null);
        return saveAsset(asset);
    }

    /**
     * 功能：删除或清理资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteAsset(TenantId tenantId, AssetId assetId) {
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        if (entityViewService.existsByTenantIdAndEntityId(tenantId, assetId)) {
            throw new DataValidationException("Can't delete asset that has entity views!");
        }

        Asset asset = assetDao.findById(tenantId, assetId.getId());
        alarmService.deleteEntityAlarmRelations(tenantId, assetId);
        deleteAsset(tenantId, asset);
    }

    /**
     * 功能：删除或清理资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `asset`：`asset` 参数。
     * 返回：无。
     */
    private void deleteAsset(TenantId tenantId, Asset asset) {
        log.trace("Executing deleteAsset [{}]", asset.getId());
        relationService.deleteEntityRelations(tenantId, asset.getId());

        assetDao.removeById(tenantId, asset.getUuidId());

        publishEvictEvent(new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), null));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.ASSET);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(asset.getId()).build());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Asset> findAssetsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantId(tenantId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantId(tenantId.getId(), pageLink);
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
    public PageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndType(tenantId.getId(), type, pageLink);
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
    public PageData<AssetInfo> findAssetInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
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
    public PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndAssetProfileId, tenantId [{}], assetProfileId [{}], pageLink [{}]", tenantId, assetProfileId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndAssetProfileId(tenantId.getId(), assetProfileId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndIdsAsync, tenantId [{}], assetIds [{}]", tenantId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(assetIds));
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteAssetsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAssetsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAssetsRemover.removeEntities(tenantId, tenantId);
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
    public PageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
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
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
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
    public PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
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
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
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
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId, tenantId [{}], customerId [{}], assetProfileId [{}], pageLink [{}]", tenantId, customerId, assetProfileId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(tenantId.getId(), customerId.getId(), assetProfileId.getId(), pageLink);
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
    public ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], assetIds [{}]", tenantId, customerId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId.getId(), customerId.getId(), toUUIDs(assetIds));
    }

    /**
     * 功能：执行 `unassignCustomerAssets` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    public void unassignCustomerAssets(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerAssets, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerAssetsUnasigner.removeEntities(tenantId, customerId);
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, AssetSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Asset>> assets = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Asset>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ASSET) {
                    futures.add(findAssetByIdAsync(tenantId, new AssetId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());
        assets = Futures.transform(assets, assetList ->
                        assetList == null ?
                                Collections.emptyList() :
                                assetList.stream()
                                        .filter(asset -> query.getAssetTypes().contains(asset.getType()))
                                        .collect(Collectors.toList()),
                MoreExecutors.directExecutor()
        );
        return assets;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findAssetTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return assetDao.findTenantAssetTypesAsync(tenantId.getId());
    }

    /**
     * 功能：执行 `assignAssetToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId) {
        Asset asset = findAssetById(tenantId, assetId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign asset to non-existent edge!");
        }
        if (!edge.getTenantId().getId().equals(asset.getTenantId().getId())) {
            throw new DataValidationException("Can't assign asset to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create asset relation. Edge Id: [{}]", assetId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(assetId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return asset;
    }

    /**
     * 功能：执行 `unassignAssetFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId) {
        Asset asset = findAssetById(tenantId, assetId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign asset from non-existent edge!");
        }

        checkAssignedEntityViewsToEdge(tenantId, assetId, edgeId);

        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete asset relation. Edge Id: [{}]", assetId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(assetId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return asset;
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
    public PageData<Asset> findAssetsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
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
    public PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}] pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    private final PaginatedRemover<TenantId, Asset> tenantAssetsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Asset> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return assetDao.findAssetsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Asset asset) {
            deleteAsset(tenantId, asset);
        }
    };

    private final PaginatedRemover<CustomerId, Asset> customerAssetsUnasigner = new PaginatedRemover<CustomerId, Asset>() {

        @Override
        protected PageData<Asset> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Asset entity) {
            unassignAssetFromCustomer(tenantId, new AssetId(entity.getId().getId()));
        }
    };

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAssetById(tenantId, new AssetId(entityId.getId())));
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public long countByTenantId(TenantId tenantId) {
        return assetDao.countByTenantId(tenantId);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteAsset(tenantId, (AssetId) id);
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
 * 1. 核心职责：`BaseAssetService` 在 ThingsBoard DAO 模块 中承担实体与关系持久化服务类型职责，核心目的是维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 核心流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
 * 3. 关键依赖：主要依赖或协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
