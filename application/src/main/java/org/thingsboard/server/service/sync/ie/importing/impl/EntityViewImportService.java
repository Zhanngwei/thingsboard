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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

/**
 * 中文说明：
 * 1. `EntityViewImportService` 是 ThingsBoard Application 中负责实体视图的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityViewImportService extends BaseEntityImportService<EntityViewId, EntityView, EntityExportData<EntityView>> {

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    private final EntityViewService entityViewService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private TbEntityViewService tbEntityViewService;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityView`：实体对象。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, EntityView entityView, IdProvider idProvider) {
        entityView.setTenantId(tenantId);
        entityView.setCustomerId(idProvider.getInternalId(entityView.getCustomerId()));
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityView`：实体对象。
     * - `old`：`old` 参数。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected EntityView prepare(EntitiesImportCtx ctx, EntityView entityView, EntityView old, EntityExportData<EntityView> exportData, IdProvider idProvider) {
        entityView.setEntityId(idProvider.getInternalId(entityView.getEntityId()));
        return entityView;
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityView`：实体对象。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected EntityView saveOrUpdate(EntitiesImportCtx ctx, EntityView entityView, EntityExportData<EntityView> exportData, IdProvider idProvider) {
        return entityViewService.saveEntityView(entityView);
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `savedEntityView`：实体对象。
     * - `oldEntityView`：实体对象。
     * 返回：无。
     */
    @Override
    protected void onEntitySaved(User user, EntityView savedEntityView, EntityView oldEntityView) throws ThingsboardException {
        tbEntityViewService.updateEntityViewAttributes(user.getTenantId(), savedEntityView, oldEntityView, user);
        super.onEntitySaved(user, savedEntityView, oldEntityView);
        clusterService.broadcastEntityStateChangeEvent(savedEntityView.getTenantId(), savedEntityView.getId(),
                oldEntityView == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `entityView`：实体对象。
     * 返回：处理结果。
     */
    @Override
    protected EntityView deepCopy(EntityView entityView) {
        return new EntityView(entityView);
    }

    /**
     * 功能：删除或清理`For Comparison`。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    protected void cleanupForComparison(EntityView e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }

}
