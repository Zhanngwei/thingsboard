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
package org.thingsboard.server.dao.sql.notification;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.NotificationTemplateEntity;
import org.thingsboard.server.dao.notification.NotificationTemplateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `JpaNotificationTemplateDao` 是 ThingsBoard DAO 中负责通知存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`NotificationTemplateDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationTemplateDao extends JpaAbstractDao<NotificationTemplateEntity, NotificationTemplate> implements NotificationTemplateDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final NotificationTemplateRepository notificationTemplateRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<NotificationTemplateEntity> getEntityClass() {
        return NotificationTemplateEntity.class;
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationTypes`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTemplate> findByTenantIdAndNotificationTypesAndPageLink(TenantId tenantId, List<NotificationType> notificationTypes, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTemplateRepository.findByTenantIdAndNotificationTypesAndSearchText(tenantId.getId(),
                notificationTypes, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void removeByTenantId(TenantId tenantId) {
        notificationTemplateRepository.deleteByTenantId(tenantId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationTemplate findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(notificationTemplateRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public NotificationTemplate findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(notificationTemplateRepository.findByTenantIdAndName(tenantId, name));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTemplate> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTemplateRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationTemplateId getExternalIdByInternal(NotificationTemplateId internalId) {
        return DaoUtil.toEntityId(notificationTemplateRepository.getExternalIdByInternal(internalId.getId()), NotificationTemplateId::new);
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<NotificationTemplateEntity, UUID> getRepository() {
        return notificationTemplateRepository;
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
