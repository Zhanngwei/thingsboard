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

import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.EmailTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;

@RestController
@RequestMapping("/api/auth/2fa")
@TbCoreComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`TwoFactorAuthController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class TwoFactorAuthController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `twoFactorAuthService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TwoFactorAuthService twoFactorAuthService;
    private final TwoFaConfigManager twoFaConfigManager;
    /**
     * 字段说明：
     * 1. 保存 `tokenFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final JwtTokenFactory tokenFactory;
    private final SystemSecurityService systemSecurityService;
    /**
     * 字段说明：
     * 1. 保存 `userService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final UserService userService;


    @ApiOperation(value = "Request 2FA verification code (requestTwoFaVerificationCode)",
            notes = "Request 2FA verification code." + NEW_LINE +
                    "To make a request to this endpoint, you need an access token with the scope of PRE_VERIFICATION_TOKEN, " +
                    "which is issued on username/password auth if 2FA is enabled." + NEW_LINE +
                    "The API method is rate limited (using rate limit config from TwoFactorAuthSettings). " +
                    "Will return a Bad Request error if provider is not configured for usage, " +
                    "and Too Many Requests error if rate limits are exceeded.")
    @PostMapping("/verification/send")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `requestTwoFaVerificationCode` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void requestTwoFaVerificationCode(@RequestParam TwoFaProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, providerType, true);
    }

    @ApiOperation(value = "Check 2FA verification code (checkTwoFaVerificationCode)",
            notes = "Checks 2FA verification code, and if it is correct the method returns a regular access and refresh token pair." + NEW_LINE +
                    "The API method is rate limited (using rate limit config from TwoFactorAuthSettings), and also will block a user " +
                    "after X unsuccessful verification attempts if such behavior is configured (in TwoFactorAuthSettings)." + NEW_LINE +
                    "Will return a Bad Request error if provider is not configured for usage, " +
                    "and Too Many Requests error if rate limits are exceeded.")
    @PostMapping("/verification/check")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `checkTwoFaVerificationCode` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtPair checkTwoFaVerificationCode(@RequestParam TwoFaProviderType providerType,
                                              @RequestParam String verificationCode, HttpServletRequest servletRequest) throws Exception {
        SecurityUser user = getCurrentUser();
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, providerType, verificationCode, true);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (verificationSuccess) {
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, null);
            user = new SecurityUser(userService.findUserById(user.getTenantId(), user.getId()), true, user.getUserPrincipal());
            return tokenFactory.createTokenPair(user);
        } else {
            ThingsboardException error = new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, error);
            throw error;
        }
    }


    @ApiOperation(value = "Get available 2FA providers (getAvailableTwoFaProviders)", notes =
            "Get the list of 2FA provider infos available for user to use. Example:\n" +
                    "```\n[\n" +
                    "  {\n    \"type\": \"EMAIL\",\n    \"default\": true,\n    \"contact\": \"ab*****ko@gmail.com\"\n  },\n" +
                    "  {\n    \"type\": \"TOTP\",\n    \"default\": false,\n    \"contact\": null\n  },\n" +
                    "  {\n    \"type\": \"SMS\",\n    \"default\": false,\n    \"contact\": \"+38********12\"\n  }\n" +
                    "]\n```")
    @GetMapping("/providers")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `getAvailableTwoFaProviders` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<TwoFaProviderInfo> getAvailableTwoFaProviders() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        Optional<PlatformTwoFaSettings> platformTwoFaSettings = twoFaConfigManager.getPlatformTwoFaSettings(user.getTenantId(), true);
        return twoFaConfigManager.getAccountTwoFaSettings(user.getTenantId(), user.getId())
                .map(settings -> settings.getConfigs().values()).orElse(Collections.emptyList())
                .stream().map(config -> {
                    String contact = null;
                    // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
                    switch (config.getProviderType()) {
                        case SMS:
                            String phoneNumber = ((SmsTwoFaAccountConfig) config).getPhoneNumber();
                            contact = StringUtils.obfuscate(phoneNumber, 2, '*', phoneNumber.indexOf('+') + 1, phoneNumber.length());
                            break;
                        case EMAIL:
                            String email = ((EmailTwoFaAccountConfig) config).getEmail();
                            contact = StringUtils.obfuscate(email, 2, '*', 0, email.indexOf('@'));
                            break;
                    }
                    return TwoFaProviderInfo.builder()
                            .type(config.getProviderType())
                            .isDefault(config.isUseByDefault())
                            .contact(contact)
                            .minVerificationCodeSendPeriod(platformTwoFaSettings.get().getMinVerificationCodeSendPeriod())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    @Builder
    /**
     * 中文说明：
     * 1. 类目的：`TwoFaProviderInfo` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
     * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 MVC Controller / Facade。
     */
    public static class TwoFaProviderInfo {
        /**
         * 字段说明：
         * 1. 保存 `type` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private TwoFaProviderType type;
        private boolean isDefault;
        /**
         * 字段说明：
         * 1. 保存 `contact` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private String contact;
        private Integer minVerificationCodeSendPeriod;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TwoFactorAuthController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
