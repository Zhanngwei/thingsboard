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
package org.thingsboard.server.service.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.RefreshAuthenticationToken;
import org.thingsboard.server.service.security.auth.TokenOutdatingService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`RefreshTokenAuthenticationProvider` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {
    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final JwtTokenFactory tokenFactory;
    private final UserService userService;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    private final CustomerService customerService;
    private final TokenOutdatingService tokenOutdatingService;

    /**
     * 功能：执行 `authenticate` 对应的处理。
     * 参数：
     * - `authentication`：`authentication` 参数。
     * 返回：处理结果。
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser unsafeUser = tokenFactory.parseRefreshToken(rawAccessToken.getToken());
        UserPrincipal principal = unsafeUser.getUserPrincipal();

        SecurityUser securityUser;
        if (principal.getType() == UserPrincipal.Type.USER_NAME) {
            securityUser = authenticateByUserId(unsafeUser.getId());
        } else {
            securityUser = authenticateByPublicId(principal.getValue());
        }
        securityUser.setSessionId(unsafeUser.getSessionId());
        if (tokenOutdatingService.isOutdated(rawAccessToken.getToken(), securityUser.getId())) {
            throw new CredentialsExpiredException("Token is outdated");
        }

        return new RefreshAuthenticationToken(securityUser);
    }

    /**
     * 功能：执行 `authenticateByUserId` 对应的处理。
     * 参数：
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    private SecurityUser authenticateByUserId(UserId userId) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        User user = userService.findUserById(systemId, userId);
        if (user == null) {
            throw new UsernameNotFoundException("User not found by refresh token");
        }

        UserCredentials userCredentials = userService.findUserCredentialsByUserId(systemId, user.getId());
        if (userCredentials == null) {
            throw new UsernameNotFoundException("User credentials not found");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        if (user.getAuthority() == null) throw new InsufficientAuthenticationException("User has no authority assigned");

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);

        return securityUser;
    }

    /**
     * 功能：执行 `authenticateByPublicId` 对应的处理。
     * 参数：
     * - `publicId`：`publicId`ID。
     * 返回：处理结果。
     */
    private SecurityUser authenticateByPublicId(String publicId) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token is not valid");
        }
        Customer publicCustomer = customerService.findCustomerById(systemId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found by refresh token");
        }

        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Refresh token is not valid");
        }

        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId);

        SecurityUser securityUser = new SecurityUser(user, true, userPrincipal);

        return securityUser;
    }

    /**
     * 功能：执行 `supports` 对应的处理。
     * 参数：
     * - `authentication`：`authentication` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return (RefreshAuthenticationToken.class.isAssignableFrom(authentication));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RefreshTokenAuthenticationProvider` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
