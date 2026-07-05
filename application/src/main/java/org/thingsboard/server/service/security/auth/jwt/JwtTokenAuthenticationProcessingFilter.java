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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.thingsboard.server.service.security.auth.JwtAuthenticationToken;
import org.thingsboard.server.service.security.auth.jwt.extractor.TokenExtractor;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 中文说明：
 * 1. 类目的：`JwtTokenAuthenticationProcessingFilter` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class JwtTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final AuthenticationFailureHandler failureHandler;
    private final TokenExtractor tokenExtractor;

    /**
     * 功能：创建 `JwtTokenAuthenticationProcessingFilter` 实例，并初始化必要字段。
     * 参数：
     * - `failureHandler`：处理器对象。
     * - `tokenExtractor`：`tokenExtractor` 参数。
     * - `matcher`：`matcher` 参数。
     * 返回：新创建的对象实例。
     */
    @Autowired
    public JwtTokenAuthenticationProcessingFilter(AuthenticationFailureHandler failureHandler,
                                                  TokenExtractor tokenExtractor, RequestMatcher matcher) {
        super(matcher);
        this.failureHandler = failureHandler;
        this.tokenExtractor = tokenExtractor;
    }

    /**
     * 功能：执行 `attemptAuthentication` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：处理结果。
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        RawAccessJwtToken token = new RawAccessJwtToken(tokenExtractor.extract(request));
        return getAuthenticationManager().authenticate(new JwtAuthenticationToken(token));
    }

    /**
     * 功能：执行 `successfulAuthentication` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * - `chain`：`chain` 参数。
     * - `authResult`：`authResult` 参数。
     * 返回：无。
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    /**
     * 功能：执行 `unsuccessfulAuthentication` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * - `failed`：`failed` 参数。
     * 返回：无。
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JwtTokenAuthenticationProcessingFilter` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
