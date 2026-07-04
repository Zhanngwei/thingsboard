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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.EmailTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.EmailTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TotpTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`TwoFactorAuthTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class TwoFactorAuthTest extends AbstractControllerTest {

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
     * 1. 保存 `auditLogService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AuditLogService auditLogService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `userService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private UserService userService;

    /**
     * 字段说明：
     * 1. 保存 `user` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private User user;
    private String username;
    /**
     * 字段说明：
     * 1. 保存 `password` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String password;

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
        username = "mfa@tb.io";
        password = "psswrd";

        user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail(username);
        user.setTenantId(tenantId);

        loginSysAdmin();
        user = createUser(user, password);
        doNothing().when(twoFactorAuthService).checkProvider(any(), any());
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
        twoFaConfigManager.deletePlatformTwoFaSettings(tenantId);
        twoFaConfigManager.deletePlatformTwoFaSettings(TenantId.SYS_TENANT_ID);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTwoFa_totp` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTwoFa_totp() throws Exception {
        TotpTwoFaAccountConfig totpTwoFaAccountConfig = configureTotpTwoFa();

        logInWithPreVerificationToken(username, password);

        doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                .andExpect(status().isOk());

        String correctVerificationCode = getCorrectTotp(totpTwoFaAccountConfig);

        JsonNode tokenPair = readResponse(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + correctVerificationCode)
                .andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenPair, username);

        User currentUser = readResponse(doGet("/api/auth/user")
                .andExpect(status().isOk()), User.class);
        assertThat(currentUser.getId()).isEqualTo(user.getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTwoFa_sms` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTwoFa_sms() throws Exception {
        configureSmsTwoFa();

        logInWithPreVerificationToken(username, password);

        doPost("/api/auth/2fa/verification/send?providerType=SMS")
                .andExpect(status().isOk());

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendSms(eq(tenantId), any(), any(), verificationCodeCaptor.capture());
        String correctVerificationCode = verificationCodeCaptor.getValue();

        JsonNode tokenPair = readResponse(doPost("/api/auth/2fa/verification/check?providerType=SMS&verificationCode=" + correctVerificationCode)
                .andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenPair, username);

        User currentUser = readResponse(doGet("/api/auth/user")
                .andExpect(status().isOk()), User.class);
        assertThat(currentUser.getId()).isEqualTo(user.getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTwoFaPreVerificationTokenLifetime` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTwoFaPreVerificationTokenLifetime() throws Exception {
        configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setTotalAllowedTimeForVerification(65);
        });

        logInWithPreVerificationToken(username, password);

        await("expiration of the pre-verification token")
                .atLeast(Duration.ofSeconds(30).plusMillis(500))
                .atMost(Duration.ofSeconds(70))
                .untilAsserted(() -> {
                    doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                            .andExpect(status().isUnauthorized());
                });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCheckVerificationCode_userBlocked` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCheckVerificationCode_userBlocked() throws Exception {
        configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(10);
        });

        logInWithPreVerificationToken(username, password);

        Stream.generate(() -> StringUtils.randomNumeric(6))
                .limit(9)
                .forEach(incorrectVerificationCode -> {
                    try {
                        String errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + incorrectVerificationCode)
                                .andExpect(status().isBadRequest()));
                        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
                    // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                    } catch (Exception e) {
                        fail();
                    }
                });

        String errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + StringUtils.randomNumeric(6))
                .andExpect(status().isUnauthorized()));
        assertThat(errorMessage).containsIgnoringCase("account was locked due to exceeded 2fa verification attempts");

        errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + StringUtils.randomNumeric(6))
                .andExpect(status().isUnauthorized()));
        assertThat(errorMessage).containsIgnoringCase("user is disabled");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSendVerificationCode_rateLimit` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSendVerificationCode_rateLimit() throws Exception {
        configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setMinVerificationCodeSendPeriod(10);
        });

        logInWithPreVerificationToken(username, password);

        doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                .andExpect(status().isOk());

        String rateLimitExceededError = getErrorMessage(doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                .andExpect(status().isTooManyRequests()));
        assertThat(rateLimitExceededError).containsIgnoringCase("too many requests");

        await("verification code sending rate limit resetting")
                .atLeast(Duration.ofSeconds(8))
                .atMost(Duration.ofSeconds(12))
                .untilAsserted(() -> {
                    doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                            .andExpect(status().isOk());
                });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCheckVerificationCode_rateLimit` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCheckVerificationCode_rateLimit() throws Exception {
        TotpTwoFaAccountConfig totpTwoFaAccountConfig = configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setVerificationCodeCheckRateLimit("3:10");
        });

        logInWithPreVerificationToken(username, password);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 3; i++) {
            String incorrectVerificationCodeError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                    .andExpect(status().isBadRequest()));
            assertThat(incorrectVerificationCodeError).containsIgnoringCase("verification code is incorrect");
        }

        String rateLimitExceededError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                .andExpect(status().isTooManyRequests()));
        assertThat(rateLimitExceededError).containsIgnoringCase("too many requests");

        await("verification code checking rate limit resetting")
                .atLeast(Duration.ofSeconds(8))
                .atMost(Duration.ofSeconds(12))
                .untilAsserted(() -> {
                    String incorrectVerificationCodeError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                            .andExpect(status().isBadRequest()));
                    assertThat(incorrectVerificationCodeError).containsIgnoringCase("verification code is incorrect");
                });

        doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + getCorrectTotp(totpTwoFaAccountConfig))
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCheckVerificationCode_invalidVerificationCode` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCheckVerificationCode_invalidVerificationCode() throws Exception {
        configureTotpTwoFa();
        logInWithPreVerificationToken(username, password);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (String invalidVerificationCode : new String[]{"1234567", "ab1212", "12311 ", "oewkriwejqf"}) {
            String errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + invalidVerificationCode)
                    .andExpect(status().isBadRequest()));
            assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCheckVerificationCode_codeExpiration` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCheckVerificationCode_codeExpiration() throws Exception {
        configureSmsTwoFa(smsTwoFaProviderConfig -> {
            smsTwoFaProviderConfig.setVerificationCodeLifetime(10);
        });

        logInWithPreVerificationToken(username, password);

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/auth/2fa/verification/send?providerType=SMS").andExpect(status().isOk());
        verify(smsService).sendSms(eq(tenantId), any(), any(), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();

        await("verification code expiration")
                .pollDelay(10, TimeUnit.SECONDS)
                .atLeast(10, TimeUnit.SECONDS)
                .atMost(12, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String incorrectVerificationCodeError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=SMS&verificationCode=" + correctVerificationCode)
                            .andExpect(status().isBadRequest()));
                    assertThat(incorrectVerificationCodeError).containsIgnoringCase("verification code is incorrect");
                });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTwoFa_logLoginAction` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTwoFa_logLoginAction() throws Exception {
        TotpTwoFaAccountConfig totpTwoFaAccountConfig = configureTotpTwoFa();

        logInWithPreVerificationToken(username, password);
        await("async audit log saving").during(1, TimeUnit.SECONDS);
        assertThat(getLogInAuditLogs()).isEmpty();
        assertThat(userService.findUserById(tenantId, user.getId()).getAdditionalInfo()
                .get("lastLoginTs")).isNull();

        doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                .andExpect(status().isBadRequest());

        await("async audit log saving").atMost(1, TimeUnit.SECONDS)
                .until(() -> getLogInAuditLogs().size() == 1);
        assertThat(getLogInAuditLogs().get(0)).satisfies(failedLogInAuditLog -> {
            assertThat(failedLogInAuditLog.getActionStatus()).isEqualTo(ActionStatus.FAILURE);
            assertThat(failedLogInAuditLog.getActionFailureDetails()).containsIgnoringCase("verification code is incorrect");
            assertThat(failedLogInAuditLog.getUserName()).isEqualTo(username);
        });

        doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + getCorrectTotp(totpTwoFaAccountConfig))
                .andExpect(status().isOk());
        await("async audit log saving").atMost(1, TimeUnit.SECONDS)
                .until(() -> getLogInAuditLogs().size() == 2);
        assertThat(getLogInAuditLogs().get(0)).satisfies(successfulLogInAuditLog -> {
            assertThat(successfulLogInAuditLog.getActionStatus()).isEqualTo(ActionStatus.SUCCESS);
            assertThat(successfulLogInAuditLog.getUserName()).isEqualTo(username);
        });
        assertThat(userService.findUserById(tenantId, user.getId()).getAdditionalInfo()
                .get("lastLoginTs").asLong())
                .isGreaterThan(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(3));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getLogInAuditLogs` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<AuditLog> getLogInAuditLogs() {
        return auditLogService.findAuditLogsByTenantIdAndUserId(tenantId, user.getId(), List.of(ActionType.LOGIN),
                new TimePageLink(new PageLink(10, 0, null, new SortOrder("createdTime", SortOrder.Direction.DESC)), 0L, System.currentTimeMillis())).getData();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAuthWithoutTwoFaAccountConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAuthWithoutTwoFaAccountConfig() throws ThingsboardException {
        configureTotpTwoFa();
        twoFaConfigManager.deleteTwoFaAccountConfig(tenantId, user.getId(), TwoFaProviderType.TOTP);

        assertDoesNotThrow(() -> {
            login(username, password);
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTwoFa_multipleProviders` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTwoFa_multipleProviders() throws Exception {
        PlatformTwoFaSettings platformTwoFaSettings = new PlatformTwoFaSettings();

        TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("TB");

        SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate("${code}");

        EmailTwoFaProviderConfig emailTwoFaProviderConfig = new EmailTwoFaProviderConfig();
        emailTwoFaProviderConfig.setVerificationCodeLifetime(60);

        platformTwoFaSettings.setProviders(List.of(totpTwoFaProviderConfig, smsTwoFaProviderConfig, emailTwoFaProviderConfig));
        platformTwoFaSettings.setMinVerificationCodeSendPeriod(5);
        platformTwoFaSettings.setTotalAllowedTimeForVerification(100);
        twoFaConfigManager.savePlatformTwoFaSettings(TenantId.SYS_TENANT_ID, platformTwoFaSettings);

        User twoFaUser = new User();
        twoFaUser.setAuthority(Authority.TENANT_ADMIN);
        twoFaUser.setTenantId(tenantId);
        twoFaUser.setEmail("2fa@thingsboard.org");
        twoFaUser = createUserAndLogin(twoFaUser, "12345678");

        TotpTwoFaAccountConfig totpTwoFaAccountConfig = (TotpTwoFaAccountConfig) twoFactorAuthService.generateNewAccountConfig(twoFaUser, TwoFaProviderType.TOTP);
        totpTwoFaAccountConfig.setUseByDefault(true);
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, twoFaUser.getId(), totpTwoFaAccountConfig);

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38012312322");
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, twoFaUser.getId(), smsTwoFaAccountConfig);

        EmailTwoFaAccountConfig emailTwoFaAccountConfig = new EmailTwoFaAccountConfig();
        emailTwoFaAccountConfig.setEmail(twoFaUser.getEmail());
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, twoFaUser.getId(), emailTwoFaAccountConfig);

        logInWithPreVerificationToken(twoFaUser.getEmail(), "12345678");

        Map<TwoFaProviderType, TwoFactorAuthController.TwoFaProviderInfo> providersInfos = readResponse(doGet("/api/auth/2fa/providers").andExpect(status().isOk()), new TypeReference<List<TwoFactorAuthController.TwoFaProviderInfo>>() {}).stream()
                .collect(Collectors.toMap(TwoFactorAuthController.TwoFaProviderInfo::getType, v -> v));

        assertThat(providersInfos).size().isEqualTo(3);

        assertThat(providersInfos).containsKey(TwoFaProviderType.TOTP);
        assertThat(providersInfos.get(TwoFaProviderType.TOTP).isDefault()).isTrue();

        assertThat(providersInfos).containsKey(TwoFaProviderType.SMS);
        assertThat(providersInfos.get(TwoFaProviderType.SMS).isDefault()).isFalse();

        assertThat(providersInfos).containsKey(TwoFaProviderType.EMAIL);
        assertThat(providersInfos.get(TwoFaProviderType.EMAIL).isDefault()).isFalse();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logInWithPreVerificationToken` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logInWithPreVerificationToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);

        JwtPair response = readResponse(doPost("/api/auth/login", loginRequest).andExpect(status().isOk()), JwtPair.class);
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getScope()).isEqualTo(Authority.PRE_VERIFICATION_TOKEN);

        this.token = response.getToken();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `configureTotpTwoFa` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TotpTwoFaAccountConfig configureTotpTwoFa(Consumer<PlatformTwoFaSettings>... customizer) throws ThingsboardException {
        TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");

        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Arrays.stream(new TwoFaProviderConfig[]{totpTwoFaProviderConfig}).collect(Collectors.toList()));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);
        Arrays.stream(customizer).forEach(c -> c.accept(twoFaSettings));
        twoFaConfigManager.savePlatformTwoFaSettings(TenantId.SYS_TENANT_ID, twoFaSettings);

        TotpTwoFaAccountConfig totpTwoFaAccountConfig = (TotpTwoFaAccountConfig) twoFactorAuthService.generateNewAccountConfig(user, TwoFaProviderType.TOTP);
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, user.getId(), totpTwoFaAccountConfig);
        return totpTwoFaAccountConfig;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `configureSmsTwoFa` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private SmsTwoFaAccountConfig configureSmsTwoFa(Consumer<SmsTwoFaProviderConfig>... customizer) throws ThingsboardException {
        SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate("${code}");
        Arrays.stream(customizer).forEach(c -> c.accept(smsTwoFaProviderConfig));

        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Arrays.stream(new TwoFaProviderConfig[]{smsTwoFaProviderConfig}).collect(Collectors.toList()));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);
        twoFaConfigManager.savePlatformTwoFaSettings(TenantId.SYS_TENANT_ID, twoFaSettings);

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38050505050");
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, user.getId(), smsTwoFaAccountConfig);
        return smsTwoFaAccountConfig;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCorrectTotp` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getCorrectTotp(TotpTwoFaAccountConfig totpTwoFaAccountConfig) {
        String secret = StringUtils.substringAfterLast(totpTwoFaAccountConfig.getAuthUrl(), "secret=");
        return new Totp(secret).now();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TwoFactorAuthTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
