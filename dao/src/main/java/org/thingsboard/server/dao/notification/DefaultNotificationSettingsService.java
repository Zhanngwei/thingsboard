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

@Service
@RequiredArgsConstructor
@Slf4j
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
public class DefaultNotificationSettingsService implements NotificationSettingsService {

    /**
     * 字段说明：
     * 1. 保存 `adminSettingsService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final AdminSettingsService adminSettingsService;
    private final NotificationTargetService notificationTargetService;
    /**
     * 字段说明：
     * 1. 保存 `notificationTemplateService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final NotificationTemplateService notificationTemplateService;
    private final DefaultNotifications defaultNotifications;
    /**
     * 字段说明：
     * 1. 保存 `userSettingsService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final UserSettingsService userSettingsService;

    /**
     * 字段说明：
     * 1. 保存 `SETTINGS_KEY` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String SETTINGS_KEY = "notifications";

    @CacheEvict(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveNotificationSettings` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void saveNotificationSettings(TenantId tenantId, NotificationSettings settings) {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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

    @Cacheable(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findNotificationSettings` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public NotificationSettings findNotificationSettings(TenantId tenantId) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), NotificationSettings.class))
                .orElseGet(() -> {
                    NotificationSettings settings = new NotificationSettings();
                    settings.setDeliveryMethodsConfigs(Collections.emptyMap());
                    return settings;
                });
    }

    @CacheEvict(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteNotificationSettings` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void deleteNotificationSettings(TenantId tenantId) {
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveUserNotificationSettings` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public UserNotificationSettings saveUserNotificationSettings(TenantId tenantId, UserId userId, UserNotificationSettings settings) {
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(userId);
        userSettings.setType(UserSettingsType.NOTIFICATIONS);
        userSettings.setSettings(JacksonUtil.valueToTree(settings));
        userSettingsService.saveUserSettings(tenantId, userSettings);
        return formatUserNotificationSettings(settings);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getUserNotificationSettings` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public UserNotificationSettings getUserNotificationSettings(TenantId tenantId, UserId userId, boolean format) {
        UserSettings userSettings = userSettingsService.findUserSettings(tenantId, userId, UserSettingsType.NOTIFICATIONS);
        UserNotificationSettings settings = null;
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (userSettings != null) {
            try {
                settings = JacksonUtil.treeToValue(userSettings.getSettings(), UserNotificationSettings.class);
            // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
            } catch (Exception e) {
                log.warn("Failed to parse notification settings for user {}", userId, e);
            }
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (settings == null) {
            settings = UserNotificationSettings.DEFAULT;
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (format) {
            settings = formatUserNotificationSettings(settings);
        }
        return settings;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `formatUserNotificationSettings` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private UserNotificationSettings formatUserNotificationSettings(UserNotificationSettings settings) {
        Map<NotificationType, NotificationPref> prefs = new EnumMap<>(NotificationType.class);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (settings != null) {
            prefs.putAll(settings.getPrefs());
        }
        NotificationPref defaultPref = NotificationPref.createDefault();
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (NotificationType notificationType : NotificationType.values()) {
            NotificationPref pref = prefs.get(notificationType);
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED) // so that parent transaction is not aborted on method failure
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createDefaultNotificationConfigs` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void createDefaultNotificationConfigs(TenantId tenantId) {
        NotificationTarget allUsers = createTarget(tenantId, "All users", new AllUsersFilter(),
                tenantId.isSysTenantId() ? "All platform users" : "All users in scope of the tenant");
        NotificationTarget tenantAdmins = createTarget(tenantId, "Tenant administrators", new TenantAdministratorsFilter(),
                tenantId.isSysTenantId() ? "All tenant administrators" : "Tenant administrators");

        defaultNotifications.create(tenantId, DefaultNotifications.maintenanceWork);

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateDefaultNotificationConfigs` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void updateDefaultNotificationConfigs(TenantId tenantId) {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (tenantId.isSysTenantId()) {
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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

            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (existingNotificationTypes.containsAll(requiredNotificationTypes)) {
                return;
            }

            NotificationTarget tenantAdmins = notificationTargetService.findNotificationTargetsByTenantIdAndUsersFilterType(tenantId, UsersFilterType.TENANT_ADMINISTRATORS)
                    .stream()
                    .findFirst()
                    .orElseGet(() -> createTarget(tenantId, "Tenant administrators", new TenantAdministratorsFilter(), "Tenant administrators"));

            // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
            for (NotificationType type : requiredNotificationTypes) {
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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
     * 方法说明：
     * 1. 职责：执行 `createTarget` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
