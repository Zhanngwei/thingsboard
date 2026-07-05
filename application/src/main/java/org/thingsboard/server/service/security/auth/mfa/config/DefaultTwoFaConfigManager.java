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
@Service
@RequiredArgsConstructor
public class DefaultTwoFaConfigManager implements TwoFaConfigManager {

    /**
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    private final UserAuthSettingsDao userAuthSettingsDao;
    private final AdminSettingsService adminSettingsService;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AdminSettingsDao adminSettingsDao;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired @Lazy
    private TwoFactorAuthService twoFactorAuthService;

    /**
     * 配置常量，用于统一引用固定值。
     */
    protected static final String TWO_FACTOR_AUTH_SETTINGS_KEY = "twoFaSettings";


    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Optional<AccountTwoFaSettings> getAccountTwoFaSettings(TenantId tenantId, UserId userId) {
        PlatformTwoFaSettings platformTwoFaSettings = getPlatformTwoFaSettings(tenantId, true).orElse(null);
        return Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .map(userAuthSettings -> {
                    AccountTwoFaSettings twoFaSettings = userAuthSettings.getTwoFaSettings();
                    if (twoFaSettings == null) return null;
                    boolean updateNeeded;

                    Map<TwoFaProviderType, TwoFaAccountConfig> configs = twoFaSettings.getConfigs();
                    updateNeeded = configs.keySet().removeIf(providerType -> {
                        return platformTwoFaSettings == null || platformTwoFaSettings.getProviderConfig(providerType).isEmpty();
                    });
                    if (configs.size() == 1 && configs.containsKey(TwoFaProviderType.BACKUP_CODE)) {
                        configs.remove(TwoFaProviderType.BACKUP_CODE);
                        updateNeeded = true;
                    }
                    if (!configs.isEmpty() && configs.values().stream().noneMatch(TwoFaAccountConfig::isUseByDefault)) {
                        configs.values().stream()
                                .filter(config -> config.getProviderType() != TwoFaProviderType.BACKUP_CODE)
                                .findFirst().ifPresent(config -> config.setUseByDefault(true));
                        updateNeeded = true;
                    }

                    if (updateNeeded) {
                        twoFaSettings = saveAccountTwoFaSettings(tenantId, userId, twoFaSettings);
                    }
                    return twoFaSettings;
                });
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `settings`：配置对象。
     * 返回：匹配的数据集合。
     */
    protected AccountTwoFaSettings saveAccountTwoFaSettings(TenantId tenantId, UserId userId, AccountTwoFaSettings settings) {
        UserAuthSettings userAuthSettings = Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .orElseGet(() -> {
                    UserAuthSettings newUserAuthSettings = new UserAuthSettings();
                    newUserAuthSettings.setUserId(userId);
                    return newUserAuthSettings;
                });
        userAuthSettings.setTwoFaSettings(settings);
        settings.getConfigs().values().forEach(accountConfig -> accountConfig.setSerializeHiddenFields(true));
        userAuthSettingsDao.save(tenantId, userAuthSettings);
        settings.getConfigs().values().forEach(accountConfig -> accountConfig.setSerializeHiddenFields(false));
        return settings;
    }


    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `providerType`：类型。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<TwoFaAccountConfig> getTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType) {
        return getAccountTwoFaSettings(tenantId, userId)
                .map(AccountTwoFaSettings::getConfigs)
                .flatMap(configs -> Optional.ofNullable(configs.get(providerType)));
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `accountConfig`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public AccountTwoFaSettings saveTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaAccountConfig accountConfig) {
        getTwoFaProviderConfig(tenantId, accountConfig.getProviderType())
                .orElseThrow(() -> new IllegalArgumentException("2FA provider is not configured"));

        AccountTwoFaSettings settings = getAccountTwoFaSettings(tenantId, userId).orElseGet(() -> {
            AccountTwoFaSettings newSettings = new AccountTwoFaSettings();
            newSettings.setConfigs(new LinkedHashMap<>());
            return newSettings;
        });
        Map<TwoFaProviderType, TwoFaAccountConfig> configs = settings.getConfigs();
        if (configs.isEmpty() && accountConfig.getProviderType() == TwoFaProviderType.BACKUP_CODE) {
            throw new IllegalArgumentException("To use 2FA backup codes you first need to configure at least one provider");
        }
        if (accountConfig.isUseByDefault()) {
            configs.values().forEach(config -> config.setUseByDefault(false));
        }
        configs.put(accountConfig.getProviderType(), accountConfig);
        if (configs.values().stream().noneMatch(TwoFaAccountConfig::isUseByDefault)) {
            configs.values().stream().findFirst().ifPresent(config -> config.setUseByDefault(true));
        }
        return saveAccountTwoFaSettings(tenantId, userId, settings);
    }

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `providerType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public AccountTwoFaSettings deleteTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType) {
        AccountTwoFaSettings settings = getAccountTwoFaSettings(tenantId, userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA not configured"));
        settings.getConfigs().remove(providerType);
        if (settings.getConfigs().size() == 1) {
            settings.getConfigs().remove(TwoFaProviderType.BACKUP_CODE);
        }
        if (!settings.getConfigs().isEmpty() && settings.getConfigs().values().stream()
                .noneMatch(TwoFaAccountConfig::isUseByDefault)) {
            settings.getConfigs().values().stream()
                    .min(Comparator.comparing(TwoFaAccountConfig::getProviderType))
                    .ifPresent(config -> config.setUseByDefault(true));
        }
        return saveAccountTwoFaSettings(tenantId, userId, settings);
    }


    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `providerType`：类型。
     * 返回：可能存在的结果。
     */
    private Optional<TwoFaProviderConfig> getTwoFaProviderConfig(TenantId tenantId, TwoFaProviderType providerType) {
        return getPlatformTwoFaSettings(tenantId, true)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType));
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `sysadminSettingsAsDefault`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public Optional<PlatformTwoFaSettings> getPlatformTwoFaSettings(TenantId tenantId, boolean sysadminSettingsAsDefault) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, TWO_FACTOR_AUTH_SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), PlatformTwoFaSettings.class));
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `twoFactorAuthSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
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

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
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
