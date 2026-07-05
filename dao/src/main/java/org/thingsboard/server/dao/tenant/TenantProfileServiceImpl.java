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
package org.thingsboard.server.dao.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. 类目的：`TenantProfileServiceImpl` 是 ThingsBoard DAO 模块 中的租户、客户或用户持久化服务类型，用于管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 生命周期：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository。
 */
@Service("TenantProfileDaoService")
@Slf4j
public class TenantProfileServiceImpl extends AbstractCachedEntityService<TenantProfileCacheKey, TenantProfile, TenantProfileEvictEvent> implements TenantProfileService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final String INCORRECT_TENANT_PROFILE_ID = "Incorrect tenantProfileId ";

    /**
     * 租户，用于读取或保存对应领域对象。
     */
    @Autowired
    private TenantProfileDao tenantProfileDao;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    private DataValidator<TenantProfile> tenantProfileValidator;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = TenantProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(TenantProfileEvictEvent event) {
        List<TenantProfileCacheKey> keys = new ArrayList<>(2);
        if (event.getTenantProfileId() != null) {
            keys.add(TenantProfileCacheKey.fromId(event.getTenantProfileId()));
        }
        if (event.isDefaultProfile()) {
            keys.add(TenantProfileCacheKey.defaultProfile());
        }
        cache.evict(keys);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile findTenantProfileById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileById [{}]", tenantProfileId);
        Validator.validateId(tenantProfileId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        return cache.getAndPutInTransaction(TenantProfileCacheKey.fromId(tenantProfileId),
                () -> tenantProfileDao.findById(tenantId, tenantProfileId.getId()), true);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public EntityInfo findTenantProfileInfoById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileInfoById [{}]", tenantProfileId);
        TenantProfile profile = findTenantProfileById(tenantId, tenantProfileId);
        return profile == null ? null : new EntityInfo(profile.getId(), profile.getName());
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile saveTenantProfile(TenantId tenantId, TenantProfile tenantProfile) {
        log.trace("Executing saveTenantProfile [{}]", tenantProfile);
        tenantProfileValidator.validate(tenantProfile, (tenantProfile1) -> TenantId.SYS_TENANT_ID);
        TenantProfile savedTenantProfile;
        try {
            savedTenantProfile = tenantProfileDao.save(tenantId, tenantProfile);
            publishEvictEvent(new TenantProfileEvictEvent(savedTenantProfile.getId(), savedTenantProfile.isDefault()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                    .entityId(savedTenantProfile.getId()).created(tenantProfile.getId() == null).build());
        } catch (Exception t) {
            handleEvictEvent(new TenantProfileEvictEvent(null, tenantProfile.isDefault()));
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("tenant_profile_name_unq_key")) {
                throw new DataValidationException("Tenant profile with such name already exists!");
            } else {
                throw t;
            }
        }
        return savedTenantProfile;
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing deleteTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (tenantProfile != null && tenantProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Tenant Profile is prohibited!");
        }
        this.removeTenantProfile(tenantId, tenantProfileId, false);
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * - `isDefault`：`isDefault` 参数。
     * 返回：无。
     */
    private void removeTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId, boolean isDefault) {
        try {
            tenantProfileDao.removeById(tenantId, tenantProfileId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_tenant_profile")) {
                throw new DataValidationException("The tenant profile referenced by the tenants cannot be deleted!");
            } else {
                throw t;
            }
        }
        deleteEntityRelations(tenantId, tenantProfileId);
        publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, isDefault));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(tenantProfileId).build());
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfiles pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfileInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfileInfos(tenantId, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile findOrCreateDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findOrCreateDefaultTenantProfile");
        TenantProfile defaultTenantProfile = findDefaultTenantProfile(tenantId);
        if (defaultTenantProfile == null) {
            defaultTenantProfile = new TenantProfile();
            defaultTenantProfile.setDefault(true);
            defaultTenantProfile.setName("Default");
            TenantProfileData profileData = new TenantProfileData();
            profileData.setConfiguration(new DefaultTenantProfileConfiguration());
            defaultTenantProfile.setProfileData(profileData);
            defaultTenantProfile.setDescription("Default tenant profile");
            defaultTenantProfile.setIsolatedTbRuleEngine(false);
            defaultTenantProfile = saveTenantProfile(tenantId, defaultTenantProfile);
        }
        return defaultTenantProfile;
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile findDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfile");
        return cache.getAndPutInTransaction(TenantProfileCacheKey.defaultProfile(),
                () -> tenantProfileDao.findDefaultTenantProfile(tenantId), true);

    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public EntityInfo findDefaultTenantProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfileInfo");
        var tenantProfile = findDefaultTenantProfile(tenantId);
        return tenantProfile == null ? null : new EntityInfo(tenantProfile.getId(), tenantProfile.getName());
    }

    /**
     * 功能：更新租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean setDefaultTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing setDefaultTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (!tenantProfile.isDefault()) {
            tenantProfile.setDefault(true);
            TenantProfile previousDefaultTenantProfile = findDefaultTenantProfile(tenantId);
            boolean changed = false;
            if (previousDefaultTenantProfile == null) {
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            } else if (!previousDefaultTenantProfile.getId().equals(tenantProfile.getId())) {
                previousDefaultTenantProfile.setDefault(false);
                tenantProfileDao.save(tenantId, previousDefaultTenantProfile);
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(previousDefaultTenantProfile.getId(), false));
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ids`：`ids` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TenantProfile> findTenantProfilesByIds(TenantId tenantId, UUID[] ids) {
        return tenantProfileDao.findTenantProfilesByIds(tenantId, ids);
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteTenantProfiles(TenantId tenantId) {
        log.trace("Executing deleteTenantProfiles");
        tenantProfilesRemover.removeEntities(tenantId, null);
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
        return Optional.ofNullable(findTenantProfileById(tenantId, new TenantProfileId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT_PROFILE;
    }

    private final PaginatedRemover<String, TenantProfile> tenantProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<TenantProfile> findEntities(TenantId tenantId, String id, PageLink pageLink) {
                    return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, TenantProfile entity) {
                    removeTenantProfile(tenantId, entity.getId(), entity.isDefault());
                }
            };

}

/*
 * 本类总结：
 * 1. 核心职责：`TenantProfileServiceImpl` 在 ThingsBoard DAO 模块 中承担租户、客户或用户持久化服务类型职责，核心目的是管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 核心流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
 * 3. 关键依赖：主要依赖或协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
