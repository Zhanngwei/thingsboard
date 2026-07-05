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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings.NotificationPref;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedTenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedUserFilter;
import org.thingsboard.server.common.data.notification.targets.platform.AllUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.OriginatorEntityOwnerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.SystemAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserSettingsService;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultNotificationSettingsService` 是 ThingsBoard DAO 模块 中的通知持久化服务类型，用于管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
 * 4. 生命周期：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Scheduler Command。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationSettingsService implements NotificationSettingsService {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AdminSettingsService adminSettingsService;
    private final NotificationTargetService notificationTargetService;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationTemplateService notificationTemplateService;
    private final DefaultNotifications defaultNotifications;
    /**
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    private final UserSettingsService userSettingsService;

    /**
     * 配置常量，用于统一引用固定值。
     */
    private static final String SETTINGS_KEY = "notifications";

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：无。
     */
    @CacheEvict(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public void saveNotificationSettings(TenantId tenantId, NotificationSettings settings) {
        if (!tenantId.isSysTenantId() && settings.getDeliveryMethodsConfigs().containsKey(NotificationDeliveryMethod.MOBILE_APP)) {
            throw new IllegalArgumentException("Mobile settings can only be configured by system administrator");
        }
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setTenantId(tenantId);
                    newAdminSettings.setKey(SETTINGS_KEY);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(tenantId, adminSettings);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Cacheable(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public NotificationSettings findNotificationSettings(TenantId tenantId) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), NotificationSettings.class))
                .orElseGet(() -> {
                    NotificationSettings settings = new NotificationSettings();
                    settings.setDeliveryMethodsConfigs(Collections.emptyMap());
                    return settings;
                });
    }

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @CacheEvict(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public void deleteNotificationSettings(TenantId tenantId) {
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `settings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserNotificationSettings saveUserNotificationSettings(TenantId tenantId, UserId userId, UserNotificationSettings settings) {
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(userId);
        userSettings.setType(UserSettingsType.NOTIFICATIONS);
        userSettings.setSettings(JacksonUtil.valueToTree(settings));
        userSettingsService.saveUserSettings(tenantId, userSettings);
        return formatUserNotificationSettings(settings);
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `format`：`format` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserNotificationSettings getUserNotificationSettings(TenantId tenantId, UserId userId, boolean format) {
        UserSettings userSettings = userSettingsService.findUserSettings(tenantId, userId, UserSettingsType.NOTIFICATIONS);
        UserNotificationSettings settings = null;
        if (userSettings != null) {
            try {
                settings = JacksonUtil.treeToValue(userSettings.getSettings(), UserNotificationSettings.class);
            } catch (Exception e) {
                log.warn("Failed to parse notification settings for user {}", userId, e);
            }
        }
        if (settings == null) {
            settings = UserNotificationSettings.DEFAULT;
        }
        if (format) {
            settings = formatUserNotificationSettings(settings);
        }
        return settings;
    }

    /**
     * 功能：执行 `formatUserNotificationSettings` 对应的处理。
     * 参数：
     * - `settings`：配置对象。
     * 返回：匹配的数据集合。
     */
    private UserNotificationSettings formatUserNotificationSettings(UserNotificationSettings settings) {
        Map<NotificationType, NotificationPref> prefs = new EnumMap<>(NotificationType.class);
        if (settings != null) {
            prefs.putAll(settings.getPrefs());
        }
        NotificationPref defaultPref = NotificationPref.createDefault();
        for (NotificationType notificationType : NotificationType.values()) {
            NotificationPref pref = prefs.get(notificationType);
            if (pref == null) {
                prefs.put(notificationType, defaultPref);
            } else {
                var enabledDeliveryMethods = new LinkedHashMap<>(pref.getEnabledDeliveryMethods());
                // in case a new delivery method was added to the platform
                UserNotificationSettings.deliveryMethods.forEach(deliveryMethod -> {
                    enabledDeliveryMethods.putIfAbsent(deliveryMethod, true);
                });
                pref.setEnabledDeliveryMethods(enabledDeliveryMethods);
            }
        }
        return new UserNotificationSettings(prefs);
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // so that parent transaction is not aborted on method failure
    @Override
    public void createDefaultNotificationConfigs(TenantId tenantId) {
        NotificationTarget allUsers = createTarget(tenantId, "All users", new AllUsersFilter(),
                tenantId.isSysTenantId() ? "All platform users" : "All users in scope of the tenant");
        NotificationTarget tenantAdmins = createTarget(tenantId, "Tenant administrators", new TenantAdministratorsFilter(),
                tenantId.isSysTenantId() ? "All tenant administrators" : "Tenant administrators");

        defaultNotifications.create(tenantId, DefaultNotifications.maintenanceWork);

        if (tenantId.isSysTenantId()) {
            NotificationTarget sysAdmins = createTarget(tenantId, "System administrators", new SystemAdministratorsFilter(), "All system administrators");
            NotificationTarget affectedTenantAdmins = createTarget(tenantId, "Affected tenant's administrators", new AffectedTenantAdministratorsFilter(), "");

            defaultNotifications.create(tenantId, DefaultNotifications.entitiesLimitForSysadmin, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.entitiesLimitForTenant, affectedTenantAdmins.getId());

            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureWarningForSysadmin, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureWarningForTenant, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureDisabledForSysadmin, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureDisabledForTenant, affectedTenantAdmins.getId());

            defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimits, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.exceededPerEntityRateLimits, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimitsForSysadmin, sysAdmins.getId());

            defaultNotifications.create(tenantId, DefaultNotifications.newPlatformVersion, sysAdmins.getId());
            return;
        }

        NotificationTarget originatorEntityOwnerUsers = createTarget(tenantId, "Users of the entity owner", new OriginatorEntityOwnerUsersFilter(),
                "In case trigger entity (e.g. created device or alarm) is owned by customer, then recipients are this customer's users, otherwise tenant admins");
        NotificationTarget affectedUser = createTarget(tenantId, "Affected user", new AffectedUserFilter(),
                "If rule trigger is an action that affects some user (e.g. alarm assigned to user) - this user");

        defaultNotifications.create(tenantId, DefaultNotifications.newAlarm, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.alarmUpdate, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.entityAction, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.deviceActivity, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.alarmComment, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.alarmAssignment, affectedUser.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.ruleEngineComponentLifecycleFailure, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.edgeConnection, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.edgeCommunicationFailures, tenantAdmins.getId());
    }

    /**
     * 功能：更新通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void updateDefaultNotificationConfigs(TenantId tenantId) {
        if (tenantId.isSysTenantId()) {
            if (notificationTemplateService.findNotificationTemplatesByTenantIdAndNotificationTypes(tenantId,
                    List.of(NotificationType.RATE_LIMITS), new PageLink(1)).getTotalElements() > 0) {
                return;
            }

            NotificationTarget sysAdmins = notificationTargetService.findNotificationTargetsByTenantIdAndUsersFilterType(tenantId, UsersFilterType.SYSTEM_ADMINISTRATORS).stream()
                    .findFirst().orElseGet(() -> createTarget(tenantId, "System administrators", new SystemAdministratorsFilter(), "All system administrators"));
            NotificationTarget affectedTenantAdmins = notificationTargetService.findNotificationTargetsByTenantIdAndUsersFilterType(tenantId, UsersFilterType.AFFECTED_TENANT_ADMINISTRATORS).stream()
                    .findFirst().orElseGet(() -> createTarget(tenantId, "Affected tenant's administrators", new AffectedTenantAdministratorsFilter(), ""));

            defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimits, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.exceededPerEntityRateLimits, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimitsForSysadmin, sysAdmins.getId());
        } else {
            var requiredNotificationTypes = List.of(NotificationType.EDGE_CONNECTION, NotificationType.EDGE_COMMUNICATION_FAILURE);
            var existingNotificationTypes = notificationTemplateService.findNotificationTemplatesByTenantIdAndNotificationTypes(
                            tenantId, requiredNotificationTypes, new PageLink(2))
                    .getData()
                    .stream()
                    .map(NotificationTemplate::getNotificationType)
                    .collect(Collectors.toSet());

            if (existingNotificationTypes.containsAll(requiredNotificationTypes)) {
                return;
            }

            NotificationTarget tenantAdmins = notificationTargetService.findNotificationTargetsByTenantIdAndUsersFilterType(tenantId, UsersFilterType.TENANT_ADMINISTRATORS)
                    .stream()
                    .findFirst()
                    .orElseGet(() -> createTarget(tenantId, "Tenant administrators", new TenantAdministratorsFilter(), "Tenant administrators"));

            for (NotificationType type : requiredNotificationTypes) {
                if (!existingNotificationTypes.contains(type)) {
                    switch (type) {
                        case EDGE_CONNECTION:
                            defaultNotifications.create(tenantId, DefaultNotifications.edgeConnection, tenantAdmins.getId());
                            break;
                        case EDGE_COMMUNICATION_FAILURE:
                            defaultNotifications.create(tenantId, DefaultNotifications.edgeCommunicationFailures, tenantAdmins.getId());
                            break;
                    }
                }
            }
        }
    }

    /**
     * 功能：保存或创建目标对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `filter`：`filter` 参数。
     * - `description`：`description` 参数。
     * 返回：处理结果。
     */
    private NotificationTarget createTarget(TenantId tenantId, String name, UsersFilter filter, String description) {
        NotificationTarget target = new NotificationTarget();
        target.setTenantId(tenantId);
        target.setName(name);

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(filter);
        targetConfig.setDescription(description);
        target.setConfiguration(targetConfig);
        return notificationTargetService.saveNotificationTarget(tenantId, target);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNotificationSettingsService` 在 ThingsBoard DAO 模块 中承担通知持久化服务类型职责，核心目的是管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
 * 2. 核心流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
 * 3. 关键依赖：主要依赖或协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
