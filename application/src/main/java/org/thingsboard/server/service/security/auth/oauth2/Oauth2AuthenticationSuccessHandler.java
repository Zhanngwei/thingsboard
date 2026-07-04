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
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;

@Slf4j
@Component(value = "oauth2AuthenticationSuccessHandler")
@TbCoreComponent
/**
 * 中文说明：
 * 1. 类目的：`Oauth2AuthenticationSuccessHandler` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class Oauth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /**
     * 字段说明：
     * 1. 保存 `tokenFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final JwtTokenFactory tokenFactory;
    private final OAuth2ClientMapperProvider oauth2ClientMapperProvider;
    /**
     * 字段说明：
     * 1. 保存 `oAuth2Service` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final OAuth2Service oAuth2Service;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    /**
     * 字段说明：
     * 1. 保存 `httpCookieOAuth2AuthorizationRequestRepository` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final SystemSecurityService systemSecurityService;

    @Autowired
    /**
     * 方法说明：
     * 1. 职责：执行 `Oauth2AuthenticationSuccessHandler` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Oauth2AuthenticationSuccessHandler(final JwtTokenFactory tokenFactory,
                                              final OAuth2ClientMapperProvider oauth2ClientMapperProvider,
                                              final OAuth2Service oAuth2Service,
                                              final OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
                                              final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository,
                                              final SystemSecurityService systemSecurityService) {
        this.tokenFactory = tokenFactory;
        this.oauth2ClientMapperProvider = oauth2ClientMapperProvider;
        this.oAuth2Service = oAuth2Service;
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
        this.systemSecurityService = systemSecurityService;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAuthenticationSuccess` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
        OAuth2AuthorizationRequest authorizationRequest = httpCookieOAuth2AuthorizationRequestRepository.loadAuthorizationRequest(request);
        String callbackUrlScheme = authorizationRequest.getAttribute(TbOAuth2ParameterNames.CALLBACK_URL_SCHEME);
        String baseUrl;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!StringUtils.isEmpty(callbackUrlScheme)) {
            baseUrl = callbackUrlScheme + ":";
        } else {
            baseUrl = this.systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
            Optional<Cookie> prevUrlOpt = CookieUtils.getCookie(request, PREV_URI_COOKIE_NAME);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (prevUrlOpt.isPresent()) {
                baseUrl += prevUrlOpt.get().getValue();
                CookieUtils.deleteCookie(request, response, PREV_URI_COOKIE_NAME);
            }
        }
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

            OAuth2Registration registration = oAuth2Service.findRegistration(UUID.fromString(token.getAuthorizedClientRegistrationId()));
            OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getPrincipal().getName());
            OAuth2ClientMapper mapper = oauth2ClientMapperProvider.getOAuth2ClientMapperByType(registration.getMapperConfig().getType());
            SecurityUser securityUser = mapper.getOrCreateUserByClientPrincipal(request, token, oAuth2AuthorizedClient.getAccessToken().getTokenValue(),
                    registration);

            clearAuthenticationAttributes(request, response);

            JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);
            getRedirectStrategy().sendRedirect(request, response, getRedirectUrl(baseUrl, tokenPair));
            systemSecurityService.logLoginAction(securityUser, new RestAuthenticationDetails(request), ActionType.LOGIN, registration.getName(), null);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.debug("Error occurred during processing authentication success result. " +
                    "request [{}], response [{}], authentication [{}]", request, response, authentication, e);
            clearAuthenticationAttributes(request, response);
            String errorPrefix;
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!StringUtils.isEmpty(callbackUrlScheme)) {
                errorPrefix = "/?error=";
            } else {
                errorPrefix = "/login?loginError=";
            }
            getRedirectStrategy().sendRedirect(request, response, baseUrl + errorPrefix +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8.toString()));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `clearAuthenticationAttributes` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getRedirectUrl` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    String getRedirectUrl(String baseUrl, JwtPair tokenPair) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (baseUrl.indexOf("?") > 0) {
            baseUrl += "&";
        } else {
            baseUrl += "/?";
        }
        return baseUrl + "accessToken=" + tokenPair.getToken() + "&refreshToken=" + tokenPair.getRefreshToken();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`Oauth2AuthenticationSuccessHandler` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
