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
package org.thingsboard.server.dao.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `DefaultNotificationTemplateService` 是 ThingsBoard DAO 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`NotificationTemplateService`、`EntityDaoService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@RequiredArgsConstructor
public class DefaultNotificationTemplateService extends AbstractEntityService implements NotificationTemplateService, EntityDaoService {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final NotificationTemplateDao notificationTemplateDao;
    private final NotificationRequestDao notificationRequestDao;

    /**
     * 功能：获取通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationTemplate findNotificationTemplateById(TenantId tenantId, NotificationTemplateId id) {
        return notificationTemplateDao.findById(tenantId, id.getId());
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * 返回：处理结果。
     */
    @Override
    public NotificationTemplate saveNotificationTemplate(TenantId tenantId, NotificationTemplate notificationTemplate) {
        if (notificationTemplate.getId() != null) {
            NotificationTemplate oldNotificationTemplate = findNotificationTemplateById(tenantId, notificationTemplate.getId());
            if (notificationTemplate.getNotificationType() != oldNotificationTemplate.getNotificationType()) {
                throw new IllegalArgumentException("Notification type cannot be updated");
            }
        }
        try {
            return notificationTemplateDao.saveAndFlush(tenantId, notificationTemplate);
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "uq_notification_template_name", "Notification template with such name already exists"
            ));
            throw e;
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationTypes`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTemplate> findNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, List<NotificationType> notificationTypes, PageLink pageLink) {
        return notificationTemplateDao.findByTenantIdAndNotificationTypesAndPageLink(tenantId, notificationTypes, pageLink);
    }

    /**
     * 功能：删除或清理通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public void deleteNotificationTemplateById(TenantId tenantId, NotificationTemplateId id) {
        if (notificationRequestDao.existsByTenantIdAndStatusAndTemplateId(tenantId, NotificationRequestStatus.SCHEDULED, id)) {
            throw new IllegalArgumentException("Notification template is referenced by scheduled notification request");
        }
        try {
            notificationTemplateDao.removeById(tenantId, id.getId());
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "fk_notification_rule_template_id", "Notification template is referenced by notification rule"
            ));
            throw e;
        }
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteNotificationTemplatesByTenantId(TenantId tenantId) {
        notificationTemplateDao.removeByTenantId(tenantId);
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
        return Optional.ofNullable(findNotificationTemplateById(tenantId, new NotificationTemplateId(entityId.getId())));
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
        deleteNotificationTemplateById(tenantId, (NotificationTemplateId) id);
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
