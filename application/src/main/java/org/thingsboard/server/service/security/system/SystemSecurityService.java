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
package org.thingsboard.server.service.security.system;

import org.springframework.security.core.AuthenticationException;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;

/**
 * 中文说明：
 * 1. 类目的：`SystemSecurityService` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public interface SystemSecurityService {

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    SecuritySettings getSecuritySettings();

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `securitySettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    SecuritySettings saveSecuritySettings(SecuritySettings securitySettings);

    /**
     * 功能：校验密码。
     * 参数：
     * - `password`：`password` 参数。
     * - `passwordPolicy`：`passwordPolicy` 参数。
     * 返回：无。
     */
    void validatePasswordByPolicy(String password, UserPasswordPolicy passwordPolicy);

    /**
     * 功能：校验用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException;

    /**
     * 功能：校验`Two Fa Verification`。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `verificationSuccess`：`verificationSuccess` 参数。
     * - `twoFaSettings`：配置对象。
     * 返回：无。
     */
    void validateTwoFaVerification(SecurityUser securityUser, boolean verificationSuccess, PlatformTwoFaSettings twoFaSettings);

    /**
     * 功能：校验密码。
     * 参数：
     * - `password`：`password` 参数。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：无。
     */
    void validatePassword(String password, UserCredentials userCredentials) throws DataValidationException;

    /**
     * 功能：获取基础访问地址。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `httpServletRequest`：请求对象。
     * 返回：文本结果。
     */
    String getBaseUrl(TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest);

    /**
     * 功能：执行 `logLoginAction` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `authenticationDetails`：`authenticationDetails` 参数。
     * - `actionType`：类型。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, Exception e);

    /**
     * 功能：执行 `logLoginAction` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `authenticationDetails`：`authenticationDetails` 参数。
     * - `actionType`：类型。
     * - `provider`：`provider` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, String provider, Exception e);
}

/*
 * 本类总结：
 * 1. 核心职责：`SystemSecurityService` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
