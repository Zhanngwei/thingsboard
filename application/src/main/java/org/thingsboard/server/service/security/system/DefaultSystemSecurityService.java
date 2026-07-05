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
package org.thingsboard.server.service.security.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.user.UserServiceImpl;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.exception.UserPasswordExpiredException;
import org.thingsboard.server.service.security.exception.UserPasswordNotValidException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.utils.MiscUtils;
import ua_parser.Client;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.CacheConstants.SECURITY_SETTINGS_CACHE;

/**
 * 中文说明：
 * 1. 类目的：`DefaultSystemSecurityService` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Service
@Slf4j
public class DefaultSystemSecurityService implements SystemSecurityService {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AdminSettingsService adminSettingsService;

    /**
     * 编码器，表示当前对象的对应属性。
     */
    @Autowired
    private BCryptPasswordEncoder encoder;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    private UserService userService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private MailService mailService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AuditLogService auditLogService;

    /**
     * `self` 字段，保存当前对象的对应属性。
     */
    @Resource
    private SystemSecurityService self;

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Cacheable(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings getSecuritySettings() {
        SecuritySettings securitySettings = null;
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "securitySettings");
        if (adminSettings != null) {
            try {
                securitySettings = JacksonUtil.convertValue(adminSettings.getJsonValue(), SecuritySettings.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load security settings!", e);
            }
        } else {
            securitySettings = new SecuritySettings();
            securitySettings.setPasswordPolicy(new UserPasswordPolicy());
            securitySettings.getPasswordPolicy().setMinimumLength(6);
            securitySettings.getPasswordPolicy().setMaximumLength(72);
        }
        return securitySettings;
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `securitySettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @CacheEvict(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "securitySettings");
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setTenantId(TenantId.SYS_TENANT_ID);
            adminSettings.setKey("securitySettings");
        }
        adminSettings.setJsonValue(JacksonUtil.valueToTree(securitySettings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
        try {
            return JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), SecuritySettings.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load security settings!", e);
        }
    }

    /**
     * 功能：校验用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    @Override
    public void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException {
        if (!encoder.matches(password, userCredentials.getPassword())) {
            int failedLoginAttempts = userService.increaseFailedLoginAttempts(tenantId, userCredentials.getUserId());
            SecuritySettings securitySettings = self.getSecuritySettings();
            if (securitySettings.getMaxFailedLoginAttempts() != null && securitySettings.getMaxFailedLoginAttempts() > 0) {
                if (failedLoginAttempts > securitySettings.getMaxFailedLoginAttempts() && userCredentials.isEnabled()) {
                    lockAccount(userCredentials.getUserId(), username, securitySettings.getUserLockoutNotificationEmail(), securitySettings.getMaxFailedLoginAttempts());
                    throw new LockedException("Authentication Failed. Username was locked due to security policy.");
                }
            }
            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        userService.resetFailedLoginAttempts(tenantId, userCredentials.getUserId());

        SecuritySettings securitySettings = self.getSecuritySettings();
        if (isPositiveInteger(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays())) {
            if ((userCredentials.getCreatedTime()
                    + TimeUnit.DAYS.toMillis(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays()))
                    < System.currentTimeMillis()) {
                userCredentials = userService.requestExpiredPasswordReset(tenantId, userCredentials.getId());
                throw new UserPasswordExpiredException("User password expired!", userCredentials.getResetToken());
            }
        }
    }

    /**
     * 功能：校验`Two Fa Verification`。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `verificationSuccess`：`verificationSuccess` 参数。
     * - `twoFaSettings`：配置对象。
     * 返回：无。
     */
    @Override
    public void validateTwoFaVerification(SecurityUser securityUser, boolean verificationSuccess, PlatformTwoFaSettings twoFaSettings) {
        TenantId tenantId = securityUser.getTenantId();
        UserId userId = securityUser.getId();

        int failedVerificationAttempts;
        if (!verificationSuccess) {
            failedVerificationAttempts = userService.increaseFailedLoginAttempts(tenantId, userId);
        } else {
            userService.resetFailedLoginAttempts(tenantId, userId);
            return;
        }

        Integer maxVerificationFailures = twoFaSettings.getMaxVerificationFailuresBeforeUserLockout();
        if (maxVerificationFailures != null && maxVerificationFailures > 0
                && failedVerificationAttempts >= maxVerificationFailures) {
            userService.setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
            SecuritySettings securitySettings = self.getSecuritySettings();
            lockAccount(userId, securityUser.getEmail(), securitySettings.getUserLockoutNotificationEmail(), maxVerificationFailures);
            throw new LockedException("User account was locked due to exceeded 2FA verification attempts");
        }
    }

    /**
     * 功能：执行 `lockAccount` 对应的处理。
     * 参数：
     * - `userId`：用户ID。
     * - `username`：名称。
     * - `userLockoutNotificationEmail`：`userLockoutNotificationEmail` 参数。
     * - `maxFailedLoginAttempts`：`maxFailedLoginAttempts` 参数。
     * 返回：无。
     */
    private void lockAccount(UserId userId, String username, String userLockoutNotificationEmail, Integer maxFailedLoginAttempts) {
        userService.setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
        if (StringUtils.isNotBlank(userLockoutNotificationEmail)) {
            try {
                mailService.sendAccountLockoutEmail(username, userLockoutNotificationEmail, maxFailedLoginAttempts);
            } catch (ThingsboardException e) {
                log.warn("Can't send email regarding user account [{}] lockout to provided email [{}]", username, userLockoutNotificationEmail, e);
            }
        }
    }

    /**
     * 功能：校验密码。
     * 参数：
     * - `password`：`password` 参数。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：无。
     */
    @Override
    public void validatePassword(String password, UserCredentials userCredentials) throws DataValidationException {
        SecuritySettings securitySettings = self.getSecuritySettings();
        UserPasswordPolicy passwordPolicy = securitySettings.getPasswordPolicy();

        validatePasswordByPolicy(password, passwordPolicy);

        if (userCredentials != null && isPositiveInteger(passwordPolicy.getPasswordReuseFrequencyDays())) {
            long passwordReuseFrequencyTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(passwordPolicy.getPasswordReuseFrequencyDays());
            JsonNode additionalInfo = userCredentials.getAdditionalInfo();
            if (additionalInfo instanceof ObjectNode && additionalInfo.has(UserServiceImpl.USER_PASSWORD_HISTORY)) {
                JsonNode userPasswordHistoryJson = additionalInfo.get(UserServiceImpl.USER_PASSWORD_HISTORY);
                Map<String, String> userPasswordHistoryMap = JacksonUtil.convertValue(userPasswordHistoryJson, new TypeReference<>() {});
                for (Map.Entry<String, String> entry : userPasswordHistoryMap.entrySet()) {
                    if (encoder.matches(password, entry.getValue()) && Long.parseLong(entry.getKey()) > passwordReuseFrequencyTs) {
                        throw new DataValidationException("Password was already used for the last " + passwordPolicy.getPasswordReuseFrequencyDays() + " days");
                    }
                }
            }
        }
    }

    /**
     * 功能：校验密码。
     * 参数：
     * - `password`：`password` 参数。
     * - `passwordPolicy`：`passwordPolicy` 参数。
     * 返回：无。
     */
    @Override
    public void validatePasswordByPolicy(String password, UserPasswordPolicy passwordPolicy) {
        List<Rule> passwordRules = new ArrayList<>();

        Integer maximumLength = passwordPolicy.getMaximumLength();
        Integer minLengthBound = passwordPolicy.getMinimumLength();
        int maxLengthBound = (maximumLength != null && maximumLength > passwordPolicy.getMinimumLength()) ? maximumLength : Integer.MAX_VALUE;

        passwordRules.add(new LengthRule(minLengthBound, maxLengthBound));
        if (isPositiveInteger(passwordPolicy.getMinimumUppercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.UpperCase, passwordPolicy.getMinimumUppercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumLowercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.LowerCase, passwordPolicy.getMinimumLowercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumDigits())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Digit, passwordPolicy.getMinimumDigits()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumSpecialCharacters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Special, passwordPolicy.getMinimumSpecialCharacters()));
        }
        if (passwordPolicy.getAllowWhitespaces() != null && !passwordPolicy.getAllowWhitespaces()) {
            passwordRules.add(new WhitespaceRule());
        }
        PasswordValidator validator = new PasswordValidator(passwordRules);
        PasswordData passwordData = new PasswordData(password);
        RuleResult result = validator.validate(passwordData);
        if (!result.isValid()) {
            String message = String.join("\n", validator.getMessages(result));
            throw new DataValidationException(message);
        }
    }

    /**
     * 功能：获取基础访问地址。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `httpServletRequest`：请求对象。
     * 返回：文本结果。
     */
    @Override
    public String getBaseUrl(TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest) {
        String baseUrl = null;
        AdminSettings generalSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "general");

        JsonNode prohibitDifferentUrl = generalSettings.getJsonValue().get("prohibitDifferentUrl");

        if ((prohibitDifferentUrl != null && prohibitDifferentUrl.asBoolean()) || httpServletRequest == null) {
            baseUrl = generalSettings.getJsonValue().get("baseUrl").asText();
        }

        if (StringUtils.isEmpty(baseUrl) && httpServletRequest != null) {
            baseUrl = MiscUtils.constructBaseUrl(httpServletRequest);
        }

        return baseUrl;
    }

    /**
     * 功能：执行 `logLoginAction` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `authenticationDetails`：`authenticationDetails` 参数。
     * - `actionType`：类型。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void logLoginAction(User user, Object authenticationDetails, ActionType actionType, Exception e) {
        logLoginAction(user, authenticationDetails, actionType, null, e);
    }

    /**
     * 功能：执行 `logLoginAction` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `authenticationDetails`：`authenticationDetails` 参数。
     * - `actionType`：类型。
     * - `provider`：`provider` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void logLoginAction(User user, Object authenticationDetails, ActionType actionType, String provider, Exception e) {
        String clientAddress = "Unknown";
        String browser = "Unknown";
        String os = "Unknown";
        String device = "Unknown";
        if (authenticationDetails instanceof RestAuthenticationDetails) {
            RestAuthenticationDetails details = (RestAuthenticationDetails) authenticationDetails;
            clientAddress = details.getClientAddress();
            if (details.getUserAgent() != null) {
                Client userAgent = details.getUserAgent();
                if (userAgent.userAgent != null) {
                    browser = userAgent.userAgent.family;
                    if (userAgent.userAgent.major != null) {
                        browser += " " + userAgent.userAgent.major;
                        if (userAgent.userAgent.minor != null) {
                            browser += "." + userAgent.userAgent.minor;
                            if (userAgent.userAgent.patch != null) {
                                browser += "." + userAgent.userAgent.patch;
                            }
                        }
                    }
                }
                if (userAgent.os != null) {
                    os = userAgent.os.family;
                    if (userAgent.os.major != null) {
                        os += " " + userAgent.os.major;
                        if (userAgent.os.minor != null) {
                            os += "." + userAgent.os.minor;
                            if (userAgent.os.patch != null) {
                                os += "." + userAgent.os.patch;
                                if (userAgent.os.patchMinor != null) {
                                    os += "." + userAgent.os.patchMinor;
                                }
                            }
                        }
                    }
                }
                if (userAgent.device != null) {
                    device = userAgent.device.family;
                }
            }
        }
        if (actionType == ActionType.LOGIN && e == null) {
            userService.setLastLoginTs(user.getTenantId(), user.getId());
        }
        auditLogService.logEntityAction(
                user.getTenantId(), user.getCustomerId(), user.getId(),
                user.getName(), user.getId(), null, actionType, e, clientAddress, browser, os, device, provider);
    }

    /**
     * 功能：判断`Positive Integer`。
     * 参数：
     * - `val`：`val` 参数。
     * 返回：判断结果。
     */
    private static boolean isPositiveInteger(Integer val) {
        return val != null && val.intValue() > 0;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultSystemSecurityService` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
