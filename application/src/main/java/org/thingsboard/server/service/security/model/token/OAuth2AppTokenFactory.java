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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`OAuth2AppTokenFactory` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Component
@Slf4j
public class OAuth2AppTokenFactory {

    /**
     * 回调常量，用于统一引用固定值。
     */
    private static final String CALLBACK_URL_SCHEME = "callbackUrlScheme";

    private static final long MAX_EXPIRATION_TIME_DIFF_MS = TimeUnit.MINUTES.toMillis(5);

    /**
     * 功能：校验回调。
     * 参数：
     * - `appPackage`：`appPackage` 参数。
     * - `appToken`：`appToken` 参数。
     * - `appSecret`：`appSecret` 参数。
     * 返回：判断结果。
     */
    public String validateTokenAndGetCallbackUrlScheme(String appPackage, String appToken, String appSecret) {
        Jws<Claims> jwsClaims;
        try {
            jwsClaims = Jwts.parser().setSigningKey(appSecret).parseClaimsJws(appToken);
        }
        catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException | SignatureException ex) {
            throw new IllegalArgumentException("Invalid Application token: ", ex);
        } catch (ExpiredJwtException expiredEx) {
            throw new IllegalArgumentException("Application token expired", expiredEx);
        }
        Claims claims = jwsClaims.getBody();
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new IllegalArgumentException("Application token must have expiration date");
        }
        long timeDiff = expiration.getTime() - System.currentTimeMillis();
        if (timeDiff > MAX_EXPIRATION_TIME_DIFF_MS) {
            throw new IllegalArgumentException("Application token expiration time can't be longer than 5 minutes");
        }
        if (!claims.getIssuer().equals(appPackage)) {
            throw new IllegalArgumentException("Application token issuer doesn't match application package");
        }
        String callbackUrlScheme = claims.get(CALLBACK_URL_SCHEME, String.class);
        if (StringUtils.isEmpty(callbackUrlScheme)) {
            throw new IllegalArgumentException("Application token doesn't have callbackUrlScheme");
        }
        return callbackUrlScheme;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2AppTokenFactory` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
