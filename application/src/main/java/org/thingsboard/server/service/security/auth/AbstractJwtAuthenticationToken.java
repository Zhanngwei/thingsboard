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

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

/**
 * 中文说明：
 * 1. 类目的：`AbstractJwtAuthenticationToken` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public abstract class AbstractJwtAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -6212297506742428406L;

    /**
     * 令牌，用于认证或安全校验。
     */
    private RawAccessJwtToken rawAccessToken;
    private SecurityUser securityUser;

    /**
     * 功能：创建 `AbstractJwtAuthenticationToken` 实例，并初始化必要字段。
     * 参数：
     * - `unsafeToken`：`unsafeToken` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractJwtAuthenticationToken(RawAccessJwtToken unsafeToken) {
        super(null);
        this.rawAccessToken = unsafeToken;
        this.setAuthenticated(false);
    }

    /**
     * 功能：创建 `AbstractJwtAuthenticationToken` 实例，并初始化必要字段。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractJwtAuthenticationToken(SecurityUser securityUser) {
        super(securityUser.getAuthorities());
        this.eraseCredentials();
        this.securityUser = securityUser;
        super.setAuthenticated(true);
    }

    /**
     * 功能：更新`Authenticated`。
     * 参数：
     * - `authenticated`：`authenticated` 参数。
     * 返回：无。
     */
    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Object getCredentials() {
        return rawAccessToken;
    }

    /**
     * 功能：获取`Principal`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Object getPrincipal() {
        return this.securityUser;
    }

    /**
     * 功能：执行 `eraseCredentials` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.rawAccessToken = null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractJwtAuthenticationToken` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
