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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mail.MailOauth2Provider;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.service.mail.RefreshTokenExpCheckService.AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS;

/**
 * 中文说明：
 * 1. 类目的：`TbMailSender` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public class TbMailSender extends JavaMailSenderImpl {

    /**
     * `MAIL_PROP`常量，用于统一引用固定值。
     */
    private static final String MAIL_PROP = "mail.";
    private final TbMailContextComponent ctx;
    /**
     * 锁，用于保护并发读写的共享状态。
     */
    private final Lock lock;

    /**
     * 是否启用`oauth2`。
     */
    private final Boolean oauth2Enabled;
    private volatile String accessToken;
    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    private volatile long tokenExpires;

    /**
     * 功能：创建 `TbMailSender` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `jsonConfig`：配置对象。
     * 返回：新创建的对象实例。
     */
    public TbMailSender(TbMailContextComponent ctx, JsonNode jsonConfig) {
        super();
        this.lock = new ReentrantLock();
        this.tokenExpires = 0L;
        this.ctx = ctx;
        this.oauth2Enabled = jsonConfig.has("enableOauth2") && jsonConfig.get("enableOauth2").asBoolean();

        setHost(jsonConfig.get("smtpHost").asText());
        setPort(parsePort(jsonConfig.get("smtpPort").asText()));
        setUsername(jsonConfig.get("username").asText());
        if (jsonConfig.has("password")) {
            setPassword(jsonConfig.get("password").asText());
        }
        setJavaMailProperties(createJavaMailProperties(jsonConfig));
    }

    /**
     * 功能：获取`Oauth2 Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public Boolean getOauth2Enabled() {
        return oauth2Enabled;
    }

    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getTokenExpires() {
        return tokenExpires;
    }

    /**
     * 功能：执行 `doSend` 对应的处理。
     * 参数：
     * - `mimeMessages`：待处理消息。
     * - `originalMessages`：待处理消息。
     * 返回：无。
     */
    @Override
    protected void doSend(MimeMessage[] mimeMessages, @Nullable Object[] originalMessages) throws MailException {
        updateOauth2PasswordIfExpired();
        doSendSuper(mimeMessages, originalMessages);
    }

    /**
     * 功能：执行 `doSendSuper` 对应的处理。
     * 参数：
     * - `mimeMessages`：待处理消息。
     * - `originalMessages`：待处理消息。
     * 返回：无。
     */
    public void doSendSuper(MimeMessage[] mimeMessages, Object[] originalMessages) {
        super.doSend(mimeMessages, originalMessages);
    }

    /**
     * 功能：验证`Connection`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void testConnection() throws MessagingException {
        updateOauth2PasswordIfExpired();
        testConnectionSuper();
    }

    /**
     * 功能：验证`Connection Super`相关场景。
     * 参数：无。
     * 返回：无。
     */
    public void testConnectionSuper() throws MessagingException {
        super.testConnection();
    }

    /**
     * 功能：更新密码。
     * 参数：无。
     * 返回：无。
     */
    public void updateOauth2PasswordIfExpired()  {
        if (getOauth2Enabled() && (System.currentTimeMillis() > getTokenExpires())){
            refreshAccessToken();
            setPassword(accessToken);
        }
    }

    /**
     * 功能：保存或创建`Java Mail Properties`。
     * 参数：
     * - `jsonConfig`：配置对象。
     * 返回：处理结果。
     */
    private Properties createJavaMailProperties(JsonNode jsonConfig) {
        Properties javaMailProperties = new Properties();
        String protocol = jsonConfig.get("smtpProtocol").asText();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", jsonConfig.get("smtpHost").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".port", jsonConfig.get("smtpPort").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", jsonConfig.get("timeout").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(jsonConfig.get("username").asText())));
        boolean enableTls = false;
        if (jsonConfig.has("enableTls")) {
            if (jsonConfig.get("enableTls").isBoolean() && jsonConfig.get("enableTls").booleanValue()) {
                enableTls = true;
            } else if (jsonConfig.get("enableTls").isTextual()) {
                enableTls = "true".equalsIgnoreCase(jsonConfig.get("enableTls").asText());
            }
        }
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", enableTls);
        if (enableTls && jsonConfig.has("tlsVersion") && !jsonConfig.get("tlsVersion").isNull()) {
            String tlsVersion = jsonConfig.get("tlsVersion").asText();
            if (StringUtils.isNoneEmpty(tlsVersion)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".ssl.protocols", tlsVersion);
            }
        }

        boolean enableProxy = jsonConfig.has("enableProxy") && jsonConfig.get("enableProxy").asBoolean();

        if (enableProxy) {
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.host", jsonConfig.get("proxyHost").asText());
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.port", jsonConfig.get("proxyPort").asText());
            String proxyUser = jsonConfig.get("proxyUser").asText();
            if (StringUtils.isNoneEmpty(proxyUser)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.user", proxyUser);
            }
            String proxyPassword = jsonConfig.get("proxyPassword").asText();
            if (StringUtils.isNoneEmpty(proxyPassword)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.password", proxyPassword);
            }
        }

        if (oauth2Enabled) {
            javaMailProperties.put(MAIL_PROP + protocol + ".auth.mechanisms", "XOAUTH2");
        }
        return javaMailProperties;
    }

    /**
     * 功能：更新令牌。
     * 参数：无。
     * 返回：无。
     */
    public void refreshAccessToken() {
        lock.lock();
        try {
            if (System.currentTimeMillis() > getTokenExpires()) {
                AdminSettings settings = ctx.getAdminSettingsService().findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
                JsonNode jsonValue = settings.getJsonValue();

                String clientId = jsonValue.get("clientId").asText();
                String clientSecret = jsonValue.get("clientSecret").asText();
                String refreshToken = jsonValue.get("refreshToken").asText();
                String tokenUri = jsonValue.get("tokenUri").asText();
                String providerId = jsonValue.get("providerId").asText();

                TokenResponse tokenResponse = new RefreshTokenRequest(new NetHttpTransport(), new GsonFactory(),
                        new GenericUrl(tokenUri), refreshToken)
                        .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                        .execute();
                if (MailOauth2Provider.OFFICE_365.name().equals(providerId)) {
                    ((ObjectNode)jsonValue).put("refreshToken", tokenResponse.getRefreshToken());
                    ((ObjectNode)jsonValue).put("refreshTokenExpires", Instant.now().plus(Duration.ofDays(AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS)).toEpochMilli());
                    ctx.getAdminSettingsService().saveAdminSettings(TenantId.SYS_TENANT_ID, settings);
                }
                accessToken = tokenResponse.getAccessToken();
                tokenExpires = System.currentTimeMillis() + (tokenResponse.getExpiresInSeconds().intValue() * 1000);
            }
        } catch (Exception e) {
            log.error("Unable to retrieve access token: {}", e.getMessage());
            throw new RuntimeException("Error while retrieving access token: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：解析端口号。
     * 参数：
     * - `strPort`：`strPort` 参数。
     * 返回：数值结果。
     */
    private int parsePort(String strPort) {
        try {
            return Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterException(String.format("Invalid smtp port value: %s", strPort));
        }
    }

/*
 * 本类总结：
 * 1. 核心职责：`TbMailSender` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}