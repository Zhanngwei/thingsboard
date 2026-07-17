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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. `AssetProfileServiceImpl` 是 ThingsBoard DAO 中负责资产配置的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCachedEntityService`、`AssetProfileService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("AssetProfileDaoService")
@Slf4j
public class AssetProfileServiceImpl extends AbstractCachedEntityService<AssetProfileCacheKey, AssetProfile, AssetProfileEvictEvent> implements AssetProfileService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    /**
     * 资产配置常量，用于统一引用固定值。
     */
    private static final String INCORRECT_ASSET_PROFILE_ID = "Incorrect assetProfileId ";

    /**
     * 资产配置常量，用于统一引用固定值。
     */
    private static final String INCORRECT_ASSET_PROFILE_NAME = "Incorrect assetProfileName ";

    /**
     * 资产配置常量，用于统一引用固定值。
     */
    private static final String ASSET_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS = "Asset profile with such name already exists!";

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileDao assetProfileDao;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetDao assetDao;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetService assetService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private DataValidator<AssetProfile> assetProfileValidator;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ImageService imageService;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = AssetProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(AssetProfileEvictEvent event) {
        List<AssetProfileCacheKey> keys = new ArrayList<>(2);
        keys.add(AssetProfileCacheKey.fromName(event.getTenantId(), event.getNewName()));
        if (event.getAssetProfileId() != null) {
            keys.add(AssetProfileCacheKey.fromId(event.getAssetProfileId()));
        }
        if (event.isDefaultProfile()) {
            keys.add(AssetProfileCacheKey.defaultProfile(event.getTenantId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(AssetProfileCacheKey.fromName(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId) {
        return findAssetProfileById(tenantId, assetProfileId, true);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `putInCache`：`putInCache` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId, boolean putInCache) {
        log.trace("Executing findAssetProfileById [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        return cache.getOrFetchFromDB(AssetProfileCacheKey.fromId(assetProfileId),
                () -> assetProfileDao.findById(tenantId, assetProfileId.getId()), true, putInCache);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findAssetProfileByName(TenantId tenantId, String profileName) {
        return findAssetProfileByName(tenantId, profileName, true);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * - `putInCache`：`putInCache` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findAssetProfileByName(TenantId tenantId, String profileName, boolean putInCache) {
        log.trace("Executing findAssetProfileByName [{}][{}]", tenantId, profileName);
        Validator.validateString(profileName, INCORRECT_ASSET_PROFILE_NAME + profileName);
        return cache.getOrFetchFromDB(AssetProfileCacheKey.fromName(tenantId, profileName),
                () -> assetProfileDao.findByName(tenantId, profileName), false, putInCache);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing findAssetProfileInfoById [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        return toAssetProfileInfo(findAssetProfileById(tenantId, assetProfileId));
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile saveAssetProfile(AssetProfile assetProfile, boolean doValidate) {
        return doSaveAssetProfile(assetProfile, doValidate);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        return doSaveAssetProfile(assetProfile, true);
    }

    /**
     * 功能：执行 `doSaveAssetProfile` 对应的处理。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：匹配的数据集合。
     */
    private AssetProfile doSaveAssetProfile(AssetProfile assetProfile, boolean doValidate) {
        log.trace("Executing saveAssetProfile [{}]", assetProfile);
        AssetProfile oldAssetProfile = null;
        if (doValidate) {
            oldAssetProfile = assetProfileValidator.validate(assetProfile, AssetProfile::getTenantId);
        } else if (assetProfile.getId() != null) {
            oldAssetProfile = findAssetProfileById(assetProfile.getTenantId(), assetProfile.getId(), false);
        }
        AssetProfile savedAssetProfile;
        try {
            imageService.replaceBase64WithImageUrl(assetProfile, "asset profile");
            savedAssetProfile = assetProfileDao.saveAndFlush(assetProfile.getTenantId(), assetProfile);
            publishEvictEvent(new AssetProfileEvictEvent(savedAssetProfile.getTenantId(), savedAssetProfile.getName(),
                    oldAssetProfile != null ? oldAssetProfile.getName() : null, savedAssetProfile.getId(), savedAssetProfile.isDefault()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedAssetProfile.getTenantId()).entityId(savedAssetProfile.getId())
                    .created(oldAssetProfile == null).build());
        } catch (Exception t) {
            handleEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(),
                    oldAssetProfile != null ? oldAssetProfile.getName() : null, null, assetProfile.isDefault()));
            checkConstraintViolation(t,
                    Map.of("asset_profile_name_unq_key", ASSET_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS,
                            "asset_profile_external_id_unq_key", "Asset profile with such external id already exists!"));
            throw t;
        }
        if (oldAssetProfile != null && !oldAssetProfile.getName().equals(assetProfile.getName())) {
            PageLink pageLink = new PageLink(100);
            PageData<Asset> pageData;
            do {
                pageData = assetDao.findAssetsByTenantIdAndProfileId(assetProfile.getTenantId().getId(), assetProfile.getUuidId(), pageLink);
                for (Asset asset : pageData.getData()) {
                    asset.setType(assetProfile.getName());
                    assetService.saveAsset(asset);
                }
                pageLink = pageLink.nextPageLink();
            } while (pageData.hasNext());
        }
        return savedAssetProfile;
    }

    /**
     * 功能：删除或清理资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteAssetProfile(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing deleteAssetProfile [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        AssetProfile assetProfile = assetProfileDao.findById(tenantId, assetProfileId.getId());
        if (assetProfile != null && assetProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Asset Profile is prohibited!");
        }
        this.removeAssetProfile(tenantId, assetProfile);
    }

    /**
     * 功能：删除或清理资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：无。
     */
    private void removeAssetProfile(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfileId assetProfileId = assetProfile.getId();
        try {
            deleteEntityRelations(tenantId, assetProfileId);
            assetProfileDao.removeById(tenantId, assetProfileId.getId());
            publishEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(),
                    null, assetProfile.getId(), assetProfile.isDefault()));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(assetProfileId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_asset_profile")) {
                throw new DataValidationException("The asset profile referenced by the assets cannot be deleted!");
            } else {
                throw t;
            }
        }
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
        log.trace("Executing findAssetProfiles tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return assetProfileDao.findAssetProfiles(tenantId, pageLink);
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
        log.trace("Executing findAssetProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return assetProfileDao.findAssetProfileInfos(tenantId, pageLink);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findOrCreateAssetProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateAssetProfile");
        AssetProfile assetProfile = findAssetProfileByName(tenantId, name, false);
        if (assetProfile == null) {
            try {
                assetProfile = this.doCreateDefaultAssetProfile(tenantId, name, name.equals("default"));
            } catch (DataValidationException e) {
                if (ASSET_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS.equals(e.getMessage())) {
                    assetProfile = findAssetProfileByName(tenantId, name, false);
                } else {
                    throw e;
                }
            }
        }
        return assetProfile;
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile createDefaultAssetProfile(TenantId tenantId) {
        log.trace("Executing createDefaultAssetProfile tenantId [{}]", tenantId);
        return doCreateDefaultAssetProfile(tenantId, "default", true);
    }

    /**
     * 功能：执行 `doCreateDefaultAssetProfile` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * - `defaultProfile`：`defaultProfile` 参数。
     * 返回：匹配的数据集合。
     */
    private AssetProfile doCreateDefaultAssetProfile(TenantId tenantId, String profileName, boolean defaultProfile) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setDefault(defaultProfile);
        assetProfile.setName(profileName);
        assetProfile.setDescription("Default asset profile");
        return saveAssetProfile(assetProfile);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findDefaultAssetProfile(TenantId tenantId) {
        log.trace("Executing findDefaultAssetProfile tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(AssetProfileCacheKey.defaultProfile(tenantId),
                () -> assetProfileDao.findDefaultAssetProfile(tenantId), true);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultAssetProfileInfo tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return toAssetProfileInfo(findDefaultAssetProfile(tenantId));
    }

    /**
     * 功能：更新资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：判断结果。
     */
    @Override
    public boolean setDefaultAssetProfile(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing setDefaultAssetProfile [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        AssetProfile assetProfile = assetProfileDao.findById(tenantId, assetProfileId.getId());
        if (!assetProfile.isDefault()) {
            assetProfile.setDefault(true);
            AssetProfile previousDefaultAssetProfile = findDefaultAssetProfile(tenantId);
            boolean changed = false;
            if (previousDefaultAssetProfile == null) {
                assetProfileDao.save(tenantId, assetProfile);
                publishEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(), null, assetProfile.getId(), true));
                changed = true;
            } else if (!previousDefaultAssetProfile.getId().equals(assetProfile.getId())) {
                previousDefaultAssetProfile.setDefault(false);
                assetProfileDao.save(tenantId, previousDefaultAssetProfile);
                assetProfileDao.save(tenantId, assetProfile);
                publishEvictEvent(new AssetProfileEvictEvent(previousDefaultAssetProfile.getTenantId(), previousDefaultAssetProfile.getName(), null, previousDefaultAssetProfile.getId(), false));
                publishEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(), null, assetProfile.getId(), true));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteAssetProfilesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAssetProfilesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAssetProfilesRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAssetProfileById(tenantId, new AssetProfileId(entityId.getId())));
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
        deleteAssetProfile(tenantId, (AssetProfileId) id);
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

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activeOnly`：`activeOnly` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityInfo> findAssetProfileNamesByTenantId(TenantId tenantId, boolean activeOnly) {
        log.trace("Executing findAssetProfileNamesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return assetProfileDao.findTenantAssetProfileNames(tenantId.getId(), activeOnly)
                .stream().sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());
    }

    private final PaginatedRemover<TenantId, AssetProfile> tenantAssetProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<AssetProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return assetProfileDao.findAssetProfiles(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, AssetProfile entity) {
                    removeAssetProfile(tenantId, entity);
                }
            };

    /**
     * 功能：执行 `toAssetProfileInfo` 对应的处理。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：匹配的数据集合。
     */
    private AssetProfileInfo toAssetProfileInfo(AssetProfile profile) {
        return profile == null ? null : new AssetProfileInfo(profile.getId(), profile.getTenantId(), profile.getName(), profile.getImage(),
                profile.getDefaultDashboardId());
    }
}
