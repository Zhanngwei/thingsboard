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
package org.thingsboard.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultJwtSettingsValidator` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class DefaultJwtSettingsValidator implements JwtSettingsValidator {

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `validate` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void validate(JwtSettings jwtSettings) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isEmpty(jwtSettings.getTokenIssuer())) {
            throw new DataValidationException("JWT token issuer should be specified!");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (Optional.ofNullable(jwtSettings.getRefreshTokenExpTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(15)) {
            throw new DataValidationException("JWT refresh token expiration time should be at least 15 minutes!");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (Optional.ofNullable(jwtSettings.getTokenExpirationTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(1)) {
            throw new DataValidationException("JWT token expiration time should be at least 1 minute!");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (jwtSettings.getTokenExpirationTime() >= jwtSettings.getRefreshTokenExpTime()) {
            throw new DataValidationException("JWT token expiration time should greater than JWT refresh token expiration time!");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isEmpty(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be specified!");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(jwtSettings.getTokenSigningKey());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            throw new DataValidationException("JWT token signing key should be a valid Base64 encoded string! " + e.getMessage());
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (Arrays.isNullOrEmpty(decodedKey)) {
            throw new DataValidationException("JWT token signing key should be non-empty after Base64 decoding!");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (decodedKey.length * Byte.SIZE < 256 && !JwtSettingsService.TOKEN_SIGNING_KEY_DEFAULT.equals(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be a Base64 encoded string representing at least 256 bits of data!");
        }

        System.arraycopy(decodedKey, 0, RandomUtils.nextBytes(decodedKey.length), 0, decodedKey.length); //secure memory
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultJwtSettingsValidator` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
