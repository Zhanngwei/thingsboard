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
package org.thingsboard.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.TenantProfileNotFoundException;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 中文说明：
 * 1. 类目的：`RateLimitProcessingFilter` 是ThingsBoard Application 模块中的Spring 配置类型，用于声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 生命周期：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Configuration / Factory。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitProcessingFilter extends OncePerRequestFilter {

    /**
     * 响应，负责处理对应任务或消息。
     */
    private final ThingsboardErrorResponseHandler errorResponseHandler;
    private final RateLimitService rateLimitService;

    /**
     * 功能：执行 `doFilterInternal` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * - `chain`：`chain` 参数。
     * 返回：无。
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        SecurityUser user = getCurrentUser();
        if (user != null && !user.isSystemAdmin()) {
            try {
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_TENANT, user.getTenantId())) {
                    rateLimitExceeded(EntityType.TENANT, response);
                    return;
                }
            } catch (TenantProfileNotFoundException e) {
                log.debug("[{}] Failed to lookup tenant profile", user.getTenantId());
                errorResponseHandler.handle(new BadCredentialsException("Failed to lookup tenant profile"), response);
                return;
            }

            if (user.isCustomerUser()) {
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_CUSTOMER, user.getTenantId(), user.getCustomerId())) {
                    rateLimitExceeded(EntityType.CUSTOMER, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 功能：执行 `shouldNotFilterAsyncDispatch` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * 功能：执行 `shouldNotFilterErrorDispatch` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /**
     * 功能：执行 `rateLimitExceeded` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `response`：响应对象。
     * 返回：无。
     */
    private void rateLimitExceeded(EntityType type, HttpServletResponse response) {
        errorResponseHandler.handle(new TbRateLimitsException(type), response);
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    protected SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            return null;
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RateLimitProcessingFilter` 在 ThingsBoard Application 模块 中承担Spring 配置类型职责，核心目的是声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 核心流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
