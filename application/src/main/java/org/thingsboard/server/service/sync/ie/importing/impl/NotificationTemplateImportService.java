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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

/**
 * 中文说明：
 * 1. `NotificationTemplateImportService` 是 ThingsBoard Application 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NotificationTemplateImportService extends BaseEntityImportService<NotificationTemplateId, NotificationTemplate, EntityExportData<NotificationTemplate>> {

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationTemplateService notificationTemplateService;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, NotificationTemplate notificationTemplate, IdProvider idProvider) {
        notificationTemplate.setTenantId(tenantId);
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * - `oldEntity`：实体对象。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected NotificationTemplate prepare(EntitiesImportCtx ctx, NotificationTemplate notificationTemplate, NotificationTemplate oldEntity, EntityExportData<NotificationTemplate> exportData, IdProvider idProvider) {
        return notificationTemplate;
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected NotificationTemplate saveOrUpdate(EntitiesImportCtx ctx, NotificationTemplate notificationTemplate, EntityExportData<NotificationTemplate> exportData, IdProvider idProvider) {
        ConstraintValidator.validateFields(notificationTemplate);
        return notificationTemplateService.saveNotificationTemplate(ctx.getTenantId(), notificationTemplate);
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `savedEntity`：实体对象。
     * - `oldEntity`：实体对象。
     * 返回：无。
     */
    @Override
    protected void onEntitySaved(User user, NotificationTemplate savedEntity, NotificationTemplate oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * 返回：处理结果。
     */
    @Override
    protected NotificationTemplate deepCopy(NotificationTemplate notificationTemplate) {
        return new NotificationTemplate(notificationTemplate);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TEMPLATE;
    }

}
