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
package org.thingsboard.server.service.security.auth.mfa;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTwoFactorAuthService` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Service
@RequiredArgsConstructor
@TbCoreComponent
public class DefaultTwoFactorAuthService implements TwoFactorAuthService {

    /**
     * 配置，负责处理对应任务或消息。
     */
    private final TwoFaConfigManager configManager;
    private final SystemSecurityService systemSecurityService;
    /**
     * 用户，提供当前类调用的业务操作。
     */
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final Map<TwoFaProviderType, TwoFaProvider<TwoFaProviderConfig, TwoFaAccountConfig>> providers = new EnumMap<>(TwoFaProviderType.class);

    private static final ThingsboardException ACCOUNT_NOT_CONFIGURED_ERROR = new ThingsboardException("2FA is not configured for account", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    private static final ThingsboardException PROVIDER_NOT_CONFIGURED_ERROR = new ThingsboardException("2FA provider is not configured", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    private static final ThingsboardException PROVIDER_NOT_AVAILABLE_ERROR = new ThingsboardException("2FA provider is not available", ThingsboardErrorCode.GENERAL);
    private static final ThingsboardException TOO_MANY_REQUESTS_ERROR = new ThingsboardException("Too many requests", ThingsboardErrorCode.TOO_MANY_REQUESTS);

    /**
     * 功能：判断`Two Fa Enabled`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：判断结果。
     */
    @Override
    public boolean isTwoFaEnabled(TenantId tenantId, UserId userId) {
        return configManager.getAccountTwoFaSettings(tenantId, userId)
                .map(settings -> !settings.getConfigs().isEmpty())
                .orElse(false);
    }

    /**
     * 功能：校验提供者。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `providerType`：类型。
     * 返回：无。
     */
    @Override
    public void checkProvider(TenantId tenantId, TwoFaProviderType providerType) throws ThingsboardException {
        getTwoFaProvider(providerType).check(tenantId);
    }


    /**
     * 功能：执行 `prepareVerificationCode` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerType`：类型。
     * - `checkLimits`：数量限制。
     * 返回：无。
     */
    @Override
    public void prepareVerificationCode(SecurityUser user, TwoFaProviderType providerType, boolean checkLimits) throws Exception {
        TwoFaAccountConfig accountConfig = configManager.getTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType)
                .orElseThrow(() -> ACCOUNT_NOT_CONFIGURED_ERROR);
        prepareVerificationCode(user, accountConfig, checkLimits);
    }

    /**
     * 功能：执行 `prepareVerificationCode` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `accountConfig`：配置对象。
     * - `checkLimits`：数量限制。
     * 返回：无。
     */
    @Override
    public void prepareVerificationCode(SecurityUser user, TwoFaAccountConfig accountConfig, boolean checkLimits) throws ThingsboardException {
        PlatformTwoFaSettings twoFaSettings = configManager.getPlatformTwoFaSettings(user.getTenantId(), true)
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        if (checkLimits) {
            Integer minVerificationCodeSendPeriod = twoFaSettings.getMinVerificationCodeSendPeriod();
            String rateLimit = null;
            if (minVerificationCodeSendPeriod != null && minVerificationCodeSendPeriod > 4) {
                rateLimit = "1:" + minVerificationCodeSendPeriod;
            }
            if (!rateLimitService.checkRateLimit(LimitedApi.TWO_FA_VERIFICATION_CODE_SEND,
                    Pair.of(user.getId(), accountConfig.getProviderType()), rateLimit)) {
                throw TOO_MANY_REQUESTS_ERROR;
            }
        }

        TwoFaProviderConfig providerConfig = twoFaSettings.getProviderConfig(accountConfig.getProviderType())
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        getTwoFaProvider(accountConfig.getProviderType()).prepareVerificationCode(user, providerConfig, accountConfig);
    }


    /**
     * 功能：校验编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerType`：类型。
     * - `verificationCode`：`verificationCode` 参数。
     * - `checkLimits`：数量限制。
     * 返回：判断结果。
     */
    @Override
    public boolean checkVerificationCode(SecurityUser user, TwoFaProviderType providerType, String verificationCode, boolean checkLimits) throws ThingsboardException {
        TwoFaAccountConfig accountConfig = configManager.getTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType)
                .orElseThrow(() -> ACCOUNT_NOT_CONFIGURED_ERROR);
        return checkVerificationCode(user, verificationCode, accountConfig, checkLimits);
    }

    /**
     * 功能：校验编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `verificationCode`：`verificationCode` 参数。
     * - `accountConfig`：配置对象。
     * - `checkLimits`：数量限制。
     * 返回：判断结果。
     */
    @Override
    public boolean checkVerificationCode(SecurityUser user, String verificationCode, TwoFaAccountConfig accountConfig, boolean checkLimits) throws ThingsboardException {
        if (!userService.findUserCredentialsByUserId(user.getTenantId(), user.getId()).isEnabled()) {
            throw new ThingsboardException("User is disabled", ThingsboardErrorCode.AUTHENTICATION);
        }

        PlatformTwoFaSettings twoFaSettings = configManager.getPlatformTwoFaSettings(user.getTenantId(), true)
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        if (checkLimits) {
            if (!rateLimitService.checkRateLimit(LimitedApi.TWO_FA_VERIFICATION_CODE_CHECK,
                    Pair.of(user.getId(), accountConfig.getProviderType()), twoFaSettings.getVerificationCodeCheckRateLimit())) {
                throw TOO_MANY_REQUESTS_ERROR;
            }
        }
        TwoFaProviderConfig providerConfig = twoFaSettings.getProviderConfig(accountConfig.getProviderType())
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);

        boolean verificationSuccess = false;
        if (StringUtils.isNotBlank(verificationCode)) {
            if (StringUtils.isNumeric(verificationCode) || accountConfig.getProviderType() == TwoFaProviderType.BACKUP_CODE) {
                verificationSuccess = getTwoFaProvider(accountConfig.getProviderType()).checkVerificationCode(user, verificationCode, providerConfig, accountConfig);
            }
        }
        if (checkLimits) {
            try {
                systemSecurityService.validateTwoFaVerification(user, verificationSuccess, twoFaSettings);
            } catch (LockedException e) {
                cleanUpRateLimits(user.getId());
                throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.AUTHENTICATION);
            }
            if (verificationSuccess) {
                cleanUpRateLimits(user.getId());
            }
        }
        return verificationSuccess;
    }

    /**
     * 功能：执行 `generateNewAccountConfig` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerType`：类型。
     * 返回：处理结果。
     */
    @Override
    public TwoFaAccountConfig generateNewAccountConfig(User user, TwoFaProviderType providerType) throws ThingsboardException {
        TwoFaProviderConfig providerConfig = getTwoFaProviderConfig(user.getTenantId(), providerType);
        return getTwoFaProvider(providerType).generateNewAccountConfig(user, providerConfig);
    }

    /**
     * 功能：删除或清理频率。
     * 参数：
     * - `userId`：用户ID。
     * 返回：无。
     */
    private void cleanUpRateLimits(UserId userId) {
        for (TwoFaProviderType providerType : TwoFaProviderType.values()) {
            rateLimitService.cleanUp(LimitedApi.TWO_FA_VERIFICATION_CODE_SEND, Pair.of(userId, providerType));
            rateLimitService.cleanUp(LimitedApi.TWO_FA_VERIFICATION_CODE_CHECK, Pair.of(userId, providerType));
        }
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `providerType`：类型。
     * 返回：处理结果。
     */
    private TwoFaProviderConfig getTwoFaProviderConfig(TenantId tenantId, TwoFaProviderType providerType) throws ThingsboardException {
        return configManager.getPlatformTwoFaSettings(tenantId, true)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType))
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
    }

    /**
     * 功能：获取提供者。
     * 参数：
     * - `providerType`：类型。
     * 返回：处理结果。
     */
    private TwoFaProvider<TwoFaProviderConfig, TwoFaAccountConfig> getTwoFaProvider(TwoFaProviderType providerType) throws ThingsboardException {
        return Optional.ofNullable(providers.get(providerType))
                .orElseThrow(() -> PROVIDER_NOT_AVAILABLE_ERROR);
    }

    /**
     * 功能：更新`Providers`。
     * 参数：
     * - `providers`：数据列表。
     * 返回：无。
     */
    @Autowired
    private void setProviders(Collection<TwoFaProvider> providers) {
        providers.forEach(provider -> {
            this.providers.put(provider.getType(), provider);
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTwoFactorAuthService` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
