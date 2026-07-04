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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.event.UserSessionInvalidationEvent;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.model.ActivateUserRequest;
import org.thingsboard.server.service.security.model.ChangePasswordRequest;
import org.thingsboard.server.service.security.model.ResetPasswordEmailRequest;
import org.thingsboard.server.service.security.model.ResetPasswordRequest;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`AuthController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class AuthController extends BaseController {

    @Value("${server.rest.rate_limits.reset_password_per_user:5:3600}")
    /**
     * 字段说明：
     * 1. 保存 `defaultLimitsConfiguration` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String defaultLimitsConfiguration;
    private final BCryptPasswordEncoder passwordEncoder;
    /**
     * 字段说明：
     * 1. 保存 `tokenFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final JwtTokenFactory tokenFactory;
    private final MailService mailService;
    /**
     * 字段说明：
     * 1. 保存 `systemSecurityService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final SystemSecurityService systemSecurityService;
    private final RateLimitService rateLimitService;
    /**
     * 字段说明：
     * 1. 保存 `eventPublisher` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final ApplicationEventPublisher eventPublisher;


    @ApiOperation(value = "Get current User (getUser)",
            notes = "Get the information about the User which credentials are used to perform this REST API call.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/user", method = RequestMethod.GET)
    public @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    User getUser() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        return userService.findUserById(securityUser.getTenantId(), securityUser.getId());
    }

    @ApiOperation(value = "Logout (logout)",
            notes = "Special API call to record the 'logout' of the user to the Audit Logs. Since platform uses [JWT](https://jwt.io/), the actual logout is the procedure of clearing the [JWT](https://jwt.io/) token on the client side. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/logout", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `logout` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void logout(HttpServletRequest request) throws ThingsboardException {
        logLogoutAction(request);
    }

    @ApiOperation(value = "Change password for current User (changePassword)",
            notes = "Change the password for the User which credentials are used to perform this REST API call. Be aware that previously generated [JWT](https://jwt.io/) tokens will be still valid until they expire.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/changePassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `changePassword` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ObjectNode changePassword(
            @ApiParam(value = "Change Password Request")
            @RequestBody ChangePasswordRequest changePasswordRequest) throws ThingsboardException {
        String currentPassword = changePasswordRequest.getCurrentPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        SecurityUser securityUser = getCurrentUser();
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, securityUser.getId());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!passwordEncoder.matches(currentPassword, userCredentials.getPassword())) {
            throw new ThingsboardException("Current password doesn't match!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        systemSecurityService.validatePassword(newPassword, userCredentials);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (passwordEncoder.matches(newPassword, userCredentials.getPassword())) {
            throw new ThingsboardException("New password should be different from existing!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        userCredentials.setPassword(passwordEncoder.encode(newPassword));
        userService.replaceUserCredentials(securityUser.getTenantId(), userCredentials);

        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        ObjectNode response = JacksonUtil.newObjectNode();
        response.put("token", tokenFactory.createAccessJwtToken(securityUser).getToken());
        response.put("refreshToken", tokenFactory.createRefreshToken(securityUser).getToken());
        return response;
    }

    @ApiOperation(value = "Get the current User password policy (getUserPasswordPolicy)",
            notes = "API call to get the password policy for the password validation form(s).")
    @RequestMapping(value = "/noauth/userPasswordPolicy", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getUserPasswordPolicy` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public UserPasswordPolicy getUserPasswordPolicy() throws ThingsboardException {
        SecuritySettings securitySettings =
                checkNotNull(systemSecurityService.getSecuritySettings());
        return securitySettings.getPasswordPolicy();
    }

    @ApiOperation(value = "Check Activate User Token (checkActivateToken)",
            notes = "Checks the activation token and forwards user to 'Create Password' page. " +
                    "If token is valid, returns '303 See Other' (redirect) response code with the correct address of 'Create Password' page and same 'activateToken' specified in the URL parameters. " +
                    "If token is not valid, returns '409 Conflict'.")
    @RequestMapping(value = "/noauth/activate", params = {"activateToken"}, method = RequestMethod.GET)
    /**
     * 方法说明：
     * 1. 职责：执行 `checkActivateToken` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ResponseEntity<String> checkActivateToken(
            @ApiParam(value = "The activate token string.")
            @RequestParam(value = "activateToken") String activateToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, activateToken);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (userCredentials != null) {
            String createURI = "/login/createPassword";
            try {
                URI location = new URI(createURI + "?activateToken=" + activateToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", createURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }

    @ApiOperation(value = "Request reset password email (requestResetPasswordByEmail)",
            notes = "Request to send the reset password email if the user with specified email address is present in the database. " +
                    "Always return '200 OK' status for security purposes.")
    @RequestMapping(value = "/noauth/resetPasswordByEmail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `requestResetPasswordByEmail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void requestResetPasswordByEmail(
            @ApiParam(value = "The JSON object representing the reset password email request.")
            @RequestBody ResetPasswordEmailRequest resetPasswordByEmailRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String email = resetPasswordByEmailRequest.getEmail();
            UserCredentials userCredentials = userService.requestPasswordReset(TenantId.SYS_TENANT_ID, email);
            User user = userService.findUserById(TenantId.SYS_TENANT_ID, userCredentials.getUserId());
            String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
            String resetUrl = String.format("%s/api/noauth/resetPassword?resetToken=%s", baseUrl,
                    userCredentials.getResetToken());

            mailService.sendResetPasswordEmailAsync(resetUrl, email);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.warn("Error occurred: {}", e.getMessage());
        }
    }

    @ApiOperation(value = "Check password reset token (checkResetToken)",
            notes = "Checks the password reset token and forwards user to 'Reset Password' page. " +
                    "If token is valid, returns '303 See Other' (redirect) response code with the correct address of 'Reset Password' page and same 'resetToken' specified in the URL parameters. " +
                    "If token is not valid, returns '409 Conflict'.")
    @RequestMapping(value = "/noauth/resetPassword", params = {"resetToken"}, method = RequestMethod.GET)
    /**
     * 方法说明：
     * 1. 职责：执行 `checkResetToken` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ResponseEntity<String> checkResetToken(
            @ApiParam(value = "The reset token string.")
            @RequestParam(value = "resetToken") String resetToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        String resetURI = "/login/resetPassword";
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(TenantId.SYS_TENANT_ID, resetToken);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (userCredentials != null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!rateLimitService.checkRateLimit(LimitedApi.PASSWORD_RESET, userCredentials.getUserId(), defaultLimitsConfiguration)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
            try {
                URI location = new URI(resetURI + "?resetToken=" + resetToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", resetURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }

    @ApiOperation(value = "Activate User",
            notes = "Checks the activation token and updates corresponding user password in the database. " +
                    "Now the user may start using his password to login. " +
                    "The response already contains the [JWT](https://jwt.io) activation and refresh tokens, " +
                    "to simplify the user activation flow and avoid asking user to input password again after activation. " +
                    "If token is valid, returns the object that contains [JWT](https://jwt.io/) access and refresh tokens. " +
                    "If token is not valid, returns '404 Bad Request'.")
    @RequestMapping(value = "/noauth/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `activateUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtPair activateUser(
            @ApiParam(value = "Activate user request.")
            @RequestBody ActivateUserRequest activateRequest,
            @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail,
            HttpServletRequest request) throws ThingsboardException {
        String activateToken = activateRequest.getActivateToken();
        String password = activateRequest.getPassword();
        systemSecurityService.validatePassword(password, null);
        String encodedPassword = passwordEncoder.encode(password);
        UserCredentials credentials = userService.activateUserCredentials(TenantId.SYS_TENANT_ID, activateToken, encodedPassword);
        User user = userService.findUserById(TenantId.SYS_TENANT_ID, credentials.getUserId());
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
        userService.setUserCredentialsEnabled(user.getTenantId(), user.getId(), true);
        String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
        String loginUrl = String.format("%s/login", baseUrl);
        String email = user.getEmail();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (sendActivationMail) {
            try {
                mailService.sendAccountActivatedEmail(loginUrl, email);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                log.info("Unable to send account activation email [{}]", e.getMessage());
            }
        }

        return tokenFactory.createTokenPair(securityUser);
    }

    @ApiOperation(value = "Reset password (resetPassword)",
            notes = "Checks the password reset token and updates the password. " +
                    "If token is valid, returns the object that contains [JWT](https://jwt.io/) access and refresh tokens. " +
                    "If token is not valid, returns '404 Bad Request'.")
    @RequestMapping(value = "/noauth/resetPassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `resetPassword` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtPair resetPassword(
            @ApiParam(value = "Reset password request.")
            @RequestBody ResetPasswordRequest resetPasswordRequest,
            HttpServletRequest request) throws ThingsboardException {
        String resetToken = resetPasswordRequest.getResetToken();
        String password = resetPasswordRequest.getPassword();
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(TenantId.SYS_TENANT_ID, resetToken);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (userCredentials != null) {
            systemSecurityService.validatePassword(password, userCredentials);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (passwordEncoder.matches(password, userCredentials.getPassword())) {
                throw new ThingsboardException("New password should be different from existing!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            String encodedPassword = passwordEncoder.encode(password);
            userCredentials.setPassword(encodedPassword);
            userCredentials.setResetToken(null);
            userCredentials = userService.replaceUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);
            User user = userService.findUserById(TenantId.SYS_TENANT_ID, userCredentials.getUserId());
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), principal);
            String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
            String loginUrl = String.format("%s/login", baseUrl);
            String email = user.getEmail();
            mailService.sendPasswordWasResetEmail(loginUrl, email);

            eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

            return tokenFactory.createTokenPair(securityUser);
        } else {
            throw new ThingsboardException("Invalid reset token!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logLogoutAction` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logLogoutAction(HttpServletRequest request) throws ThingsboardException {
        var user = getCurrentUser();
        systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(request), ActionType.LOGOUT, null);
        eventPublisher.publishEvent(new UserSessionInvalidationEvent(user.getSessionId()));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AuthController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
