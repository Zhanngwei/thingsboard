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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.mail.javamail.JavaMailSender;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. 职责：定义 Rule Engine、通知和账户流程使用邮件能力的统一入口。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的外部邮件服务边界。
 * 3. 协作对象：与邮件规则节点、通知中心、系统/租户邮件配置、{@link TbEmail} 和 JavaMailSender 实现协作。
 * 4. 生命周期：接口由 Spring Bean 实现提供，规则节点或系统流程在需要发送邮件时通过 {@link TbContext} 获取并调用。
 * 5. 设计原因：规则节点不应直接依赖具体邮件实现，通过接口隔离配置刷新、测试连接和真实发送。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库；具体实现可能读取配置、执行外部 SMTP 调用并被 Rule Engine 使用。
 */
public interface MailService {

    /**
     * 功能：更新`Mail Configuration`。
     * 参数：无。
     * 返回：无。
     */
    void updateMailConfiguration();

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * - `subject`：`subject` 参数。
     * - `message`：待处理消息。
     * 返回：无。
     */
    void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException;

    /**
     * 功能：发送或提交`Test Mail`。
     * 参数：
     * - `config`：配置对象。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    void sendTestMail(JsonNode config, String email) throws ThingsboardException;

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `activationLink`：`activationLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    void sendActivationEmail(String activationLink, String email) throws ThingsboardException;

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `loginLink`：`loginLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException;

    /**
     * 功能：发送或提交密码。
     * 参数：
     * - `passwordResetLink`：`passwordResetLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    void sendResetPasswordEmail(String passwordResetLink, String email) throws ThingsboardException;

    /**
     * 功能：发送或提交密码。
     * 参数：
     * - `passwordResetLink`：`passwordResetLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    void sendResetPasswordEmailAsync(String passwordResetLink, String email);

    /**
     * 功能：发送或提交密码。
     * 参数：
     * - `loginLink`：`loginLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException;

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `lockoutEmail`：`lockoutEmail` 参数。
     * - `email`：`email` 参数。
     * - `maxFailedLoginAttempts`：`maxFailedLoginAttempts` 参数。
     * 返回：无。
     */
    void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException;

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * - `verificationCode`：`verificationCode` 参数。
     * - `expirationTimeSeconds`：`expirationTimeSeconds` 参数。
     * 返回：无。
     */
    void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException;

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `tbEmail`：`tbEmail` 参数。
     * 返回：无。
     */
    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException;

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `tbEmail`：`tbEmail` 参数。
     * - `javaMailSender`：`javaMailSender` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException;

    /**
     * 功能：发送或提交状态。
     * 参数：
     * - `apiFeature`：`apiFeature` 参数。
     * - `stateValue`：值。
     * - `email`：`email` 参数。
     * - `recordState`：`recordState` 参数。
     * 返回：无。
     */
    void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException;

    /**
     * 功能：验证`Connection`相关场景。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void testConnection(TenantId tenantId) throws Exception;

    /**
     * 功能：判断`Configured`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    boolean isConfigured(TenantId tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：为 Rule Engine、通知和账户流程提供统一邮件发送 API。
 * 2. 核心流程：调用方准备邮件内容或配置，MailService 实现读取配置并调用外部 SMTP 服务。
 * 3. 关键依赖：TbEmail、TenantId、CustomerId、JavaMailSender、邮件配置和通知流程。
 * 4. 学习重点：规则节点通过服务接口使用邮件能力，不直接管理 SMTP 客户端和配置刷新细节。
 */
