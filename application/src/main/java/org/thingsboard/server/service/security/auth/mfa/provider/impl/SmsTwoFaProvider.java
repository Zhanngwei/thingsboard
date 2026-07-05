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
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`SmsTwoFaProvider` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Service
@TbCoreComponent
public class SmsTwoFaProvider extends OtpBasedTwoFaProvider<SmsTwoFaProviderConfig, SmsTwoFaAccountConfig> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final SmsService smsService;
    private final AuditLogService auditLogService;

    /**
     * 功能：创建 `SmsTwoFaProvider` 实例，并初始化必要字段。
     * 参数：
     * - `cacheManager`：管理器对象。
     * - `smsService`：服务对象。
     * - `auditLogService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public SmsTwoFaProvider(CacheManager cacheManager, SmsService smsService, AuditLogService auditLogService) {
        super(cacheManager);
        this.smsService = smsService;
        this.auditLogService = auditLogService;
    }


    /**
     * 功能：执行 `generateNewAccountConfig` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerConfig`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public SmsTwoFaAccountConfig generateNewAccountConfig(User user, SmsTwoFaProviderConfig providerConfig) {
        return new SmsTwoFaAccountConfig();
    }

    /**
     * 功能：发送或提交编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `verificationCode`：`verificationCode` 参数。
     * - `providerConfig`：配置对象。
     * - `accountConfig`：配置对象。
     * 返回：无。
     */
    @Override
    protected void sendVerificationCode(SecurityUser user, String verificationCode, SmsTwoFaProviderConfig providerConfig, SmsTwoFaAccountConfig accountConfig) throws ThingsboardException {
        Map<String, String> messageData = Map.of(
                "code", verificationCode,
                "userEmail", user.getEmail()
        );
        String message = TbNodeUtils.processTemplate(providerConfig.getSmsVerificationMessageTemplate(), messageData);
        String phoneNumber = accountConfig.getPhoneNumber();
        try {
            smsService.sendSms(user.getTenantId(), user.getCustomerId(), new String[]{phoneNumber}, message);
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, null, phoneNumber);
        } catch (ThingsboardException e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, e, phoneNumber);
            throw e;
        }
    }

    /**
     * 功能：执行 `check` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void check(TenantId tenantId) throws ThingsboardException {
        if (!smsService.isConfigured(tenantId)) {
            throw new ThingsboardException("SMS service is not configured", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.SMS;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SmsTwoFaProvider` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
