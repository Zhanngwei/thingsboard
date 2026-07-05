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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.mail.MailOauth2Provider.OFFICE_365;

/**
 * 中文说明：
 * 1. 类目的：`RefreshTokenExpCheckService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenExpCheckService {
    /**
     * 令牌常量，用于统一引用固定值。
     */
    public static final int AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS = 90;
    private final AdminSettingsService adminSettingsService;

    /**
     * 功能：执行 `check` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${mail.oauth2.refreshTokenCheckingInterval})}",
            fixedDelayString = "${mail.oauth2.refreshTokenCheckingInterval}",
            timeUnit = TimeUnit.SECONDS)
    public void check() throws IOException {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        if (settings != null && settings.getJsonValue().has("enableOauth2") && settings.getJsonValue().get("enableOauth2").asBoolean()) {
            JsonNode jsonValue = settings.getJsonValue();
            if (OFFICE_365.name().equals(jsonValue.get("providerId").asText()) && jsonValue.has("refreshToken")
                    && jsonValue.has("refreshTokenExpires")) {
                try {
                    long expiresIn = jsonValue.get("refreshTokenExpires").longValue();
                    long tokenLifeDuration = expiresIn - System.currentTimeMillis();
                    if (tokenLifeDuration < 0) {
                        ((ObjectNode) jsonValue).put("tokenGenerated", false);
                        ((ObjectNode) jsonValue).remove("refreshToken");
                        ((ObjectNode) jsonValue).remove("refreshTokenExpires");

                        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, settings);
                    } else if (tokenLifeDuration < 604800000L) { //less than 7 days
                        log.info("Trying to refresh refresh token.");

                        String clientId = jsonValue.get("clientId").asText();
                        String clientSecret = jsonValue.get("clientSecret").asText();
                        String refreshToken = jsonValue.get("refreshToken").asText();
                        String tokenUri = jsonValue.get("tokenUri").asText();

                        TokenResponse tokenResponse = new RefreshTokenRequest(new NetHttpTransport(), new GsonFactory(),
                                new GenericUrl(tokenUri), refreshToken)
                                .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                                .execute();
                        ((ObjectNode) jsonValue).put("refreshToken", tokenResponse.getRefreshToken());
                        ((ObjectNode) jsonValue).put("refreshTokenExpires", Instant.now().plus(Duration.ofDays(AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS)).toEpochMilli());
                        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, settings);
                    }
                } catch (Exception e) {
                    log.error("Error occurred while checking token", e);
                }
            }
        }
    }

/*
 * 本类总结：
 * 1. 核心职责：`RefreshTokenExpCheckService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}