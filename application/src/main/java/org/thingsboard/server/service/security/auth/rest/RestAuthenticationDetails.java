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
package org.thingsboard.server.service.security.auth.rest;

import lombok.Data;
import ua_parser.Client;
import ua_parser.Parser;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * 中文说明：
 * 1. 类目的：`RestAuthenticationDetails` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Data
public class RestAuthenticationDetails implements Serializable {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final String clientAddress;
    private final Client userAgent;

    /**
     * 功能：创建 `RestAuthenticationDetails` 实例，并初始化必要字段。
     * 参数：
     * - `request`：请求对象。
     * 返回：新创建的对象实例。
     */
    public RestAuthenticationDetails(HttpServletRequest request) {
        this.clientAddress = getClientIP(request);
        this.userAgent = getUserAgent(request);
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    private static String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    private static Client getUserAgent(HttpServletRequest request) {
        Parser uaParser = new Parser();
        return uaParser.parse(request.getHeader("User-Agent"));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RestAuthenticationDetails` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
