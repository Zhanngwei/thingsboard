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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

/**
 * 中文说明：
 * 1. `NotificationTargetExportService` 是 ThingsBoard Application 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityExportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
public class NotificationTargetExportService extends BaseEntityExportService<NotificationTargetId, NotificationTarget, EntityExportData<NotificationTarget>> {

    /**
     * 功能：更新`Related Entities`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationTarget`：`notificationTarget` 参数。
     * - `exportData`：待处理数据。
     * 返回：无。
     */
    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, NotificationTarget notificationTarget, EntityExportData<NotificationTarget> exportData) {
        if (notificationTarget.getConfiguration().getType() == NotificationTargetType.PLATFORM_USERS) {
            UsersFilter usersFilter = ((PlatformUsersNotificationTargetConfig) notificationTarget.getConfiguration()).getUsersFilter();
            switch (usersFilter.getType()) {
                case CUSTOMER_USERS:
                    CustomerUsersFilter customerUsersFilter = (CustomerUsersFilter) usersFilter;
                    customerUsersFilter.setCustomerId(getExternalIdOrElseInternal(ctx, new CustomerId(customerUsersFilter.getCustomerId())).getId());
                    break;
                case USER_LIST:
                    // users list stays as is and is replaced with current user id on import (due to user entities not being supported by VC)
                    break;
            }
        }
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.NOTIFICATION_TARGET);
    }

}
