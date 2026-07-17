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
package org.thingsboard.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.targets.platform.SystemAdministratorsFilter;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `DefaultJwtSettingsService` 是 ThingsBoard Application 中负责 `Jwt` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `JwtSettingsService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJwtSettingsService implements JwtSettingsService {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AdminSettingsService adminSettingsService;
    private final Optional<TbClusterService> tbClusterService;
    /**
     * 通知，表示当前对象的对应属性。
     */
    private final Optional<NotificationCenter> notificationCenter;
    private final JwtSettingsValidator jwtSettingsValidator;

    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    @Value("${security.jwt.tokenExpirationTime:9000}")
    private Integer tokenExpirationTime;
    /**
     * 刷新令牌过期时间，用于控制时间范围或等待时长。
     */
    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private Integer refreshTokenExpTime;
    /**
     * 令牌，用于认证或安全校验。
     */
    @Value("${security.jwt.tokenIssuer:thingsboard.io}")
    private String tokenIssuer;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Value("${security.jwt.tokenSigningKey:thingsboardDefaultSigningKey}")
    private String tokenSigningKey;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private volatile JwtSettings jwtSettings = null; //lazy init

    /**
     * Create JWT admin settings is intended to be called from Install scripts only
     */
    /**
     * 功能：保存或创建配置。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void createRandomJwtSettings() {
        if (getJwtSettingsFromDb() == null) {
            log.info("Creating JWT admin settings...");
            this.jwtSettings = getJwtSettingsFromYml();
            if (isSigningKeyDefault(jwtSettings)) {
                this.jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                        RandomStringUtils.randomAlphanumeric(64).getBytes(StandardCharsets.UTF_8)));
            }
            saveJwtSettings(jwtSettings);
        } else {
            log.info("Skip creating JWT admin settings because they already exist.");
        }
    }

    /**
     * Create JWT admin settings is intended to be called from Upgrade scripts only
     */
    /**
     * 功能：保存或创建配置。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void saveLegacyYmlSettings() {
        log.info("Saving legacy JWT admin settings from YML...");
        if (getJwtSettingsFromDb() == null) {
            saveJwtSettings(getJwtSettingsFromYml());
        }
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `jwtSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public JwtSettings saveJwtSettings(JwtSettings jwtSettings) {
        jwtSettingsValidator.validate(jwtSettings);
        final AdminSettings adminJwtSettings = mapJwtToAdminSettings(jwtSettings);
        final AdminSettings existedSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        if (existedSettings != null) {
            adminJwtSettings.setId(existedSettings.getId());
        }

        log.info("Saving new JWT admin settings. From this moment, the JWT parameters from YAML and ENV will be ignored");
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminJwtSettings);

        tbClusterService.ifPresent(cs -> cs.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, ComponentLifecycleEvent.UPDATED));
        return reloadJwtSettings();
    }

    /**
     * 功能：更新配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public JwtSettings reloadJwtSettings() {
        log.trace("Executing reloadJwtSettings");
        return getJwtSettings(true);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public JwtSettings getJwtSettings() {
        log.trace("Executing getJwtSettings");
        return getJwtSettings(false);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `forceReload`：`forceReload` 参数。
     * 返回：匹配的数据集合。
     */
    public JwtSettings getJwtSettings(boolean forceReload) {
        if (this.jwtSettings == null || forceReload) {
            synchronized (this) {
                if (this.jwtSettings == null || forceReload) {
                    JwtSettings result = getJwtSettingsFromDb();
                    if (result == null) {
                        result = getJwtSettingsFromYml();
                        log.warn("Loading the JWT settings from YML since there are no settings in DB. Looks like the upgrade script was not applied.");
                    }
                    if (isSigningKeyDefault(result)) {
                        log.warn("WARNING: The platform is configured to use default JWT Signing Key. " +
                                "This is a security issue that needs to be resolved. Please change the JWT Signing Key using the Web UI. " +
                                "Navigate to \"System settings -> Security settings\" while logged in as a System Administrator.");
                        notificationCenter.ifPresent(notificationCenter -> {
                            notificationCenter.sendGeneralWebNotification(TenantId.SYS_TENANT_ID, new SystemAdministratorsFilter(), DefaultNotifications.jwtSigningKeyIssue.toTemplate());
                        });
                    }
                    this.jwtSettings = result;
                }
            }
        }
        return this.jwtSettings;
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private JwtSettings getJwtSettingsFromYml() {
        return new JwtSettings(this.tokenExpirationTime, this.refreshTokenExpTime, this.tokenIssuer, this.tokenSigningKey);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private JwtSettings getJwtSettingsFromDb() {
        AdminSettings adminJwtSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        return adminJwtSettings != null ? mapAdminToJwtSettings(adminJwtSettings) : null;
    }

    /**
     * 功能：转换配置。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    private JwtSettings mapAdminToJwtSettings(AdminSettings adminSettings) {
        Objects.requireNonNull(adminSettings, "adminSettings for JWT is null");
        return JacksonUtil.treeToValue(adminSettings.getJsonValue(), JwtSettings.class);
    }

    /**
     * 功能：转换配置。
     * 参数：
     * - `jwtSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    private AdminSettings mapJwtToAdminSettings(JwtSettings jwtSettings) {
        Objects.requireNonNull(jwtSettings, "jwtSettings is null");
        AdminSettings adminJwtSettings = new AdminSettings();
        adminJwtSettings.setTenantId(TenantId.SYS_TENANT_ID);
        adminJwtSettings.setKey(ADMIN_SETTINGS_JWT_KEY);
        adminJwtSettings.setJsonValue(JacksonUtil.valueToTree(jwtSettings));
        return adminJwtSettings;
    }

    /**
     * 功能：判断键。
     * 参数：
     * - `settings`：配置对象。
     * 返回：判断结果。
     */
    private boolean isSigningKeyDefault(JwtSettings settings) {
        return TOKEN_SIGNING_KEY_DEFAULT.equals(settings.getTokenSigningKey());
    }

}
