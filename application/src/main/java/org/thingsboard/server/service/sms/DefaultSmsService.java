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
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.transport.DefaultTransportApiService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 中文说明：
 * 1. 类目的：`DefaultSmsService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
public class DefaultSmsService implements SmsService {

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final SmsSenderFactory smsSenderFactory;
    private final AdminSettingsService adminSettingsService;
    /**
     * 状态，提供当前类调用的业务操作。
     */
    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageReportClient apiUsageClient;

    /**
     * `smsSender` 字段，保存当前对象的对应属性。
     */
    private SmsSender smsSender;

    /**
     * 功能：创建 `DefaultSmsService` 实例，并初始化必要字段。
     * 参数：
     * - `smsSenderFactory`：`smsSenderFactory` 参数。
     * - `adminSettingsService`：服务对象。
     * - `apiUsageStateService`：服务对象。
     * - `apiUsageClient`：客户端对象。
     * 返回：新创建的对象实例。
     */
    public DefaultSmsService(SmsSenderFactory smsSenderFactory, AdminSettingsService adminSettingsService, TbApiUsageStateService apiUsageStateService, TbApiUsageReportClient apiUsageClient) {
        this.smsSenderFactory = smsSenderFactory;
        this.adminSettingsService = adminSettingsService;
        this.apiUsageStateService = apiUsageStateService;
        this.apiUsageClient = apiUsageClient;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        updateSmsConfiguration();
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (this.smsSender != null) {
            this.smsSender.destroy();
        }
    }

    /**
     * 功能：更新`Sms Configuration`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void updateSmsConfiguration() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "sms");
        if (settings != null) {
            try {
                JsonNode jsonConfig = settings.getJsonValue();
                SmsProviderConfiguration configuration = JacksonUtil.convertValue(jsonConfig, SmsProviderConfiguration.class);
                SmsSender newSmsSender = this.smsSenderFactory.createSmsSender(configuration);
                if (this.smsSender != null) {
                    this.smsSender.destroy();
                }
                this.smsSender = newSmsSender;
            } catch (Exception e) {
                log.error("Failed to create SMS sender", e);
            }
        }
    }

    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `numberTo`：`numberTo` 参数。
     * - `message`：待处理消息。
     * 返回：数值结果。
     */
    protected int sendSms(String numberTo, String message) throws ThingsboardException {
        if (this.smsSender == null) {
            throw new ThingsboardException("Unable to send SMS: no SMS provider configured!", ThingsboardErrorCode.GENERAL);
        }
        return this.sendSms(this.smsSender, numberTo, message);
    }

    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `numbersTo`：`numbersTo` 参数。
     * - `message`：待处理消息。
     * 返回：无。
     */
    @Override
    public void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws ThingsboardException {
        if (apiUsageStateService.getApiUsageState(tenantId).isSmsSendEnabled()) {
            int smsCount = 0;
            try {
                for (String numberTo : numbersTo) {
                    smsCount += this.sendSms(numberTo, message);
                }
            } finally {
                if (smsCount > 0) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.SMS_EXEC_COUNT, smsCount);
                }
            }
        } else {
            throw new RuntimeException("SMS sending is disabled due to API limits!");
        }
    }

    /**
     * 功能：发送或提交`Test Sms`。
     * 参数：
     * - `testSmsRequest`：请求对象。
     * 返回：无。
     */
    @Override
    public void sendTestSms(TestSmsRequest testSmsRequest) throws ThingsboardException {
        SmsSender testSmsSender;
        try {
            testSmsSender = this.smsSenderFactory.createSmsSender(testSmsRequest.getProviderConfiguration());
        } catch (Exception e) {
            throw handleException(e);
        }
        this.sendSms(testSmsSender, testSmsRequest.getNumberTo(), testSmsRequest.getMessage());
        testSmsSender.destroy();
    }

    /**
     * 功能：判断`Configured`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean isConfigured(TenantId tenantId) {
        return smsSender != null;
    }

    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `smsSender`：`smsSender` 参数。
     * - `numberTo`：`numberTo` 参数。
     * - `message`：待处理消息。
     * 返回：数值结果。
     */
    private int sendSms(SmsSender smsSender, String numberTo, String message) throws ThingsboardException {
        try {
            int sentSms = smsSender.sendSms(numberTo, message);
            log.trace("Successfully sent sms to number: {}", numberTo);
            return sentSms;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * 功能：处理`Exception`。
     * 参数：
     * - `exception`：`exception` 参数。
     * 返回：处理结果。
     */
    private ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send SMS: {}", message);
        return new ThingsboardException(String.format("Unable to send SMS: %s", message),
                ThingsboardErrorCode.GENERAL);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultSmsService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
