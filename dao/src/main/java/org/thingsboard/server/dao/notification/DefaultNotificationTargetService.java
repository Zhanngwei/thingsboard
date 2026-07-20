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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * 中文说明：
 * 1. `DefaultNotificationTargetService` 是 ThingsBoard DAO 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`NotificationTargetService`、`EntityDaoService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationTargetService extends AbstractEntityService implements NotificationTargetService, EntityDaoService {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final NotificationTargetDao notificationTargetDao;
    private final NotificationRequestDao notificationRequestDao;
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final NotificationRuleDao notificationRuleDao;
    private final UserService userService;

    /**
     * 功能：保存或创建目标对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationTarget`：`notificationTarget` 参数。
     * 返回：处理结果。
     */
    @Override
    public NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget) {
        try {
            return notificationTargetDao.saveAndFlush(tenantId, notificationTarget);
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "uq_notification_target_name", "Recipients group with such name already exists"
            ));
            throw e;
        }
    }

    /**
     * 功能：获取目标对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        return notificationTargetDao.findById(tenantId, id.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantIdAndSupportedNotificationType(TenantId tenantId, NotificationType notificationType, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndSupportedNotificationTypeAndPageLink(tenantId, notificationType, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ids`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationTarget> findNotificationTargetsByTenantIdAndIds(TenantId tenantId, List<NotificationTargetId> ids) {
        return notificationTargetDao.findByTenantIdAndIds(tenantId, ids);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `filterType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationTarget> findNotificationTargetsByTenantIdAndUsersFilterType(TenantId tenantId, UsersFilterType filterType) {
        return notificationTargetDao.findByTenantIdAndUsersFilterType(tenantId, filterType);
    }

    /**
     * 功能：获取目标对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `targetId`：目标对象ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findRecipientsForNotificationTarget(TenantId tenantId, CustomerId customerId, NotificationTargetId targetId, PageLink pageLink) {
        NotificationTarget notificationTarget = findNotificationTargetById(tenantId, targetId);
        Objects.requireNonNull(notificationTarget, "Notification target [" + targetId + "] not found");
        NotificationTargetConfig configuration = notificationTarget.getConfiguration();
        return findRecipientsForNotificationTargetConfig(tenantId, (PlatformUsersNotificationTargetConfig) configuration, pageLink);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `targetConfig`：配置对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findRecipientsForNotificationTargetConfig(TenantId tenantId, PlatformUsersNotificationTargetConfig targetConfig, PageLink pageLink) {
        UsersFilter usersFilter = targetConfig.getUsersFilter();
        switch (usersFilter.getType()) {
            case USER_LIST: {
                List<User> users = ((UserListFilter) usersFilter).getUsersIds().stream()
                        .limit(pageLink.getPageSize())
                        .map(UserId::new).map(userId -> userService.findUserById(tenantId, userId))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                return new PageData<>(users, 1, users.size(), false);
            }
            case CUSTOMER_USERS: {
                if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    throw new IllegalArgumentException("Customer users target is not supported for system administrator");
                }
                CustomerUsersFilter filter = (CustomerUsersFilter) usersFilter;
                return userService.findCustomerUsers(tenantId, new CustomerId(filter.getCustomerId()), pageLink);
            }
            case TENANT_ADMINISTRATORS: {
                TenantAdministratorsFilter filter = (TenantAdministratorsFilter) usersFilter;
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return userService.findTenantAdmins(tenantId, pageLink);
                } else {
                    if (isNotEmpty(filter.getTenantsIds())) {
                        return userService.findTenantAdminsByTenantsIds(filter.getTenantsIds().stream()
                                .map(TenantId::fromUUID).collect(Collectors.toList()), pageLink);
                    } else if (isNotEmpty(filter.getTenantProfilesIds())) {
                        return userService.findTenantAdminsByTenantProfilesIds(filter.getTenantProfilesIds().stream()
                                .map(TenantProfileId::new).collect(Collectors.toList()), pageLink);
                    } else {
                        return userService.findAllTenantAdmins(pageLink);
                    }
                }
            }
            case SYSTEM_ADMINISTRATORS:
                return userService.findSysAdmins(pageLink);
            case ALL_USERS: {
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return userService.findUsersByTenantId(tenantId, pageLink);
                } else {
                    return userService.findAllUsers(pageLink);
                }
            }
            default:
                throw new IllegalArgumentException("Recipient type not supported");
        }
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `targetConfig`：配置对象。
     * - `info`：`info` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findRecipientsForRuleNotificationTargetConfig(TenantId tenantId, PlatformUsersNotificationTargetConfig targetConfig, RuleOriginatedNotificationInfo info, PageLink pageLink) {
        switch (targetConfig.getUsersFilter().getType()) {
            case ORIGINATOR_ENTITY_OWNER_USERS:
                CustomerId customerId = info.getAffectedCustomerId();
                if (customerId != null && !customerId.isNullUid()) {
                    return userService.findCustomerUsers(tenantId, customerId, pageLink);
                } else {
                    return userService.findTenantAdmins(tenantId, pageLink);
                }
            case AFFECTED_USER:
                UserId userId = info.getAffectedUserId();
                if (userId != null) {
                    return new PageData<>(List.of(userService.findUserById(tenantId, userId)), 1, 1, false);
                }
            case AFFECTED_TENANT_ADMINISTRATORS:
                TenantId affectedTenantId = info.getAffectedTenantId();
                if (affectedTenantId == null) {
                    affectedTenantId = tenantId;
                }
                if (!affectedTenantId.isNullUid()) {
                    return userService.findTenantAdmins(affectedTenantId, pageLink);
                }
                break;
            default:
                throw new IllegalArgumentException("Recipient type not supported");
        }
        return new PageData<>();
    }

    /**
     * 功能：删除或清理目标对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public void deleteNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        if (notificationRequestDao.existsByTenantIdAndStatusAndTargetId(tenantId, NotificationRequestStatus.SCHEDULED, id)) {
            throw new IllegalArgumentException("Recipients group is referenced by scheduled notification request");
        }
        if (notificationRuleDao.existsByTenantIdAndTargetId(tenantId, id)) {
            throw new IllegalArgumentException("Recipients group is being used in notification rule");
        }
        notificationTargetDao.removeById(tenantId, id.getId());
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteNotificationTargetsByTenantId(TenantId tenantId) {
        notificationTargetDao.removeByTenantId(tenantId);
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public long countNotificationTargetsByTenantId(TenantId tenantId) {
        return notificationTargetDao.countByTenantId(tenantId);
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
        return Optional.ofNullable(findNotificationTargetById(tenantId, new NotificationTargetId(entityId.getId())));
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
        deleteNotificationTargetById(tenantId, (NotificationTargetId) id);
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
