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
package org.thingsboard.server.service.security.auth.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.MfaAuthenticationToken;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.exception.UserPasswordNotValidException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.UUID;


/**
 * 中文说明：
 * 1. 类目的：`RestAuthenticationProvider` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Component
@Slf4j
@TbCoreComponent
public class RestAuthenticationProvider implements AuthenticationProvider {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final SystemSecurityService systemSecurityService;
    private final UserService userService;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    private final CustomerService customerService;
    private final TwoFactorAuthService twoFactorAuthService;

    /**
     * 功能：创建 `RestAuthenticationProvider` 实例，并初始化必要字段。
     * 参数：
     * - `userService`：服务对象。
     * - `customerService`：服务对象。
     * - `systemSecurityService`：服务对象。
     * - `twoFactorAuthService`：服务对象。
     * 返回：新创建的对象实例。
     */
    @Autowired
    public RestAuthenticationProvider(final UserService userService,
                                      final CustomerService customerService,
                                      final SystemSecurityService systemSecurityService,
                                      TwoFactorAuthService twoFactorAuthService) {
        this.userService = userService;
        this.customerService = customerService;
        this.systemSecurityService = systemSecurityService;
        this.twoFactorAuthService = twoFactorAuthService;
    }

    /**
     * 功能：执行 `authenticate` 对应的处理。
     * 参数：
     * - `authentication`：`authentication` 参数。
     * 返回：处理结果。
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new BadCredentialsException("Authentication Failed. Bad user principal.");
        }

        UserPrincipal userPrincipal =  (UserPrincipal) principal;
        SecurityUser securityUser;
        if (userPrincipal.getType() == UserPrincipal.Type.USER_NAME) {
            String username = userPrincipal.getValue();
            String password = (String) authentication.getCredentials();

            SecuritySettings securitySettings = systemSecurityService.getSecuritySettings();
            UserPasswordPolicy passwordPolicy = securitySettings.getPasswordPolicy();
            if (Boolean.TRUE.equals(passwordPolicy.getForceUserToResetPasswordIfNotValid())) {
                try {
                    systemSecurityService.validatePasswordByPolicy(password, passwordPolicy);
                } catch (DataValidationException e) {
                    throw new UserPasswordNotValidException("The entered password violates our policies. If this is your real password, please reset it.");
                }
            }

            securityUser = authenticateByUsernameAndPassword(authentication, userPrincipal, username, password);
            if (twoFactorAuthService.isTwoFaEnabled(securityUser.getTenantId(), securityUser.getId())) {
                return new MfaAuthenticationToken(securityUser);
            } else {
                systemSecurityService.logLoginAction(securityUser, authentication.getDetails(), ActionType.LOGIN, null);
            }
        } else {
            String publicId = userPrincipal.getValue();
            securityUser = authenticateByPublicId(userPrincipal, publicId);
        }

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    /**
     * 功能：执行 `authenticateByUsernameAndPassword` 对应的处理。
     * 参数：
     * - `authentication`：`authentication` 参数。
     * - `userPrincipal`：`userPrincipal` 参数。
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：处理结果。
     */
    private SecurityUser authenticateByUsernameAndPassword(Authentication authentication, UserPrincipal userPrincipal, String username, String password) {
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        try {

            UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
            if (userCredentials == null) {
                throw new UsernameNotFoundException("User credentials not found");
            }

            try {
                systemSecurityService.validateUserCredentials(user.getTenantId(), userCredentials, username, password);
            } catch (LockedException e) {
                systemSecurityService.logLoginAction(user, authentication.getDetails(), ActionType.LOCKOUT, null);
                throw e;
            }

            if (user.getAuthority() == null)
                throw new InsufficientAuthenticationException("User has no authority assigned");

            return new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);
        } catch (Exception e) {
            systemSecurityService.logLoginAction(user, authentication.getDetails(), ActionType.LOGIN, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `authenticateByPublicId` 对应的处理。
     * 参数：
     * - `userPrincipal`：`userPrincipal` 参数。
     * - `publicId`：`publicId`ID。
     * 返回：处理结果。
     */
    private SecurityUser authenticateByPublicId(UserPrincipal userPrincipal, String publicId) {
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        Customer publicCustomer = customerService.findCustomerById(TenantId.SYS_TENANT_ID, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found: " + publicId);
        }
        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        return new SecurityUser(user, true, userPrincipal);
    }

    /**
     * 功能：执行 `supports` 对应的处理。
     * 参数：
     * - `authentication`：`authentication` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RestAuthenticationProvider` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
