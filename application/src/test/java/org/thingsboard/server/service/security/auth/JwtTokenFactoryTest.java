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
package org.thingsboard.server.service.security.auth;

import io.jsonwebtoken.Claims;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.targets.platform.SystemAdministratorsFilter;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsService;
import org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsValidator;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`JwtTokenFactoryTest` 是ThingsBoard Application 测试模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class JwtTokenFactoryTest {

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private JwtTokenFactory tokenFactory;
    private AdminSettingsService adminSettingsService;
    /**
     * 通知，表示当前对象的对应属性。
     */
    private NotificationCenter notificationCenter;
    private JwtSettingsService jwtSettingsService;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private JwtSettings jwtSettings;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeEach() {
        jwtSettings = new JwtSettings();
        jwtSettings.setTokenIssuer("tb");
        jwtSettings.setTokenSigningKey("abewafaf");
        jwtSettings.setTokenExpirationTime((int) TimeUnit.HOURS.toSeconds(2));
        jwtSettings.setRefreshTokenExpTime((int) TimeUnit.DAYS.toSeconds(7));

        adminSettingsService = mock(AdminSettingsService.class);
        notificationCenter = mock(NotificationCenter.class);
        jwtSettingsService = mockJwtSettingsService();
        mockJwtSettings(jwtSettings);

        tokenFactory = new JwtTokenFactory(jwtSettingsService);
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAndParseAccessJwtToken() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(new UserId(UUID.randomUUID()));
        securityUser.setEmail("tenant@thingsboard.org");
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setTenantId(new TenantId(UUID.randomUUID()));
        securityUser.setEnabled(true);
        securityUser.setFirstName("A");
        securityUser.setLastName("B");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setCustomerId(new CustomerId(UUID.randomUUID()));

        testCreateAndParseAccessJwtToken(securityUser);

        securityUser = new SecurityUser(securityUser, true, new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, securityUser.getEmail()));
        securityUser.setFirstName(null);
        securityUser.setLastName(null);
        securityUser.setCustomerId(null);

        testCreateAndParseAccessJwtToken(securityUser);
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * 返回：无。
     */
    public void testCreateAndParseAccessJwtToken(SecurityUser securityUser) {
        AccessJwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
        checkExpirationTime(accessToken, jwtSettings.getTokenExpirationTime());

        SecurityUser parsedSecurityUser = tokenFactory.parseAccessJwtToken(accessToken.getToken());
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getEmail()).isEqualTo(securityUser.getEmail());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType().equals(securityUser.getUserPrincipal().getType())
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
        assertThat(parsedSecurityUser.getAuthorities()).isEqualTo(securityUser.getAuthorities());
        assertThat(parsedSecurityUser.isEnabled()).isEqualTo(securityUser.isEnabled());
        assertThat(parsedSecurityUser.getTenantId()).isEqualTo(securityUser.getTenantId());
        assertThat(parsedSecurityUser.getCustomerId()).isEqualTo(securityUser.getCustomerId());
        assertThat(parsedSecurityUser.getFirstName()).isEqualTo(securityUser.getFirstName());
        assertThat(parsedSecurityUser.getLastName()).isEqualTo(securityUser.getLastName());
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAndParseRefreshJwtToken() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(new UserId(UUID.randomUUID()));
        securityUser.setEmail("tenant@thingsboard.org");
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setEnabled(true);
        securityUser.setTenantId(new TenantId(UUID.randomUUID()));
        securityUser.setCustomerId(new CustomerId(UUID.randomUUID()));

        JwtToken refreshToken = tokenFactory.createRefreshToken(securityUser);
        checkExpirationTime(refreshToken, jwtSettings.getRefreshTokenExpTime());

        SecurityUser parsedSecurityUser = tokenFactory.parseRefreshToken(refreshToken.getToken());
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType().equals(securityUser.getUserPrincipal().getType())
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
        assertThat(parsedSecurityUser.getAuthority()).isNull();
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAndParsePreVerificationJwtToken() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(new UserId(UUID.randomUUID()));
        securityUser.setEmail("tenant@thingsboard.org");
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setEnabled(true);
        securityUser.setTenantId(new TenantId(UUID.randomUUID()));
        securityUser.setCustomerId(new CustomerId(UUID.randomUUID()));

        int tokenLifetime = (int) TimeUnit.MINUTES.toSeconds(30);
        JwtToken preVerificationToken = tokenFactory.createPreVerificationToken(securityUser, tokenLifetime);
        checkExpirationTime(preVerificationToken, tokenLifetime);

        SecurityUser parsedSecurityUser = tokenFactory.parseAccessJwtToken(preVerificationToken.getToken());
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getAuthority()).isEqualTo(Authority.PRE_VERIFICATION_TOKEN);
        assertThat(parsedSecurityUser.getTenantId()).isEqualTo(securityUser.getTenantId());
        assertThat(parsedSecurityUser.getCustomerId()).isEqualTo(securityUser.getCustomerId());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType() == UserPrincipal.Type.USER_NAME
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
    }

    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testJwtSigningKeyIssueNotification() {
        JwtSettings badJwtSettings = jwtSettings;
        badJwtSettings.setTokenSigningKey(JwtSettingsService.TOKEN_SIGNING_KEY_DEFAULT);
        mockJwtSettings(badJwtSettings);
        jwtSettingsService = mockJwtSettingsService();

        for (int i = 0; i < 5; i++) { // to check if notification is not sent twice
            jwtSettingsService.getJwtSettings();
        }
        verify(notificationCenter, times(1)).sendGeneralWebNotification(eq(TenantId.SYS_TENANT_ID),
                isA(SystemAdministratorsFilter.class), argThat(template -> template.getConfiguration().getDeliveryMethodsTemplates().get(NotificationDeliveryMethod.WEB)
                        .getBody().contains("The platform is configured to use default JWT Signing Key")));
    }

    /**
     * 功能：执行 `mockJwtSettings` 对应的处理。
     * 参数：
     * - `settings`：配置对象。
     * 返回：无。
     */
    private void mockJwtSettings(JwtSettings settings) {
        AdminSettings adminJwtSettings = new AdminSettings();
        adminJwtSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, JwtSettingsService.ADMIN_SETTINGS_JWT_KEY))
                .thenReturn(adminJwtSettings);
    }

    /**
     * 功能：执行 `mockJwtSettingsService` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private DefaultJwtSettingsService mockJwtSettingsService() {
        return new DefaultJwtSettingsService(adminSettingsService, Optional.empty(),
                Optional.of(notificationCenter), new DefaultJwtSettingsValidator());
    }

    /**
     * 功能：校验过期时间。
     * 参数：
     * - `jwtToken`：`jwtToken` 参数。
     * - `tokenLifetime`：`tokenLifetime` 参数。
     * 返回：无。
     */
    private void checkExpirationTime(JwtToken jwtToken, int tokenLifetime) {
        Claims claims = tokenFactory.parseTokenClaims(jwtToken.getToken()).getBody();
        assertThat(claims.getExpiration()).matches(actualExpirationTime -> {
            Calendar expirationTime = Calendar.getInstance();
            expirationTime.setTime(new Date());
            expirationTime.add(Calendar.SECOND, tokenLifetime);
            if (actualExpirationTime.equals(expirationTime.getTime())) {
                return true;
            } else if (actualExpirationTime.before(expirationTime.getTime())) {
                int gap = 2;
                expirationTime.add(Calendar.SECOND, -gap);
                return actualExpirationTime.after(expirationTime.getTime());
            } else {
                return false;
            }
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JwtTokenFactoryTest` 在 ThingsBoard Application 测试模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
