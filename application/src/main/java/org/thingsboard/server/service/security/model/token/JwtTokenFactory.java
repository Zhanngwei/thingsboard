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
package org.thingsboard.server.service.security.model.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`JwtTokenFactory` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class JwtTokenFactory {

    /**
     * 字段说明：
     * 1. 保存 `SCOPES` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String SCOPES = "scopes";
    private static final String USER_ID = "userId";
    /**
     * 字段说明：
     * 1. 保存 `FIRST_NAME` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    /**
     * 字段说明：
     * 1. 保存 `ENABLED` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ENABLED = "enabled";
    private static final String IS_PUBLIC = "isPublic";
    /**
     * 字段说明：
     * 1. 保存 `TENANT_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String TENANT_ID = "tenantId";
    private static final String CUSTOMER_ID = "customerId";
    /**
     * 字段说明：
     * 1. 保存 `SESSION_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String SESSION_ID = "sessionId";

    /**
     * 字段说明：
     * 1. 保存 `jwtSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final JwtSettingsService jwtSettingsService;

    /**
     * Factory method for issuing new JWT Tokens.
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `createAccessJwtToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (securityUser.getAuthority() == null) {
            throw new IllegalArgumentException("User doesn't have any privileges");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        JwtBuilder jwtBuilder = setUpToken(securityUser, securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()), jwtSettingsService.getJwtSettings().getTokenExpirationTime());
        jwtBuilder.claim(FIRST_NAME, securityUser.getFirstName())
                .claim(LAST_NAME, securityUser.getLastName())
                .claim(ENABLED, securityUser.isEnabled())
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (securityUser.getTenantId() != null) {
            jwtBuilder.claim(TENANT_ID, securityUser.getTenantId().getId().toString());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (securityUser.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, securityUser.getCustomerId().getId().toString());
        }

        String token = jwtBuilder.compact();

        return new AccessJwtToken(token);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `parseAccessJwtToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SecurityUser parseAccessJwtToken(String token) {
        Jws<Claims> jwsClaims = parseTokenClaims(token);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("JWT Token doesn't have any scopes");
        }

        SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setEmail(subject);
        securityUser.setAuthority(Authority.parse(scopes.get(0)));
        String tenantId = claims.get(TENANT_ID, String.class);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (tenantId != null) {
            securityUser.setTenantId(TenantId.fromUUID(UUID.fromString(tenantId)));
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (securityUser.getAuthority() == Authority.SYS_ADMIN) {
            securityUser.setTenantId(TenantId.SYS_TENANT_ID);
        }
        String customerId = claims.get(CUSTOMER_ID, String.class);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (customerId != null) {
            securityUser.setCustomerId(new CustomerId(UUID.fromString(customerId)));
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }

        UserPrincipal principal;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (securityUser.getAuthority() != Authority.PRE_VERIFICATION_TOKEN) {
            securityUser.setFirstName(claims.get(FIRST_NAME, String.class));
            securityUser.setLastName(claims.get(LAST_NAME, String.class));
            securityUser.setEnabled(claims.get(ENABLED, Boolean.class));
            boolean isPublic = claims.get(IS_PUBLIC, Boolean.class);
            principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
        } else {
            principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, subject);
        }
        securityUser.setUserPrincipal(principal);

        return securityUser;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createRefreshToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtToken createRefreshToken(SecurityUser securityUser) {
        UserPrincipal principal = securityUser.getUserPrincipal();

        String token = setUpToken(securityUser, Collections.singletonList(Authority.REFRESH_TOKEN.name()), jwtSettingsService.getJwtSettings().getRefreshTokenExpTime())
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID)
                .setId(UUID.randomUUID().toString()).compact();

        return new AccessJwtToken(token);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `parseRefreshToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SecurityUser parseRefreshToken(String token) {
        Jws<Claims> jwsClaims = parseTokenClaims(token);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("Refresh Token doesn't have any scopes");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!scopes.get(0).equals(Authority.REFRESH_TOKEN.name())) {
            throw new IllegalArgumentException("Invalid Refresh Token scope");
        }
        boolean isPublic = claims.get(IS_PUBLIC, Boolean.class);
        UserPrincipal principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
        SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setUserPrincipal(principal);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }
        return securityUser;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createPreVerificationToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtToken createPreVerificationToken(SecurityUser user, Integer expirationTime) {
        JwtBuilder jwtBuilder = setUpToken(user, Collections.singletonList(Authority.PRE_VERIFICATION_TOKEN.name()), expirationTime)
                .claim(TENANT_ID, user.getTenantId().toString());
        if (user.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, user.getCustomerId().toString());
        }
        return new AccessJwtToken(jwtBuilder.compact());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpToken` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private JwtBuilder setUpToken(SecurityUser securityUser, List<String> scopes, long expirationTime) {
        if (StringUtils.isBlank(securityUser.getEmail())) {
            throw new IllegalArgumentException("Cannot create JWT Token without username/email");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        Claims claims = Jwts.claims().setSubject(principal.getValue());
        claims.put(USER_ID, securityUser.getId().getId().toString());
        claims.put(SCOPES, scopes);
        if (securityUser.getSessionId() != null) {
            claims.put(SESSION_ID, securityUser.getSessionId());
        }

        ZonedDateTime currentTime = ZonedDateTime.now();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(jwtSettingsService.getJwtSettings().getTokenIssuer())
                .setIssuedAt(Date.from(currentTime.toInstant()))
                .setExpiration(Date.from(currentTime.plusSeconds(expirationTime).toInstant()))
                .signWith(SignatureAlgorithm.HS512, jwtSettingsService.getJwtSettings().getTokenSigningKey());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `parseTokenClaims` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Jws<Claims> parseTokenClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSettingsService.getJwtSettings().getTokenSigningKey())
                    .parseClaimsJws(token);
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT Token", ex);
            throw new BadCredentialsException("Invalid JWT token: ", ex);
        } catch (SignatureException | ExpiredJwtException expiredEx) {
            log.debug("JWT Token is expired", expiredEx);
            throw new JwtExpiredTokenException(token, "JWT Token expired", expiredEx);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTokenPair` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtPair createTokenPair(SecurityUser securityUser) {
        JwtToken accessToken = createAccessJwtToken(securityUser);
        JwtToken refreshToken = createRefreshToken(securityUser);
        return new JwtPair(accessToken.getToken(), refreshToken.getToken());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JwtTokenFactory` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
