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
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 中文说明：
 * 1. 类目的：`DefaultMailService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
public class DefaultMailService implements MailService {

    /**
     * 邮箱常量，用于统一引用固定值。
     */
    public static final String TARGET_EMAIL = "targetEmail";
    public static final String UTF_8 = "UTF-8";

    /**
     * `messages` 字段，保存当前对象的对应属性。
     */
    private final MessageSource messages;
    private final Configuration freemarkerConfig;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AdminSettingsService adminSettingsService;
    private final TbApiUsageReportClient apiUsageClient;

    /**
     * 超时时间常量，用于统一引用固定值。
     */
    private static final long DEFAULT_TIMEOUT = 10_000;

    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private TbApiUsageStateService apiUsageStateService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private MailExecutorService mailExecutorService;

    /**
     * 密码集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private PasswordResetExecutorService passwordResetExecutorService;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private TbMailContextComponent tbMailContextComponent;

    /**
     * `mailSender` 字段，保存当前对象的对应属性。
     */
    private TbMailSender mailSender;

    /**
     * `mailFrom` 字段，保存当前对象的对应属性。
     */
    private String mailFrom;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private long timeout;

    /**
     * 功能：创建 `DefaultMailService` 实例，并初始化必要字段。
     * 参数：
     * - `messages`：待处理消息。
     * - `freemarkerConfig`：配置对象。
     * - `adminSettingsService`：服务对象。
     * - `apiUsageClient`：客户端对象。
     * 返回：新创建的对象实例。
     */
    public DefaultMailService(MessageSource messages, Configuration freemarkerConfig, AdminSettingsService adminSettingsService, TbApiUsageReportClient apiUsageClient) {
        this.messages = messages;
        this.freemarkerConfig = freemarkerConfig;
        this.adminSettingsService = adminSettingsService;
        this.apiUsageClient = apiUsageClient;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        updateMailConfiguration();
    }

    /**
     * 功能：更新`Mail Configuration`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void updateMailConfiguration() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        if (settings != null) {
            JsonNode jsonConfig = settings.getJsonValue();
            mailSender = new TbMailSender(tbMailContextComponent, jsonConfig);
            mailFrom = jsonConfig.get("mailFrom").asText();
            timeout = jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);
        } else {
            throw new IncorrectParameterException("Failed to update mail configuration. Settings not found!");
        }
    }

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * - `subject`：`subject` 参数。
     * - `message`：待处理消息。
     * 返回：无。
     */
    @Override
    public void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException {
        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：发送或提交`Test Mail`。
     * 参数：
     * - `jsonConfig`：配置对象。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @Override
    public void sendTestMail(JsonNode jsonConfig, String email) throws ThingsboardException {
        TbMailSender testMailSender = new TbMailSender(tbMailContextComponent, jsonConfig);
        String mailFrom = jsonConfig.get("mailFrom").asText();
        String subject = messages.getMessage("test.message.subject", null, Locale.US);
        long timeout = jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);

        Map<String, Object> model = new HashMap<>();
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("test.ftl", model);

        sendMail(testMailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `activationLink`：`activationLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @Override
    public void sendActivationEmail(String activationLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("activation.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("activationLink", activationLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("activation.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `loginLink`：`loginLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @Override
    public void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("account.activated.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("account.activated.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：发送或提交密码。
     * 参数：
     * - `passwordResetLink`：`passwordResetLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @Override
    public void sendResetPasswordEmail(String passwordResetLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("reset.password.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("passwordResetLink", passwordResetLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("reset.password.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：发送或提交密码。
     * 参数：
     * - `passwordResetLink`：`passwordResetLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @Override
    public void sendResetPasswordEmailAsync(String passwordResetLink, String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(passwordResetLink, email);
            } catch (Exception e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    /**
     * 功能：发送或提交密码。
     * 参数：
     * - `loginLink`：`loginLink` 参数。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @Override
    public void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("password.was.reset.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("password.was.reset.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `tbEmail`：`tbEmail` 参数。
     * 返回：无。
     */
    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException {
        sendMail(tenantId, customerId, tbEmail, this.mailSender, timeout);
    }

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
    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException {
        sendMail(tenantId, customerId, tbEmail, javaMailSender, timeout);
    }

    /**
     * 功能：发送或提交`Mail`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `tbEmail`：`tbEmail` 参数。
     * - `javaMailSender`：`javaMailSender` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void sendMail(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException {
        if (apiUsageStateService.getApiUsageState(tenantId).isEmailSendEnabled()) {
            try {
                MimeMessage mailMsg = javaMailSender.createMimeMessage();
                boolean multipart = (tbEmail.getImages() != null && !tbEmail.getImages().isEmpty());
                MimeMessageHelper helper = new MimeMessageHelper(mailMsg, multipart, "UTF-8");
                helper.setFrom(StringUtils.isBlank(tbEmail.getFrom()) ? mailFrom : tbEmail.getFrom());
                helper.setTo(tbEmail.getTo().split("\\s*,\\s*"));
                if (!StringUtils.isBlank(tbEmail.getCc())) {
                    helper.setCc(tbEmail.getCc().split("\\s*,\\s*"));
                }
                if (!StringUtils.isBlank(tbEmail.getBcc())) {
                    helper.setBcc(tbEmail.getBcc().split("\\s*,\\s*"));
                }
                helper.setSubject(tbEmail.getSubject());
                helper.setText(tbEmail.getBody(), tbEmail.isHtml());

                if (multipart) {
                    for (String imgId : tbEmail.getImages().keySet()) {
                        String imgValue = tbEmail.getImages().get(imgId);
                        String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                        byte[] bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(value);
                        String contentType = helper.getFileTypeMap().getContentType(imgId);
                        InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                        helper.addInline(imgId, iss, contentType);
                    }
                }
                sendMailWithTimeout(javaMailSender, helper.getMimeMessage(), timeout);
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.EMAIL_EXEC_COUNT, 1);
            } catch (Exception e) {
                throw handleException(e);
            }
        } else {
            throw new RuntimeException("Email sending is disabled due to API limits!");
        }
    }

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `lockoutEmail`：`lockoutEmail` 参数。
     * - `email`：`email` 参数。
     * - `maxFailedLoginAttempts`：`maxFailedLoginAttempts` 参数。
     * 返回：无。
     */
    @Override
    public void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        String subject = messages.getMessage("account.lockout.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("lockoutAccount", lockoutEmail);
        model.put("maxFailedLoginAttempts", maxFailedLoginAttempts);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("account.lockout.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * - `verificationCode`：`verificationCode` 参数。
     * - `expirationTimeSeconds`：`expirationTimeSeconds` 参数。
     * 返回：无。
     */
    @Override
    public void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException {
        String subject = messages.getMessage("2fa.verification.code.subject", null, Locale.US);
        String message = mergeTemplateIntoString("2fa.verification.code.ftl", Map.of(
                TARGET_EMAIL, email,
                "code", verificationCode,
                "expirationTimeSeconds", expirationTimeSeconds
        ));

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：发送或提交状态。
     * 参数：
     * - `apiFeature`：`apiFeature` 参数。
     * - `stateValue`：值。
     * - `email`：`email` 参数。
     * - `recordState`：`recordState` 参数。
     * 返回：无。
     */
    @Override
    public void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException {
        String subject = messages.getMessage("api.usage.state", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("apiFeature", apiFeature.getLabel());
        model.put(TARGET_EMAIL, email);

        String message = null;

        switch (stateValue) {
            case ENABLED:
                model.put("apiLabel", toEnabledValueLabel(apiFeature));
                message = mergeTemplateIntoString("state.enabled.ftl", model);
                break;
            case WARNING:
                model.put("apiValueLabel", toDisabledValueLabel(apiFeature) + " " + toWarningValueLabel(recordState));
                message = mergeTemplateIntoString("state.warning.ftl", model);
                break;
            case DISABLED:
                model.put("apiLimitValueLabel", toDisabledValueLabel(apiFeature) + " " + toDisabledValueLabel(recordState));
                message = mergeTemplateIntoString("state.disabled.ftl", model);
                break;
        }
        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    /**
     * 功能：验证`Connection`相关场景。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void testConnection(TenantId tenantId) throws Exception {
        mailSender.testConnection();
    }

    /**
     * 功能：判断`Configured`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean isConfigured(TenantId tenantId) {
        return mailSender != null;
    }

    /**
     * 功能：执行 `toEnabledValueLabel` 对应的处理。
     * 参数：
     * - `apiFeature`：`apiFeature` 参数。
     * 返回：文本结果。
     */
    private String toEnabledValueLabel(ApiFeature apiFeature) {
        switch (apiFeature) {
            case DB:
                return "save";
            case TRANSPORT:
                return "receive";
            case JS:
                return "invoke";
            case RE:
                return "process";
            case EMAIL:
            case SMS:
                return "send";
            case ALARM:
                return "create";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    /**
     * 功能：执行 `toDisabledValueLabel` 对应的处理。
     * 参数：
     * - `apiFeature`：`apiFeature` 参数。
     * 返回：文本结果。
     */
    private String toDisabledValueLabel(ApiFeature apiFeature) {
        switch (apiFeature) {
            case DB:
                return "saved";
            case TRANSPORT:
                return "received";
            case JS:
                return "invoked";
            case RE:
                return "processed";
            case EMAIL:
            case SMS:
                return "sent";
            case ALARM:
                return "created";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    /**
     * 功能：执行 `toWarningValueLabel` 对应的处理。
     * 参数：
     * - `recordState`：`recordState` 参数。
     * 返回：文本结果。
     */
    private String toWarningValueLabel(ApiUsageRecordState recordState) {
        String valueInM = recordState.getValueAsString();
        String thresholdInM = recordState.getThresholdAsString();
        switch (recordState.getKey()) {
            case STORAGE_DP_COUNT:
            case TRANSPORT_DP_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed data points";
            case TRANSPORT_MSG_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed messages";
            case JS_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed JavaScript functions";
            case TBEL_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed Tbel functions";
            case RE_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed Rule Engine messages";
            case EMAIL_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed Email messages";
            case SMS_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed SMS messages";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    /**
     * 功能：执行 `toDisabledValueLabel` 对应的处理。
     * 参数：
     * - `recordState`：`recordState` 参数。
     * 返回：文本结果。
     */
    private String toDisabledValueLabel(ApiUsageRecordState recordState) {
        switch (recordState.getKey()) {
            case STORAGE_DP_COUNT:
            case TRANSPORT_DP_COUNT:
                return recordState.getValueAsString() + " data points";
            case TRANSPORT_MSG_COUNT:
                return recordState.getValueAsString() + " messages";
            case JS_EXEC_COUNT:
                return "JavaScript functions " + recordState.getValueAsString() + " times";
            case TBEL_EXEC_COUNT:
                return "TBEL functions " + recordState.getValueAsString() + " times";
            case RE_EXEC_COUNT:
                return recordState.getValueAsString() + " Rule Engine messages";
            case EMAIL_EXEC_COUNT:
                return recordState.getValueAsString() + " Email messages";
            case SMS_EXEC_COUNT:
                return recordState.getValueAsString() + " SMS messages";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    /**
     * 功能：发送或提交`Mail`。
     * 参数：
     * - `mailSender`：`mailSender` 参数。
     * - `mailFrom`：`mailFrom` 参数。
     * - `email`：`email` 参数。
     * - `subject`：`subject` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void sendMail(JavaMailSenderImpl mailSender, String mailFrom, String email,
                          String subject, String message, long timeout) throws ThingsboardException {
        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, UTF_8);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);

            sendMailWithTimeout(mailSender, helper.getMimeMessage(), timeout);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * 功能：发送或提交超时时间。
     * 参数：
     * - `mailSender`：`mailSender` 参数。
     * - `msg`：待处理消息。
     * - `timeout`：`timeout` 参数。
     * 返回：无。
     */
    private void sendMailWithTimeout(JavaMailSender mailSender, MimeMessage msg, long timeout) {
        try {
            mailExecutorService.submit(() -> mailSender.send(msg)).get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.debug("Error during mail submission", e);
            throw new RuntimeException("Timeout!");
        } catch (Exception e) {
            throw new RuntimeException(ExceptionUtils.getRootCause(e));
        }
    }

    /**
     * 功能：执行 `mergeTemplateIntoString` 对应的处理。
     * 参数：
     * - `templateLocation`：`templateLocation` 参数。
     * - `model`：键值映射。
     * 返回：文本结果。
     */
    private String mergeTemplateIntoString(String templateLocation,
                                           Map<String, Object> model) throws ThingsboardException {
        try {
            Template template = freemarkerConfig.getTemplate(templateLocation);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
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
    protected ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send mail: {}", message);
        return new ThingsboardException(String.format("Unable to send mail: %s", message),
                ThingsboardErrorCode.GENERAL);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultMailService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
