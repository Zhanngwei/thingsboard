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
package org.thingsboard.server.dao.resource;

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.resourceInfo.ResourceInfoCacheKey;
import org.thingsboard.server.cache.resourceInfo.ResourceInfoEvictEvent;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. 类目的：`BaseResourceService` 是 ThingsBoard DAO 模块 中的资源与 OTA 持久化服务类型，用于管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 生命周期：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Adapter。
 */
@Service("TbResourceDaoService")
@Slf4j
@AllArgsConstructor
@Primary
public class BaseResourceService extends AbstractCachedEntityService<ResourceInfoCacheKey, TbResourceInfo, ResourceInfoEvictEvent> implements ResourceService {

    /**
     * `INCORRECT_RESOURCE_ID`常量，用于统一引用固定值。
     */
    public static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    protected final TbResourceDao resourceDao;
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    protected final TbResourceInfoDao resourceInfoDao;
    protected final ResourceDataValidator resourceValidator;

    /**
     * 功能：保存或创建`Resource`。
     * 参数：
     * - `resource`：`resource` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbResource saveResource(TbResource resource, boolean doValidate) {
        log.trace("Executing saveResource [{}]", resource);
        if (doValidate) {
            resourceValidator.validate(resource, TbResourceInfo::getTenantId);
        }
        if (resource.getData() != null) {
            resource.setEtag(calculateEtag(resource.getData()));
        }
        return doSaveResource(resource);
    }

    /**
     * 功能：保存或创建`Resource`。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbResource saveResource(TbResource resource) {
        return saveResource(resource, true);
    }

    /**
     * 功能：执行 `doSaveResource` 对应的处理。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：处理结果。
     */
    protected TbResource doSaveResource(TbResource resource) {
        TenantId tenantId = resource.getTenantId();
        try {
            TbResource saved;
            if (resource.getData() != null) {
                saved = resourceDao.save(tenantId, resource);
            } else {
                TbResourceInfo resourceInfo = saveResourceInfo(resource);
                saved = new TbResource(resourceInfo);
            }
            publishEvictEvent(new ResourceInfoEvictEvent(tenantId, resource.getId()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId())
                    .entityId(saved.getId()).created(resource.getId() == null).build());
            return saved;
        } catch (Exception t) {
            publishEvictEvent(new ResourceInfoEvictEvent(tenantId, resource.getId()));
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("resource_unq_key")) {
                throw new DataValidationException("Resource with such key already exists!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：处理结果。
     */
    private TbResourceInfo saveResourceInfo(TbResource resource) {
        return resourceInfoDao.save(resource.getTenantId(), new TbResourceInfo(resource));
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
        log.trace("Executing findResourceByTenantIdAndKey [{}] [{}] [{}]", tenantId, resourceType, resourceKey);
        return resourceDao.findResourceByTenantIdAndKey(tenantId, resourceType, resourceKey);
    }

    /**
     * 功能：获取`Resource By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    @Override
    public TbResource findResourceById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        return resourceDao.findById(tenantId, resourceId.getId());
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    @Override
    public TbResourceInfo findResourceInfoById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceInfoById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);

        return cache.getAndPutInTransaction(new ResourceInfoCacheKey(tenantId, resourceId),
                () -> resourceInfoDao.findById(tenantId, resourceId.getId()), true);
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
    public TbResourceInfo findResourceInfoByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        log.trace("Executing findResourceInfoByTenantIdAndKey [{}] [{}] [{}]", tenantId, resourceType, resourceKey);
        return resourceInfoDao.findByTenantIdAndKey(tenantId, resourceType, resourceKey);
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<TbResourceInfo> findResourceInfoByIdAsync(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceInfoById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        return resourceInfoDao.findByIdAsync(tenantId, resourceId.getId());
    }

    /**
     * 功能：删除或清理`Resource`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：无。
     */
    @Override
    public void deleteResource(TenantId tenantId, TbResourceId resourceId) {
        deleteResource(tenantId, resourceId, false);
    }

    /**
     * 功能：删除或清理`Resource`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * - `force`：`force` 参数。
     * 返回：无。
     */
    @Override
    public void deleteResource(TenantId tenantId, TbResourceId resourceId, boolean force) {
        log.trace("Executing deleteResource [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        if (!force) {
            resourceValidator.validateDelete(tenantId, resourceId);
        }
        resourceDao.removeById(tenantId, resourceId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(resourceId).build());
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
        TenantId tenantId = filter.getTenantId();
        log.trace("Executing findAllTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceInfoDao.findAllTenantResourcesByTenantId(filter, pageLink);
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
        TenantId tenantId = filter.getTenantId();
        log.trace("Executing findTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceInfoDao.findTenantResourcesByTenantId(filter, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `objectIds`：`objectIds` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TbResource> findTenantResourcesByResourceTypeAndObjectIds(TenantId tenantId, ResourceType resourceType, String[] objectIds) {
        log.trace("Executing findTenantResourcesByResourceTypeAndObjectIds [{}][{}][{}]", tenantId, resourceType, objectIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findResourcesByTenantIdAndResourceType(tenantId, resourceType, objectIds, null);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResource> findAllTenantResources(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAllTenantResources [{}][{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantId(tenantId, pageLink);
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TbResource> findTenantResourcesByResourceTypeAndPageLink(TenantId tenantId, ResourceType resourceType, PageLink pageLink) {
        log.trace("Executing findTenantResourcesByResourceTypeAndPageLink [{}][{}][{}]", tenantId, resourceType, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findResourcesByTenantIdAndResourceType(tenantId, resourceType, pageLink);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteResourcesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantResourcesRemover.removeEntities(tenantId, tenantId);
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
        return Optional.ofNullable(findResourceInfoById(tenantId, new TbResourceId(entityId.getId())));
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteResource(tenantId, (TbResourceId) id);
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

    /**
     * 功能：执行 `sumDataSizeByTenantId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public long sumDataSizeByTenantId(TenantId tenantId) {
        return resourceDao.sumDataSizeByTenantId(tenantId);
    }

    /**
     * 功能：执行 `calculateEtag` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    protected String calculateEtag(byte[] data) {
        return Hashing.sha256().hashBytes(data).toString();
    }

    private final PaginatedRemover<TenantId, TbResource> tenantResourcesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<TbResource> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return resourceDao.findAllByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, TbResource entity) {
                    deleteResource(tenantId, new TbResourceId(entity.getUuidId()));
                }
            };

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = ResourceInfoEvictEvent.class)
    @Override
    public void handleEvictEvent(ResourceInfoEvictEvent event) {
        if (event.getResourceId() != null) {
            cache.evict(new ResourceInfoCacheKey(event.getTenantId(), event.getResourceId()));
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseResourceService` 在 ThingsBoard DAO 模块 中承担资源与 OTA 持久化服务类型职责，核心目的是管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 核心流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
 * 3. 关键依赖：主要依赖或协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
