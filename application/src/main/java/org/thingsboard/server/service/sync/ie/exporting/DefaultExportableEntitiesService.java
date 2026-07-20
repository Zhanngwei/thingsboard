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
package org.thingsboard.server.service.sync.ie.exporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.AccessControlService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `DefaultExportableEntitiesService` 是 ThingsBoard Application 中负责 `Exportable Entities` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `ExportableEntitiesService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultExportableEntitiesService implements ExportableEntitiesService {

    private final Map<EntityType, Dao<?>> daos = new HashMap<>();

    /**
     * 实体，提供当前类调用的业务操作。
     */
    private final EntityServiceRegistry entityServiceRegistry;
    private final AccessControlService accessControlService;

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            entity = exportableEntityDao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id) {
        E entity = findEntityById(id);

        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }
        return entity;
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityById(I id) {
        EntityType entityType = id.getEntityType();
        Dao<E> dao = getDao(entityType);
        if (dao == null) {
            throw new IllegalArgumentException("Unsupported entity type " + entityType);
        }

        return dao.findById(TenantId.SYS_TENANT_ID, id.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name) {
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            try {
                entity = exportableEntityDao.findByTenantIdAndName(tenantId.getId(), name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> PageData<E> findEntitiesByTenantId(TenantId tenantId, EntityType entityType, PageLink pageLink) {
        ExportableEntityDao<I, E> dao = getExportableEntityDao(entityType);
        if (dao != null) {
            return dao.findByTenantId(tenantId.getId(), pageLink);
        } else {
            return new PageData<>();
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public <I extends EntityId> PageData<I> findEntitiesIdsByTenantId(TenantId tenantId, EntityType entityType, PageLink pageLink) {
        ExportableEntityDao<I, ?> dao = getExportableEntityDao(entityType);
        if (dao != null) {
            return dao.findIdsByTenantId(tenantId.getId(), pageLink);
        } else {
            return new PageData<>();
        }
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public <I extends EntityId> I getExternalIdByInternal(I internalId) {
        ExportableEntityDao<I, ?> dao = getExportableEntityDao(internalId.getEntityType());
        if (dao != null) {
            return dao.getExternalIdByInternal(internalId);
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `belongsToTenant` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    private boolean belongsToTenant(HasId<? extends EntityId> entity, TenantId tenantId) {
        return tenantId.equals(((HasTenantId) entity).getTenantId());
    }


    /**
     * 功能：删除或清理`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public <I extends EntityId> void removeById(TenantId tenantId, I id) {
        EntityType entityType = id.getEntityType();
        EntityDaoService entityService = entityServiceRegistry.getServiceByEntityType(entityType);
        if (entityService == null) {
            throw new IllegalArgumentException("Unsupported entity type " + entityType);
        }
        entityService.deleteEntity(tenantId, id);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    private <I extends EntityId, E extends ExportableEntity<I>> ExportableEntityDao<I, E> getExportableEntityDao(EntityType entityType) {
        Dao<E> dao = getDao(entityType);
        if (dao instanceof ExportableEntityDao) {
            return (ExportableEntityDao<I, E>) dao;
        } else {
            return null;
        }
    }

    /**
     * 功能：获取存取组件。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    private <E> Dao<E> getDao(EntityType entityType) {
        return (Dao<E>) daos.get(entityType);
    }

    /**
     * 功能：更新`Daos`。
     * 参数：
     * - `daos`：数据列表。
     * 返回：无。
     */
    @Autowired
    private void setDaos(Collection<Dao<?>> daos) {
        daos.forEach(dao -> {
            if (dao.getEntityType() != null) {
                this.daos.put(dao.getEntityType(), dao);
            }
        });
    }

}
