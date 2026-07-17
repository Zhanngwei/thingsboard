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
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.NotificationTargetEntity;
import org.thingsboard.server.dao.notification.NotificationTargetDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `JpaNotificationTargetDao` 是 ThingsBoard DAO 中负责通知存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`NotificationTargetDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationTargetDao extends JpaAbstractDao<NotificationTargetEntity, NotificationTarget> implements NotificationTargetDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final NotificationTargetRepository notificationTargetRepository;

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTarget> findByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTargetRepository.findByTenantIdAndSearchText(tenantId.getId(),
                pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTarget> findByTenantIdAndSupportedNotificationTypeAndPageLink(TenantId tenantId, NotificationType notificationType, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTargetRepository.findByTenantIdAndSearchTextAndUsersFilterTypeIfPresent(tenantId.getId(),
                pageLink.getTextSearch(),
                Arrays.stream(UsersFilterType.values())
                        .filter(type -> notificationType != NotificationType.GENERAL || !type.isForRules())
                        .map(Enum::name).collect(Collectors.toList()),
                DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ids`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationTarget> findByTenantIdAndIds(TenantId tenantId, List<NotificationTargetId> ids) {
        return DaoUtil.convertDataList(notificationTargetRepository.findByTenantIdAndIdIn(tenantId.getId(), DaoUtil.toUUIDs(ids)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `filterType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationTarget> findByTenantIdAndUsersFilterType(TenantId tenantId, UsersFilterType filterType) {
        return DaoUtil.convertDataList(notificationTargetRepository.findByTenantIdAndSearchTextAndUsersFilterTypeIfPresent(tenantId.getId(), null,
                List.of(filterType.name()), DaoUtil.toPageable(new PageLink(Integer.MAX_VALUE))).getContent());
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void removeByTenantId(TenantId tenantId) {
        notificationTargetRepository.deleteByTenantId(tenantId.getId());
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public Long countByTenantId(TenantId tenantId) {
        return notificationTargetRepository.countByTenantId(tenantId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationTarget findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(notificationTargetRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public NotificationTarget findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(notificationTargetRepository.findByTenantIdAndName(tenantId, name));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTarget> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTargetRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationTargetId getExternalIdByInternal(NotificationTargetId internalId) {
        return DaoUtil.toEntityId(notificationTargetRepository.getExternalIdByInternal(internalId.getId()), NotificationTargetId::new);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<NotificationTargetEntity> getEntityClass() {
        return NotificationTargetEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<NotificationTargetEntity, UUID> getRepository() {
        return notificationTargetRepository;
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
