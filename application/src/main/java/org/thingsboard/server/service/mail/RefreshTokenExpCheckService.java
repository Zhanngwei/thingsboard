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
 * 1. `RefreshTokenExpCheckService` 是 ThingsBoard Application 中负责 `Refresh Token Exp` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
}