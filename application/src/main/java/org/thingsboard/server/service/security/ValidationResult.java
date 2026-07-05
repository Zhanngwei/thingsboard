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
package org.thingsboard.server.service.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 中文说明：
 * 1. 类目的：`ValidationResult` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Data
@AllArgsConstructor
public class ValidationResult<V> {

    /**
     * 编码，表示当前对象的对应属性。
     */
    private final ValidationResultCode resultCode;
    private final String message;
    /**
     * `v` 字段，保存当前对象的对应属性。
     */
    private final V v;

    /**
     * 功能：执行 `ok` 对应的处理。
     * 参数：
     * - `v`：`v` 参数。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> ok(V v) {
        return new ValidationResult<>(ValidationResultCode.OK, "Ok", v);
    }

    /**
     * 功能：执行 `accessDenied` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> accessDenied(String message) {
        return new ValidationResult<>(ValidationResultCode.ACCESS_DENIED, message, null);
    }

    /**
     * 功能：执行 `entityNotFound` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> entityNotFound(String message) {
        return new ValidationResult<>(ValidationResultCode.ENTITY_NOT_FOUND, message, null);
    }

    /**
     * 功能：执行 `unauthorized` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> unauthorized(String message) {
        return new ValidationResult<>(ValidationResultCode.UNAUTHORIZED, message, null);
    }

    /**
     * 功能：执行 `internalError` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> internalError(String message) {
        return new ValidationResult<>(ValidationResultCode.INTERNAL_ERROR, message, null);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ValidationResult` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
