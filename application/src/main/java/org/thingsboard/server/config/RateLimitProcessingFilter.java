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

@Slf4j
@Component
@RequiredArgsConstructor
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
public class RateLimitProcessingFilter extends OncePerRequestFilter {

    /**
     * 字段说明：
     * 1. 保存 `errorResponseHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final ThingsboardErrorResponseHandler errorResponseHandler;
    private final RateLimitService rateLimitService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `doFilterInternal` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        SecurityUser user = getCurrentUser();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (user != null && !user.isSystemAdmin()) {
            try {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_TENANT, user.getTenantId())) {
                    rateLimitExceeded(EntityType.TENANT, response);
                    return;
                }
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (TenantProfileNotFoundException e) {
                log.debug("[{}] Failed to lookup tenant profile", user.getTenantId());
                errorResponseHandler.handle(new BadCredentialsException("Failed to lookup tenant profile"), response);
                return;
            }

            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (user.isCustomerUser()) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_CUSTOMER, user.getTenantId(), user.getCustomerId())) {
                    rateLimitExceeded(EntityType.CUSTOMER, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `shouldNotFilterAsyncDispatch` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `shouldNotFilterErrorDispatch` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `rateLimitExceeded` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void rateLimitExceeded(EntityType type, HttpServletResponse response) {
        errorResponseHandler.handle(new TbRateLimitsException(type), response);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCurrentUser` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
