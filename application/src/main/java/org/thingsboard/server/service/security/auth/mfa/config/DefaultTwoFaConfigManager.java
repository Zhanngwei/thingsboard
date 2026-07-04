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
package org.thingsboard.server.service.security.auth.mfa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserAuthSettings;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserAuthSettingsDao;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultTwoFaConfigManager` 是ThingsBoard Application 模块中的Spring 配置类型，用于声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 生命周期：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Configuration / Factory。
 */
public class DefaultTwoFaConfigManager implements TwoFaConfigManager {

    /**
     * 字段说明：
     * 1. 保存 `userAuthSettingsDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final UserAuthSettingsDao userAuthSettingsDao;
    private final AdminSettingsService adminSettingsService;
    /**
     * 字段说明：
     * 1. 保存 `adminSettingsDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final AdminSettingsDao adminSettingsDao;
    @Autowired @Lazy
    /**
     * 字段说明：
     * 1. 保存 `twoFactorAuthService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TwoFactorAuthService twoFactorAuthService;

    /**
     * 字段说明：
     * 1. 保存 `TWO_FACTOR_AUTH_SETTINGS_KEY` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String TWO_FACTOR_AUTH_SETTINGS_KEY = "twoFaSettings";


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getAccountTwoFaSettings` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Optional<AccountTwoFaSettings> getAccountTwoFaSettings(TenantId tenantId, UserId userId) {
        PlatformTwoFaSettings platformTwoFaSettings = getPlatformTwoFaSettings(tenantId, true).orElse(null);
        // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
        return Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .map(userAuthSettings -> {
                    AccountTwoFaSettings twoFaSettings = userAuthSettings.getTwoFaSettings();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (twoFaSettings == null) return null;
                    boolean updateNeeded;

                    Map<TwoFaProviderType, TwoFaAccountConfig> configs = twoFaSettings.getConfigs();
                    updateNeeded = configs.keySet().removeIf(providerType -> {
                        return platformTwoFaSettings == null || platformTwoFaSettings.getProviderConfig(providerType).isEmpty();
                    });
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (configs.size() == 1 && configs.containsKey(TwoFaProviderType.BACKUP_CODE)) {
                        configs.remove(TwoFaProviderType.BACKUP_CODE);
                        updateNeeded = true;
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!configs.isEmpty() && configs.values().stream().noneMatch(TwoFaAccountConfig::isUseByDefault)) {
                        configs.values().stream()
                                .filter(config -> config.getProviderType() != TwoFaProviderType.BACKUP_CODE)
                                .findFirst().ifPresent(config -> config.setUseByDefault(true));
                        updateNeeded = true;
                    }

                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (updateNeeded) {
                        twoFaSettings = saveAccountTwoFaSettings(tenantId, userId, twoFaSettings);
                    }
                    return twoFaSettings;
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveAccountTwoFaSettings` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected AccountTwoFaSettings saveAccountTwoFaSettings(TenantId tenantId, UserId userId, AccountTwoFaSettings settings) {
        // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
        UserAuthSettings userAuthSettings = Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .orElseGet(() -> {
                    UserAuthSettings newUserAuthSettings = new UserAuthSettings();
                    newUserAuthSettings.setUserId(userId);
                    return newUserAuthSettings;
                });
        userAuthSettings.setTwoFaSettings(settings);
        settings.getConfigs().values().forEach(accountConfig -> accountConfig.setSerializeHiddenFields(true));
        // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
        userAuthSettingsDao.save(tenantId, userAuthSettings);
        settings.getConfigs().values().forEach(accountConfig -> accountConfig.setSerializeHiddenFields(false));
        return settings;
    }


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getTwoFaAccountConfig` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Optional<TwoFaAccountConfig> getTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType) {
        return getAccountTwoFaSettings(tenantId, userId)
                .map(AccountTwoFaSettings::getConfigs)
                .flatMap(configs -> Optional.ofNullable(configs.get(providerType)));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveTwoFaAccountConfig` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AccountTwoFaSettings saveTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaAccountConfig accountConfig) {
        getTwoFaProviderConfig(tenantId, accountConfig.getProviderType())
                .orElseThrow(() -> new IllegalArgumentException("2FA provider is not configured"));

        AccountTwoFaSettings settings = getAccountTwoFaSettings(tenantId, userId).orElseGet(() -> {
            AccountTwoFaSettings newSettings = new AccountTwoFaSettings();
            newSettings.setConfigs(new LinkedHashMap<>());
            return newSettings;
        });
        Map<TwoFaProviderType, TwoFaAccountConfig> configs = settings.getConfigs();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (configs.isEmpty() && accountConfig.getProviderType() == TwoFaProviderType.BACKUP_CODE) {
            throw new IllegalArgumentException("To use 2FA backup codes you first need to configure at least one provider");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (accountConfig.isUseByDefault()) {
            configs.values().forEach(config -> config.setUseByDefault(false));
        }
        configs.put(accountConfig.getProviderType(), accountConfig);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (configs.values().stream().noneMatch(TwoFaAccountConfig::isUseByDefault)) {
            configs.values().stream().findFirst().ifPresent(config -> config.setUseByDefault(true));
        }
        return saveAccountTwoFaSettings(tenantId, userId, settings);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTwoFaAccountConfig` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AccountTwoFaSettings deleteTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType) {
        AccountTwoFaSettings settings = getAccountTwoFaSettings(tenantId, userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA not configured"));
        settings.getConfigs().remove(providerType);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (settings.getConfigs().size() == 1) {
            settings.getConfigs().remove(TwoFaProviderType.BACKUP_CODE);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!settings.getConfigs().isEmpty() && settings.getConfigs().values().stream()
                .noneMatch(TwoFaAccountConfig::isUseByDefault)) {
            settings.getConfigs().values().stream()
                    .min(Comparator.comparing(TwoFaAccountConfig::getProviderType))
                    .ifPresent(config -> config.setUseByDefault(true));
        }
        return saveAccountTwoFaSettings(tenantId, userId, settings);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `getTwoFaProviderConfig` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Optional<TwoFaProviderConfig> getTwoFaProviderConfig(TenantId tenantId, TwoFaProviderType providerType) {
        return getPlatformTwoFaSettings(tenantId, true)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getPlatformTwoFaSettings` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Optional<PlatformTwoFaSettings> getPlatformTwoFaSettings(TenantId tenantId, boolean sysadminSettingsAsDefault) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, TWO_FACTOR_AUTH_SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), PlatformTwoFaSettings.class));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `savePlatformTwoFaSettings` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PlatformTwoFaSettings savePlatformTwoFaSettings(TenantId tenantId, PlatformTwoFaSettings twoFactorAuthSettings) throws ThingsboardException {
        ConstraintValidator.validateFields(twoFactorAuthSettings);
        for (TwoFaProviderConfig providerConfig : twoFactorAuthSettings.getProviders()) {
            twoFactorAuthService.checkProvider(tenantId, providerConfig.getProviderType());
        }

        AdminSettings settings = Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(tenantId, TWO_FACTOR_AUTH_SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newSettings = new AdminSettings();
                    newSettings.setKey(TWO_FACTOR_AUTH_SETTINGS_KEY);
                    return newSettings;
                });
        settings.setJsonValue(JacksonUtil.valueToTree(twoFactorAuthSettings));
        adminSettingsService.saveAdminSettings(tenantId, settings);
        return twoFactorAuthSettings;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deletePlatformTwoFaSettings` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deletePlatformTwoFaSettings(TenantId tenantId) {
        Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(tenantId, TWO_FACTOR_AUTH_SETTINGS_KEY))
                .ifPresent(adminSettings -> adminSettingsDao.removeById(tenantId, adminSettings.getId().getId()));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTwoFaConfigManager` 在 ThingsBoard Application 模块 中承担Spring 配置类型职责，核心目的是声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 核心流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
