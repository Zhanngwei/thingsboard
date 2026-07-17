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
 * 1. `MailService` 是 ThingsBoard Rule Engine API 中定义 `Mail` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
