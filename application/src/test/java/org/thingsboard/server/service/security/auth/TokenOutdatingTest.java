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
package org.thingsboard.server.service.security.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.event.UserSessionInvalidationEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TokenOutdatingTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ComponentScan({"org.thingsboard.server"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DaoSqlTest
@TestPropertySource(properties = {
        "security.jwt.tokenIssuer=test.io",
        "security.jwt.tokenSigningKey=secret",
        "security.jwt.tokenExpirationTime=600",
        "security.jwt.refreshTokenExpTime=15",
        // explicitly set the wrong value to check that it is NOT used.
        "cache.specs.userSessionsInvalidation.timeToLiveInMinutes=2"
})
/**
 * 中文说明：
 * 1. 类目的：`TokenOutdatingTest` 是ThingsBoard Application 测试模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class TokenOutdatingTest {
    /**
     * 字段说明：
     * 1. 保存 `accessTokenAuthenticationProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private JwtAuthenticationProvider accessTokenAuthenticationProvider;
    private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tokenOutdatingService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TokenOutdatingService tokenOutdatingService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `eventPublisher` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tokenFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private JwtTokenFactory tokenFactory;
    private SecurityUser securityUser;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `setUp` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setUp() {
        UserId userId = new UserId(UUID.randomUUID());
        securityUser = createMockSecurityUser(userId);

        UserService userService = mock(UserService.class);

        User user = new User();
        user.setId(userId);
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("email");
        when(userService.findUserById(any(), eq(userId))).thenReturn(user);

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        when(userService.findUserCredentialsByUserId(any(), eq(userId))).thenReturn(userCredentials);

        accessTokenAuthenticationProvider = new JwtAuthenticationProvider(tokenFactory, tokenOutdatingService);
        refreshTokenAuthenticationProvider = new RefreshTokenAuthenticationProvider(tokenFactory, userService, mock(CustomerService.class), tokenOutdatingService);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testOutdateOldUserTokens` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testOutdateOldUserTokens() throws Exception {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        // Token outdatage time is rounded to 1 sec. Need to wait before outdating so that outdatage time is strictly after token issue time
        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        assertTrue(tokenOutdatingService.isOutdated(jwtToken.getToken(), securityUser.getId()));

        SECONDS.sleep(1);

        JwtToken newJwtToken = tokenFactory.createAccessJwtToken(securityUser);
        assertFalse(tokenOutdatingService.isOutdated(newJwtToken.getToken(), securityUser.getId()));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAuthenticateWithOutdatedAccessToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAuthenticateWithOutdatedAccessToken() throws InterruptedException {
        RawAccessJwtToken accessJwtToken = getRawJwtToken(tokenFactory.createAccessJwtToken(securityUser));

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAuthenticateWithOutdatedRefreshToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAuthenticateWithOutdatedRefreshToken() throws InterruptedException {
        RawAccessJwtToken refreshJwtToken = getRawJwtToken(tokenFactory.createRefreshToken(securityUser));

        assertDoesNotThrow(() -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(CredentialsExpiredException.class, () -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });
    }

    // This test takes too long to run and is basically testing the cache logic
//    @Test
//    public void testTokensOutdatageTimeRemovalFromCache() throws Exception {
//        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);
//
//        SECONDS.sleep(1);
//        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
//
//        SECONDS.sleep(1);
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30); // refreshTokenExpTime/2
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30 + 1); // refreshTokenExpTime/2 + 1
//
//        assertFalse(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testOnlyOneTokenExpired` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testOnlyOneTokenExpired() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserSessionInvalidationEvent(securityUser.getSessionId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testResetAllSessions` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testResetAllSessions() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `getRawJwtToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RawAccessJwtToken getRawJwtToken(JwtToken token) {
        return new RawAccessJwtToken(token.getToken());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createMockSecurityUser` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private SecurityUser createMockSecurityUser(UserId userId) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setEmail("email");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setAuthority(Authority.CUSTOMER_USER);
        securityUser.setId(userId);
        securityUser.setSessionId(UUID.randomUUID().toString());
        return securityUser;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TokenOutdatingTest` 在 ThingsBoard Application 测试模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
