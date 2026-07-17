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
 * 1. `DefaultTwoFaConfigManager` 是 ThingsBoard Application 中负责 `Two Fa` 的协调管理组件。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TwoFaConfigManager`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
