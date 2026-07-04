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
package org.thingsboard.server.controller;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TotpTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.OtpBasedTwoFaProvider;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.TotpTwoFaProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`TwoFactorAuthConfigTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class TwoFactorAuthConfigTest extends AbstractControllerTest {

    @SpyBean
    /**
     * 字段说明：
     * 1. 保存 `totpTwoFactorAuthProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TotpTwoFaProvider totpTwoFactorAuthProvider;
    @MockBean
    /**
     * 字段说明：
     * 1. 保存 `smsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private SmsService smsService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `cacheManager` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private CacheManager cacheManager;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `twoFaConfigManager` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TwoFaConfigManager twoFaConfigManager;
    @SpyBean
    /**
     * 字段说明：
     * 1. 保存 `twoFactorAuthService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TwoFactorAuthService twoFactorAuthService;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `beforeEach` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void beforeEach() throws Exception {
        doNothing().when(twoFactorAuthService).checkProvider(any(), any());
        loginSysAdmin();
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `afterEach` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void afterEach() {
        twoFaConfigManager.deletePlatformTwoFaSettings(TenantId.SYS_TENANT_ID);
        twoFaConfigManager.deletePlatformTwoFaSettings(tenantId);
    }


    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSavePlatformTwoFaSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSavePlatformTwoFaSettings() throws Exception {
        loginSysAdmin();

        TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");
        SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate("${code}");
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(List.of(totpTwoFaProviderConfig, smsTwoFaProviderConfig));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setVerificationCodeCheckRateLimit("3:900");
        twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(10);
        twoFaSettings.setTotalAllowedTimeForVerification(3600);

        doPost("/api/2fa/settings", twoFaSettings).andExpect(status().isOk());

        PlatformTwoFaSettings savedTwoFaSettings = readResponse(doGet("/api/2fa/settings").andExpect(status().isOk()), PlatformTwoFaSettings.class);

        assertThat(savedTwoFaSettings.getProviders()).hasSize(2);
        assertThat(savedTwoFaSettings.getProviders()).contains(totpTwoFaProviderConfig, smsTwoFaProviderConfig);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSavePlatformTwoFaSettings_validationError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSavePlatformTwoFaSettings_validationError() throws Exception {
        loginSysAdmin();

        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Collections.emptyList());
        twoFaSettings.setVerificationCodeCheckRateLimit("0:12");
        twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(-1);
        twoFaSettings.setTotalAllowedTimeForVerification(0);
        twoFaSettings.setMinVerificationCodeSendPeriod(5);

        String errorMessage = getErrorMessage(doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isBadRequest()));

        assertThat(errorMessage).contains(
                "verificationCodeCheckRateLimit is invalid",
                "maxVerificationFailuresBeforeUserLockout must be positive",
                "totalAllowedTimeForVerification must be greater than or equal to 60"
        );
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTotpTwoFaProviderConfig_validationError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTotpTwoFaProviderConfig_validationError() throws Exception {
        TotpTwoFaProviderConfig invalidTotpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        invalidTotpTwoFaProviderConfig.setIssuerName("   ");

        String errorResponse = savePlatformTwoFaSettingsAndGetError(invalidTotpTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("issuerName must not be blank");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveSmsTwoFaProviderConfig_validationError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveSmsTwoFaProviderConfig_validationError() throws Exception {
        SmsTwoFaProviderConfig invalidSmsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        invalidSmsTwoFaProviderConfig.setSmsVerificationMessageTemplate("does not contain verification code");
        invalidSmsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        String errorResponse = savePlatformTwoFaSettingsAndGetError(invalidSmsTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("must contain verification code");

        invalidSmsTwoFaProviderConfig.setSmsVerificationMessageTemplate(null);
        invalidSmsTwoFaProviderConfig.setVerificationCodeLifetime(0);
        errorResponse = savePlatformTwoFaSettingsAndGetError(invalidSmsTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("smsVerificationMessageTemplate is required");
        assertThat(errorResponse).containsIgnoringCase("verificationCodeLifetime is required");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `savePlatformTwoFaSettingsAndGetError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String savePlatformTwoFaSettingsAndGetError(TwoFaProviderConfig invalidTwoFaProviderConfig) throws Exception {
        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Collections.singletonList(invalidTwoFaProviderConfig));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);

        return getErrorMessage(doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isBadRequest()));
    }


    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTwoFaAccountConfig_providerNotConfigured` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTwoFaAccountConfig_providerNotConfigured() throws Exception {
        configureSmsTwoFaProvider("${code}");

        loginTenantAdmin();

        TwoFaProviderType notConfiguredProviderType = TwoFaProviderType.TOTP;
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/generate?providerType=" + notConfiguredProviderType)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("provider is not configured");

        TotpTwoFaAccountConfig notConfiguredProviderAccountConfig = new TotpTwoFaAccountConfig();
        notConfiguredProviderAccountConfig.setAuthUrl("otpauth://totp/aba:aba?issuer=aba&secret=ABA");
        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", notConfiguredProviderAccountConfig));
        assertThat(errorMessage).containsIgnoringCase("provider is not configured");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGenerateTotpTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGenerateTotpTwoFaAccountConfig() throws Exception {
        TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), String.class)).isNullOrEmpty();
        generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSubmitTotpTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSubmitTotpTwoFaAccountConfig() throws Exception {
        TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFaAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
        doPost("/api/2fa/account/config/submit", generatedTotpTwoFaAccountConfig).andExpect(status().isOk());
        verify(totpTwoFactorAuthProvider).prepareVerificationCode(argThat(user -> user.getEmail().equals(TENANT_ADMIN_EMAIL)),
                eq(totpTwoFaProviderConfig), eq(generatedTotpTwoFaAccountConfig));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSubmitTotpTwoFaAccountConfig_validationError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSubmitTotpTwoFaAccountConfig_validationError() throws Exception {
        configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFaAccountConfig totpTwoFaAccountConfig = new TotpTwoFaAccountConfig();
        totpTwoFaAccountConfig.setAuthUrl(null);

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("authUrl must not be blank");

        totpTwoFaAccountConfig.setAuthUrl("otpauth://totp/T B: aba");
        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("authUrl is invalid");

        totpTwoFaAccountConfig.setAuthUrl("otpauth://totp/ThingsBoard%20(Tenant):tenant@thingsboard.org?issuer=ThingsBoard+%28Tenant%29&secret=FUNBIM3CXFNNGQR6ZIPVWHP65PPFWDII");
        doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testVerifyAndSaveTotpTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testVerifyAndSaveTotpTwoFaAccountConfig() throws Exception {
        TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFaAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
        generatedTotpTwoFaAccountConfig.setUseByDefault(true);

        String secret = UriComponentsBuilder.fromUriString(generatedTotpTwoFaAccountConfig.getAuthUrl()).build()
                .getQueryParams().getFirst("secret");
        String correctVerificationCode = new Totp(secret).now();

        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, generatedTotpTwoFaAccountConfig)
                .andExpect(status().isOk());

        AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        assertThat(accountTwoFaSettings.getConfigs()).size().isOne();

        TwoFaAccountConfig twoFaAccountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.TOTP);
        assertThat(twoFaAccountConfig).isEqualTo(generatedTotpTwoFaAccountConfig);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testVerifyAndSaveTotpTwoFaAccountConfig_incorrectVerificationCode` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testVerifyAndSaveTotpTwoFaAccountConfig_incorrectVerificationCode() throws Exception {
        TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFaAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);

        String incorrectVerificationCode = "100000";
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=" + incorrectVerificationCode, generatedTotpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));

        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `generateTotpTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TotpTwoFaAccountConfig generateTotpTwoFaAccountConfig(TotpTwoFaProviderConfig totpTwoFaProviderConfig) throws Exception {
        TwoFaAccountConfig generatedTwoFaAccountConfig = readResponse(doPost("/api/2fa/account/config/generate?providerType=TOTP")
                .andExpect(status().isOk()), TwoFaAccountConfig.class);
        assertThat(generatedTwoFaAccountConfig).isInstanceOf(TotpTwoFaAccountConfig.class);

        assertThat(((TotpTwoFaAccountConfig) generatedTwoFaAccountConfig)).satisfies(accountConfig -> {
            UriComponents otpAuthUrl = UriComponentsBuilder.fromUriString(accountConfig.getAuthUrl()).build();
            assertThat(otpAuthUrl.getScheme()).isEqualTo("otpauth");
            assertThat(otpAuthUrl.getHost()).isEqualTo("totp");
            assertThat(otpAuthUrl.getQueryParams().getFirst("issuer")).isEqualTo(totpTwoFaProviderConfig.getIssuerName());
            assertThat(otpAuthUrl.getPath()).isEqualTo("/%s:%s", totpTwoFaProviderConfig.getIssuerName(), TENANT_ADMIN_EMAIL);
            assertThat(otpAuthUrl.getQueryParams().getFirst("secret")).satisfies(secretKey -> {
                assertDoesNotThrow(() -> Base32.decode(secretKey));
            });
        });
        return (TotpTwoFaAccountConfig) generatedTwoFaAccountConfig;
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetTwoFaAccountConfig_whenProviderNotConfigured` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetTwoFaAccountConfig_whenProviderNotConfigured() throws Exception {
        testVerifyAndSaveTotpTwoFaAccountConfig();
        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()),
                AccountTwoFaSettings.class).getConfigs()).isNotEmpty();

        loginSysAdmin();
        saveProvidersConfigs();

        loginTenantAdmin();

        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class).getConfigs())
                .isEmpty();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGenerateSmsTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGenerateSmsTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${code}");
        doPost("/api/2fa/account/config/generate?providerType=SMS")
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSubmitSmsTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSubmitSmsTwoFaAccountConfig() throws Exception {
        String verificationMessageTemplate = "Here is your verification code: ${code}";
        configureSmsTwoFaProvider(verificationMessageTemplate);

        loginTenantAdmin();

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38054159785");

        doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig).andExpect(status().isOk());

        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        String verificationCode = cacheManager.getCache(CacheConstants.TWO_FA_VERIFICATION_CODES_CACHE)
                .get(tenantAdminUserId, OtpBasedTwoFaProvider.Otp.class).getValue();

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(smsTwoFaAccountConfig.getPhoneNumber());
        }), eq("Here is your verification code: " + verificationCode));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSubmitSmsTwoFaAccountConfig_validationError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSubmitSmsTwoFaAccountConfig_validationError() throws Exception {
        configureSmsTwoFaProvider("${code}");

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        String blankPhoneNumber = "";
        smsTwoFaAccountConfig.setPhoneNumber(blankPhoneNumber);

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("phoneNumber must not be blank");

        String nonE164PhoneNumber = "8754868";
        smsTwoFaAccountConfig.setPhoneNumber(nonE164PhoneNumber);

        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("phoneNumber is not of E.164 format");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testVerifyAndSaveSmsTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testVerifyAndSaveSmsTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${code}");

        loginTenantAdmin();

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38051889445");
        smsTwoFaAccountConfig.setUseByDefault(true);

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig).andExpect(status().isOk());

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(smsTwoFaAccountConfig.getPhoneNumber());
        }), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();
        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, smsTwoFaAccountConfig)
                .andExpect(status().isOk());

        AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        TwoFaAccountConfig accountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.SMS);
        assertThat(accountConfig).isEqualTo(smsTwoFaAccountConfig);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testVerifyAndSaveSmsTwoFaAccountConfig_incorrectVerificationCode` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testVerifyAndSaveSmsTwoFaAccountConfig_incorrectVerificationCode() throws Exception {
        configureSmsTwoFaProvider("${code}");

        loginTenantAdmin();

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38051889445");

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=100000", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testVerifyAndSaveSmsTwoFaAccountConfig_differentAccountConfigs` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testVerifyAndSaveSmsTwoFaAccountConfig_differentAccountConfigs() throws Exception {
        configureSmsTwoFaProvider("${code}");
        loginTenantAdmin();

        SmsTwoFaAccountConfig initialSmsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        initialSmsTwoFaAccountConfig.setPhoneNumber("+38051889445");
        initialSmsTwoFaAccountConfig.setUseByDefault(true);

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/2fa/account/config/submit", initialSmsTwoFaAccountConfig).andExpect(status().isOk());

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(initialSmsTwoFaAccountConfig.getPhoneNumber());
        }), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();

        SmsTwoFaAccountConfig anotherSmsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        anotherSmsTwoFaAccountConfig.setPhoneNumber("+38111111111");
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, anotherSmsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");

        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, initialSmsTwoFaAccountConfig)
                .andExpect(status().isOk());
        AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        TwoFaAccountConfig accountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.SMS);
        assertThat(accountConfig).isEqualTo(initialSmsTwoFaAccountConfig);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `configureTotpTwoFaProvider` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TotpTwoFaProviderConfig configureTotpTwoFaProvider() throws Exception {
        TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");

        saveProvidersConfigs(totpTwoFaProviderConfig);
        return totpTwoFaProviderConfig;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `configureSmsTwoFaProvider` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private SmsTwoFaProviderConfig configureSmsTwoFaProvider(String verificationMessageTemplate) throws Exception {
        SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate(verificationMessageTemplate);
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        saveProvidersConfigs(smsTwoFaProviderConfig);
        return smsTwoFaProviderConfig;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveProvidersConfigs` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void saveProvidersConfigs(TwoFaProviderConfig... providerConfigs) throws Exception {
        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Arrays.stream(providerConfigs).collect(Collectors.toList()));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);
        doPost("/api/2fa/settings", twoFaSettings).andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testIsTwoFaEnabled` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testIsTwoFaEnabled() throws Exception {
        configureSmsTwoFaProvider("${code}");
        SmsTwoFaAccountConfig accountConfig = new SmsTwoFaAccountConfig();
        accountConfig.setPhoneNumber("+38050505050");
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, tenantAdminUserId, accountConfig);

        assertThat(twoFactorAuthService.isTwoFaEnabled(tenantId, tenantAdminUserId)).isTrue();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${code}");
        SmsTwoFaAccountConfig accountConfig = new SmsTwoFaAccountConfig();
        accountConfig.setPhoneNumber("+38050505050");

        loginTenantAdmin();

        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, tenantAdminUserId, accountConfig);

        AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        TwoFaAccountConfig savedAccountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.SMS);
        assertThat(savedAccountConfig).isEqualTo(accountConfig);

        doDelete("/api/2fa/account/config?providerType=SMS").andExpect(status().isOk());

        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class).getConfigs())
                .doesNotContainKey(TwoFaProviderType.SMS);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TwoFactorAuthConfigTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
