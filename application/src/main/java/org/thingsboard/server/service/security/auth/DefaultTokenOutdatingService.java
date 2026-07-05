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

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTokenOutdatingService` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Service
public class DefaultTokenOutdatingService implements TokenOutdatingService {

    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private final TbTransactionalCache<String, Long> cache;
    private final JwtTokenFactory tokenFactory;

    /**
     * 功能：创建 `DefaultTokenOutdatingService` 实例，并初始化必要字段。
     * 参数：
     * - `cache`：`cache` 参数。
     * - `tokenFactory`：`tokenFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public DefaultTokenOutdatingService(@Qualifier("UsersSessionInvalidation") TbTransactionalCache<String, Long> cache, JwtTokenFactory tokenFactory) {
        this.cache = cache;
        this.tokenFactory = tokenFactory;
    }

    /**
     * 功能：处理用户。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        if (StringUtils.hasText(event.getId())) {
            cache.put(event.getId(), event.getTs());
        }
    }

    /**
     * 功能：判断`Outdated`。
     * 参数：
     * - `token`：`token` 参数。
     * - `userId`：用户ID。
     * 返回：判断结果。
     */
    @Override
    public boolean isOutdated(String token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();
        String sessionId = claims.get("sessionId", String.class);
        if (isTokenOutdated(issueTime, userId.toString())){
             return true;
        } else {
             return sessionId != null && isTokenOutdated(issueTime, sessionId);
        }
    }

    /**
     * 功能：判断令牌。
     * 参数：
     * - `issueTime`：`issueTime` 参数。
     * - `sessionId`：会话ID。
     * 返回：判断结果。
     */
    private Boolean isTokenOutdated(long issueTime, String sessionId) {
        return Optional.ofNullable(cache.get(sessionId)).map(outdatageTime -> isTokenOutdated(issueTime, outdatageTime.get())).orElse(false);
    }

    /**
     * 功能：判断令牌。
     * 参数：
     * - `issueTime`：`issueTime` 参数。
     * - `outdatageTime`：待处理数据。
     * 返回：判断结果。
     */
    private boolean isTokenOutdated(long issueTime, Long outdatageTime) {
        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTokenOutdatingService` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
