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
     * 中文说明：
     * 1. 方法职责：刷新当前邮件发送配置。
     * 2. 输入参数：无。
     * 3. 返回值：无，配置刷新结果由实现类内部状态体现。
     * 4. 调用时机：系统或租户邮件配置变更后调用。
     * 5. 调用方：配置管理流程、系统服务或需要重新加载邮件配置的 Rule Engine 支撑流程。
     * 6. 使用流程：被邮件发送前的配置生命周期使用。
     * 7. 线程安全：接口不保证线程安全，具体实现需要保护共享邮件配置。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现可能更新配置缓存，间接服务 Rule Engine。
     */
    void updateMailConfiguration();

    /**
     * 中文说明：
     * 1. 方法职责：向指定邮箱发送普通邮件。
     * 2. 输入参数：tenantId 表示租户边界，email 是收件人，subject 是主题，message 是正文。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：账户通知、规则节点或系统流程需要发送简单邮件时调用。
     * 5. 调用方：通知中心、账户管理流程和邮件规则节点。
     * 6. 使用流程：属于 Rule Engine/通知外部邮件发送流程。
     * 7. 线程安全：接口不保存状态；具体实现需保证邮件客户端和配置读取的并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及 MQTT、Actor；实现可能读取配置缓存并调用 SMTP，不应在接口层开启数据库事务。
     */
    void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：使用给定 JSON 邮件配置发送测试邮件。
     * 2. 输入参数：config 是待验证的邮件配置，email 是测试收件人。
     * 3. 返回值：无；配置错误或发送失败通过 ThingsboardException 抛出。
     * 4. 调用时机：管理员测试邮件配置时调用。
     * 5. 调用方：邮件配置管理 API 或系统配置界面。
     * 6. 使用流程：属于配置验证流程，不属于单条 Rule Engine 消息处理。
     * 7. 线程安全：方法线程安全取决于实现是否隔离测试用 JavaMailSender。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、缓存、MQTT、Actor、数据库；实现会执行外部邮件连接测试。
     */
    void sendTestMail(JsonNode config, String email) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：发送账户激活邮件。
     * 2. 输入参数：activationLink 是激活链接，email 是目标邮箱。
     * 3. 返回值：无；发送失败通过 ThingsboardException 抛出。
     * 4. 调用时机：用户创建或重新发送激活链接时调用。
     * 5. 调用方：账户管理流程。
     * 6. 使用流程：属于系统账户通知流程，可与 Rule Engine 共用邮件服务实现。
     * 7. 线程安全：接口无状态，具体实现需处理模板和发送器并发。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及 MQTT、Actor、数据库事务；可能读取邮件配置缓存，不直接处理规则消息。
     */
    void sendActivationEmail(String activationLink, String email) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：发送账户已激活通知邮件。
     * 2. 输入参数：loginLink 是登录入口，email 是目标邮箱。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：账户激活完成后调用。
     * 5. 调用方：账户管理流程。
     * 6. 使用流程：属于系统邮件通知流程。
     * 7. 线程安全：由具体实现保证模板渲染和发送配置并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口本身不涉及事务、缓存、MQTT、Actor、数据库；不直接参与规则链路由。
     */
    void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：发送重置密码链接邮件。
     * 2. 输入参数：passwordResetLink 是重置链接，email 是目标邮箱。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：用户请求重置密码时调用。
     * 5. 调用方：认证和账户管理流程。
     * 6. 使用流程：属于系统账户邮件流程。
     * 7. 线程安全：由实现保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、MQTT、Actor、数据库；实现可能读取邮件配置缓存。
     */
    void sendResetPasswordEmail(String passwordResetLink, String email) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：异步发送重置密码链接邮件。
     * 2. 输入参数：passwordResetLink 是重置链接，email 是目标邮箱。
     * 3. 返回值：无；异步失败通常由实现记录或回调处理。
     * 4. 调用时机：调用方不希望阻塞当前请求线程时调用。
     * 5. 调用方：认证和账户管理流程。
     * 6. 使用流程：属于异步外部邮件发送流程。
     * 7. 线程安全：实现必须保证异步执行器和邮件配置并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、MQTT、Actor、数据库；实现可能使用异步执行器和配置缓存。
     */
    void sendResetPasswordEmailAsync(String passwordResetLink, String email);

    /**
     * 中文说明：
     * 1. 方法职责：发送密码已重置通知邮件。
     * 2. 输入参数：loginLink 是登录入口，email 是目标邮箱。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：密码重置成功后调用。
     * 5. 调用方：账户管理流程。
     * 6. 使用流程：属于系统账户邮件通知流程。
     * 7. 线程安全：由具体实现保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库。
     */
    void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：发送账户锁定通知邮件。
     * 2. 输入参数：lockoutEmail 是锁定说明，email 是目标邮箱，maxFailedLoginAttempts 是允许失败次数。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：账户因登录失败次数过多被锁定时调用。
     * 5. 调用方：认证安全流程。
     * 6. 使用流程：属于系统安全通知流程。
     * 7. 线程安全：由实现保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、MQTT、Actor、数据库；可能读取邮件模板或配置缓存。
     */
    void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：发送双因素认证验证码邮件。
     * 2. 输入参数：email 是目标邮箱，verificationCode 是验证码，expirationTimeSeconds 是有效期秒数。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：用户登录或敏感操作需要二次验证时调用。
     * 5. 调用方：认证安全流程。
     * 6. 使用流程：属于系统安全邮件通知流程。
     * 7. 线程安全：由实现保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现可能读取配置缓存。
     */
    void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：发送 Rule Engine 或通知流程构建的完整邮件对象。
     * 2. 输入参数：tenantId 是租户边界，customerId 是客户上下文，tbEmail 是邮件载荷。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：邮件规则节点或通知中心已完成模板渲染后调用。
     * 5. 调用方：邮件规则节点、通知中心。
     * 6. 使用流程：直接服务 Rule Engine 外部邮件节点流程。
     * 7. 线程安全：接口无状态，具体实现需保证发送器和配置并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及 MQTT、Actor；实现可能读取配置缓存，不应在接口层声明事务。
     */
    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：使用指定 JavaMailSender 和超时时间发送完整邮件对象。
     * 2. 输入参数：tenantId/customerId 表示上下文，tbEmail 是邮件载荷，javaMailSender 是发送器，timeout 是发送超时。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：调用方需要使用临时或定制邮件发送器时调用。
     * 5. 调用方：配置测试流程、邮件服务实现内部或规则节点扩展逻辑。
     * 6. 使用流程：属于外部邮件调用流程。
     * 7. 线程安全：取决于传入 JavaMailSender 和实现对超时控制的并发处理。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；执行外部 SMTP 调用，可能被 Rule Engine 间接触发。
     */
    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：发送 API 使用状态变化通知邮件。
     * 2. 输入参数：apiFeature 是功能项，stateValue 是状态值，email 是收件人，recordState 是记录状态。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：API 使用量或限流状态发生变化时调用。
     * 5. 调用方：API 使用状态监控或通知流程。
     * 6. 使用流程：属于系统通知流程，可被 Rule Engine API 使用状态服务间接触发。
     * 7. 线程安全：由实现保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口本身不直接涉及事务、MQTT、Actor、数据库；实现可能读取模板和配置缓存。
     */
    void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：测试指定租户邮件配置能否连接。
     * 2. 输入参数：tenantId 表示要测试的租户配置。
     * 3. 返回值：无；连接失败通过异常抛出。
     * 4. 调用时机：管理员保存或验证邮件配置时调用。
     * 5. 调用方：配置管理 API。
     * 6. 使用流程：属于邮件配置验证流程。
     * 7. 线程安全：由实现保证测试连接隔离性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor；实现可能读取数据库配置或缓存并进行 SMTP 连接。
     */
    void testConnection(TenantId tenantId) throws Exception;

    /**
     * 中文说明：
     * 1. 方法职责：判断指定租户邮件服务是否已配置。
     * 2. 输入参数：tenantId 表示租户边界。
     * 3. 返回值：true 表示可发送邮件，false 表示未配置或不可用。
     * 4. 调用时机：发送前校验或 UI 展示配置状态时调用。
     * 5. 调用方：邮件规则节点、通知中心、配置管理流程。
     * 6. 使用流程：属于 Rule Engine/通知发送前置校验。
     * 7. 线程安全：接口无状态，具体实现需保证配置读取并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现可能读取配置缓存。
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
