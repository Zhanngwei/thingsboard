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

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.server.exception.AccessDeniedException;
import org.thingsboard.server.exception.EntityNotFoundException;
import org.thingsboard.server.exception.InternalErrorException;
import org.thingsboard.server.exception.UnauthorizedException;

/**
 * Created by ashvayka on 31.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`ValidationCallback` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class ValidationCallback<T> implements FutureCallback<ValidationResult> {

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    private final T response;
    private final FutureCallback<T> action;

    /**
     * 功能：创建 `ValidationCallback` 实例，并初始化必要字段。
     * 参数：
     * - `response`：响应对象。
     * - `action`：`action` 参数。
     * 返回：新创建的对象实例。
     */
    public ValidationCallback(T response, FutureCallback<T> action) {
        this.response = response;
        this.action = action;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onSuccess(ValidationResult result) {
        if (result.getResultCode() == ValidationResultCode.OK) {
            action.onSuccess(response);
        } else {
            onFailure(getException(result));
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(Throwable e) {
        action.onFailure(e);
    }

    /**
     * 功能：获取`Exception`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    public static Exception getException(ValidationResult result) {
        ValidationResultCode resultCode = result.getResultCode();
        Exception e;
        switch (resultCode) {
            case ENTITY_NOT_FOUND:
                e = new EntityNotFoundException(result.getMessage());
                break;
            case UNAUTHORIZED:
                e = new UnauthorizedException(result.getMessage());
                break;
            case ACCESS_DENIED:
                e = new AccessDeniedException(result.getMessage());
                break;
            case INTERNAL_ERROR:
                e = new InternalErrorException(result.getMessage());
                break;
            default:
                e = new UnauthorizedException("Permission denied.");
                break;
        }
        return e;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ValidationCallback` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
