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
import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.List;

/**
 * 中文说明：
 * 1. `NotificationTargetImportService` 是 ThingsBoard Application 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NotificationTargetImportService extends BaseEntityImportService<NotificationTargetId, NotificationTarget, EntityExportData<NotificationTarget>> {

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationTargetService notificationTargetService;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationTarget`：`notificationTarget` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, NotificationTarget notificationTarget, IdProvider idProvider) {
        notificationTarget.setTenantId(tenantId);
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationTarget`：`notificationTarget` 参数。
     * - `oldNotificationTarget`：`oldNotificationTarget` 参数。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected NotificationTarget prepare(EntitiesImportCtx ctx, NotificationTarget notificationTarget, NotificationTarget oldNotificationTarget, EntityExportData<NotificationTarget> exportData, IdProvider idProvider) {
        if (notificationTarget.getConfiguration().getType() == NotificationTargetType.PLATFORM_USERS) {
            UsersFilter usersFilter = ((PlatformUsersNotificationTargetConfig) notificationTarget.getConfiguration()).getUsersFilter();
            switch (usersFilter.getType()) {
                case CUSTOMER_USERS:
                    CustomerUsersFilter customerUsersFilter = (CustomerUsersFilter) usersFilter;
                    customerUsersFilter.setCustomerId(idProvider.getInternalId(new CustomerId(customerUsersFilter.getCustomerId())).getId());
                    break;
                case USER_LIST:
                    UserListFilter userListFilter = (UserListFilter) usersFilter;
                    userListFilter.setUsersIds(List.of(ctx.getUser().getUuidId())); // user entities are not supported by VC; replacing with current user id
                    break;
                case TENANT_ADMINISTRATORS:
                    if (CollectionUtils.isNotEmpty(((TenantAdministratorsFilter) usersFilter).getTenantsIds()) ||
                            CollectionUtils.isNotEmpty(((TenantAdministratorsFilter) usersFilter).getTenantProfilesIds())) {
                        throw new IllegalArgumentException("Permission denied");
                    }
                    break;
                case SYSTEM_ADMINISTRATORS:
                    throw new AccessDeniedException("Permission denied");
            }
        }
        return notificationTarget;
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationTarget`：`notificationTarget` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected NotificationTarget saveOrUpdate(EntitiesImportCtx ctx, NotificationTarget notificationTarget, EntityExportData<NotificationTarget> exportData, IdProvider idProvider) {
        ConstraintValidator.validateFields(notificationTarget);
        return notificationTargetService.saveNotificationTarget(ctx.getTenantId(), notificationTarget);
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
    protected void onEntitySaved(User user, NotificationTarget savedEntity, NotificationTarget oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `notificationTarget`：`notificationTarget` 参数。
     * 返回：处理结果。
     */
    @Override
    protected NotificationTarget deepCopy(NotificationTarget notificationTarget) {
        return new NotificationTarget(notificationTarget);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TARGET;
    }

}
