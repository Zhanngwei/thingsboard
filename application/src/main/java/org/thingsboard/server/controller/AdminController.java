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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettingsInfo;
import org.thingsboard.server.common.data.sync.vc.VcUtils;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.auth.oauth2.CookieUtils;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.thingsboard.server.service.system.SystemInfoService;
import org.thingsboard.server.service.update.UpdateService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api/admin")
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`AdminController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class AdminController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `PREV_URI_PATH_PARAMETER` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String PREV_URI_PATH_PARAMETER = "prevUri";
    private static final String PREV_URI_COOKIE_NAME = "prev_uri";
    /**
     * 字段说明：
     * 1. 保存 `STATE_COOKIE_NAME` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String STATE_COOKIE_NAME = "state";
    private static final String MAIL_SETTINGS_KEY = "mail";

    /**
     * 字段说明：
     * 1. 保存 `mailService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final MailService mailService;
    private final SmsService smsService;
    /**
     * 字段说明：
     * 1. 保存 `adminSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final AdminSettingsService adminSettingsService;
    private final SystemSecurityService systemSecurityService;
    /**
     * 字段说明：
     * 1. 保存 `jwtSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final JwtSettingsService jwtSettingsService;
    private final JwtTokenFactory tokenFactory;
    /**
     * 字段说明：
     * 1. 保存 `versionControlService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final EntitiesVersionControlService versionControlService;
    private final TbAutoCommitSettingsService autoCommitSettingsService;
    /**
     * 字段说明：
     * 1. 保存 `updateService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final UpdateService updateService;
    private final SystemInfoService systemInfoService;
    /**
     * 字段说明：
     * 1. 保存 `auditLogService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final AuditLogService auditLogService;

    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Get the Administration Settings object using specified string key. Referencing non-existing key will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/{key}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAdminSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AdminSettings getAdminSettings(
            @ApiParam(value = "A string value of the key (e.g. 'general' or 'mail').")
            @PathVariable("key") String key) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        AdminSettings adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key), "No Administration settings found for key: " + key);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (adminSettings.getKey().equals("mail")) {
            ((ObjectNode) adminSettings.getJsonValue()).remove("password");
            ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
        }
        return adminSettings;
    }

    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Creates or Updates the Administration Settings. Platform generates random Administration Settings Id during settings creation. " +
                    "The Administration Settings Id will be present in the response. Specify the Administration Settings Id when you would like to update the Administration Settings. " +
                    "Referencing non-existing Administration Settings Id will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveAdminSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AdminSettings saveAdminSettings(
            @ApiParam(value = "A JSON value representing the Administration Settings.")
            @RequestBody AdminSettings adminSettings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
        adminSettings.setTenantId(getTenantId());
        adminSettings = checkNotNull(adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings));
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (adminSettings.getKey().equals("mail")) {
            mailService.updateMailConfiguration();
            ((ObjectNode) adminSettings.getJsonValue()).remove("password");
            ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (adminSettings.getKey().equals("sms")) {
            smsService.updateSmsConfiguration();
        }
        return adminSettings;
    }

    @ApiOperation(value = "Get the Security Settings object",
            notes = "Get the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getSecuritySettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SecuritySettings getSecuritySettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return checkNotNull(systemSecurityService.getSecuritySettings());
    }

    @ApiOperation(value = "Update Security Settings (saveSecuritySettings)",
            notes = "Updates the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveSecuritySettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SecuritySettings saveSecuritySettings(
            @ApiParam(value = "A JSON value representing the Security Settings.")
            @RequestBody SecuritySettings securitySettings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
        securitySettings = checkNotNull(systemSecurityService.saveSecuritySettings(securitySettings));
        return securitySettings;
    }

    @ApiOperation(value = "Get the JWT Settings object (getJwtSettings)",
            notes = "Get the JWT Settings object that contains JWT token policy, etc. " + SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getJwtSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtSettings getJwtSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return checkNotNull(jwtSettingsService.getJwtSettings());
    }

    @ApiOperation(value = "Update JWT Settings (saveJwtSettings)",
            notes = "Updates the JWT Settings object that contains JWT token policy, etc. The tokenSigningKey field is a Base64 encoded string." + SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveJwtSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JwtPair saveJwtSettings(
            @ApiParam(value = "A JSON value representing the JWT Settings.")
            @RequestBody JwtSettings jwtSettings) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        accessControlService.checkPermission(securityUser, Resource.ADMIN_SETTINGS, Operation.WRITE);
        checkNotNull(jwtSettingsService.saveJwtSettings(jwtSettings));
        return tokenFactory.createTokenPair(securityUser);
    }

    @ApiOperation(value = "Send test email (sendTestMail)",
            notes = "Attempts to send test email to the System Administrator User using Mail Settings provided as a parameter. " +
                    "You may change the 'To' email in the user profile of the System Administrator. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/testMail", method = RequestMethod.POST)
    /**
     * 方法说明：
     * 1. 职责：执行 `sendTestMail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendTestMail(
            @ApiParam(value = "A JSON value representing the Mail Settings.")
            @RequestBody AdminSettings adminSettings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        adminSettings = checkNotNull(adminSettings);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (adminSettings.getKey().equals("mail")) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (adminSettings.getJsonValue().has("enableOauth2") && adminSettings.getJsonValue().get("enableOauth2").asBoolean()){
                AdminSettings mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
                JsonNode refreshToken = mailSettings.getJsonValue().get("refreshToken");
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (refreshToken == null) {
                    throw new ThingsboardException("Refresh token was not generated. Please, generate refresh token.", ThingsboardErrorCode.GENERAL);
                }
                ObjectNode settings = (ObjectNode) adminSettings.getJsonValue();
                settings.put("refreshToken", refreshToken.asText());
            }
            else {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!adminSettings.getJsonValue().has("password")) {
                    AdminSettings mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
                    ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
                }
            }
            String email = getCurrentUser().getEmail();
            mailService.sendTestMail(adminSettings.getJsonValue(), email);
        }
    }

    @ApiOperation(value = "Send test sms (sendTestMail)",
            notes = "Attempts to send test sms to the System Administrator User using SMS Settings and phone number provided as a parameters of the request. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/testSms", method = RequestMethod.POST)
    /**
     * 方法说明：
     * 1. 职责：执行 `sendTestSms` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendTestSms(
            @ApiParam(value = "A JSON value representing the Test SMS request.")
            @RequestBody TestSmsRequest testSmsRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        try {
            smsService.sendTestSms(testSmsRequest);
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, null, testSmsRequest.getNumberTo());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (ThingsboardException e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, e, testSmsRequest.getNumberTo());
            throw e;
        }
    }

    @ApiOperation(value = "Get repository settings (getRepositorySettings)",
            notes = "Get the repository settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings")
    /**
     * 方法说明：
     * 1. 职责：执行 `getRepositorySettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public RepositorySettings getRepositorySettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        RepositorySettings versionControlSettings = checkNotNull(versionControlService.getVersionControlSettings(getTenantId()));
        versionControlSettings.setPassword(null);
        versionControlSettings.setPrivateKey(null);
        versionControlSettings.setPrivateKeyPassword(null);
        return versionControlSettings;
    }

    @ApiOperation(value = "Check repository settings exists (repositorySettingsExists)",
            notes = "Check whether the repository settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/exists")
    /**
     * 方法说明：
     * 1. 职责：执行 `repositorySettingsExists` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Boolean repositorySettingsExists() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return versionControlService.getVersionControlSettings(getTenantId()) != null;
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/info")
    /**
     * 方法说明：
     * 1. 职责：执行 `getRepositorySettingsInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public RepositorySettingsInfo getRepositorySettingsInfo() throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        RepositorySettings repositorySettings = versionControlService.getVersionControlSettings(getTenantId());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (repositorySettings != null) {
            return RepositorySettingsInfo.builder()
                    .configured(true)
                    .readOnly(repositorySettings.isReadOnly())
                    .build();
        } else {
            return RepositorySettingsInfo.builder()
                    .configured(false)
                    .build();
        }
    }

    @ApiOperation(value = "Creates or Updates the repository settings (saveRepositorySettings)",
            notes = "Creates or Updates the repository settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/repositorySettings")
    /**
     * 方法说明：
     * 1. 职责：执行 `saveRepositorySettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<RepositorySettings> saveRepositorySettings(@RequestBody RepositorySettings settings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        ListenableFuture<RepositorySettings> future = versionControlService.saveVersionControlSettings(getTenantId(), settings);
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return wrapFuture(Futures.transform(future, savedSettings -> {
            savedSettings.setPassword(null);
            savedSettings.setPrivateKey(null);
            savedSettings.setPrivateKeyPassword(null);
            return savedSettings;
        }, MoreExecutors.directExecutor()));
    }

    @ApiOperation(value = "Delete repository settings (deleteRepositorySettings)",
            notes = "Deletes the repository settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteRepositorySettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<Void> deleteRepositorySettings() throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return wrapFuture(versionControlService.deleteVersionControlSettings(getTenantId()));
    }

    @ApiOperation(value = "Check repository access (checkRepositoryAccess)",
            notes = "Attempts to check repository access. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings/checkAccess", method = RequestMethod.POST)
    /**
     * 方法说明：
     * 1. 职责：执行 `checkRepositoryAccess` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<Void> checkRepositoryAccess(
            @ApiParam(value = "A JSON value representing the Repository Settings.")
            @RequestBody RepositorySettings settings) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        settings = checkNotNull(settings);
        return wrapFuture(versionControlService.checkVersionControlAccess(getTenantId(), settings));
    }

    @ApiOperation(value = "Get auto commit settings (getAutoCommitSettings)",
            notes = "Get the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings")
    /**
     * 方法说明：
     * 1. 职责：执行 `getAutoCommitSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AutoCommitSettings getAutoCommitSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return checkNotNull(autoCommitSettingsService.get(getTenantId()));
    }

    @ApiOperation(value = "Check auto commit settings exists (autoCommitSettingsExists)",
            notes = "Check whether the auto commit settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings/exists")
    /**
     * 方法说明：
     * 1. 职责：执行 `autoCommitSettingsExists` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Boolean autoCommitSettingsExists() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return autoCommitSettingsService.get(getTenantId()) != null;
    }

    @ApiOperation(value = "Creates or Updates the auto commit settings (saveAutoCommitSettings)",
            notes = "Creates or Updates the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/autoCommitSettings")
    /**
     * 方法说明：
     * 1. 职责：执行 `saveAutoCommitSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AutoCommitSettings saveAutoCommitSettings(@RequestBody AutoCommitSettings settings) throws ThingsboardException {
        settings.values().forEach(config -> VcUtils.checkBranchName(config.getBranch()));
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return autoCommitSettingsService.save(getTenantId(), settings);
    }

    @ApiOperation(value = "Delete auto commit settings (deleteAutoCommitSettings)",
            notes = "Deletes the auto commit settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/autoCommitSettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteAutoCommitSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteAutoCommitSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
        autoCommitSettingsService.delete(getTenantId());
    }

    @ApiOperation(value = "Check for new Platform Releases (checkUpdates)",
            notes = "Check notifications about new platform releases. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/updates", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `checkUpdates` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public UpdateMessage checkUpdates() throws ThingsboardException {
        return updateService.checkUpdates();
    }

    @ApiOperation(value = "Get system info (getSystemInfo)",
            notes = "Get main information about system. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/systemInfo", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getSystemInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SystemInfo getSystemInfo() throws ThingsboardException {
        return systemInfoService.getSystemInfo();
    }

    @ApiOperation(value = "Get features info (getFeaturesInfo)",
            notes = "Get information about enabled/disabled features. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/featuresInfo", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getFeaturesInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public FeaturesInfo getFeaturesInfo() {
        return systemInfoService.getFeaturesInfo();
    }

    @ApiOperation(value = "Get OAuth2 log in processing URL (getMailProcessingUrl)", notes = "Returns the URL enclosed in " +
            "double quotes. After successful authentication with OAuth2 provider and user consent for requested scope, it makes a redirect to this path so that the platform can do " +
            "further log in processing and generating access tokens. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/mail/oauth2/loginProcessingUrl", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getMailProcessingUrl` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getMailProcessingUrl() throws ThingsboardException {
         accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
         return "\"/api/admin/mail/oauth2/code\"";
    }

    @ApiOperation(value = "Redirect user to mail provider login page. ", notes = "After user logged in and provided access" +
            "provider sends authorization code to specified redirect uri.)" )
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/mail/oauth2/authorize", method = RequestMethod.GET, produces = "application/text")
    /**
     * 方法说明：
     * 1. 职责：执行 `getAuthorizationUrl` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getAuthorizationUrl(HttpServletRequest request, HttpServletResponse response) throws ThingsboardException {
        String state = StringUtils.generateSafeToken();
        if (request.getParameter(PREV_URI_PATH_PARAMETER) != null) {
            CookieUtils.addCookie(response, PREV_URI_COOKIE_NAME, request.getParameter(PREV_URI_PATH_PARAMETER), 180);
        }
        CookieUtils.addCookie(response, STATE_COOKIE_NAME, state, 180);

        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        AdminSettings adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_SETTINGS_KEY), "No Administration mail settings found");
        JsonNode jsonValue = adminSettings.getJsonValue();

        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String authUri = checkNotNull(jsonValue.get("authUri"), "No authorization uri was configured").asText();
        String redirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        List<String> scope =  JacksonUtil.convertValue(checkNotNull(jsonValue.get("scope"), "No scope was configured"), new TypeReference<>() {
        });

        return "\"" + new AuthorizationCodeRequestUrl(authUri, clientId)
                .setScopes(scope)
                .setState(state)
                .setRedirectUri(redirectUri)
                .build() + "\"";
    }

    @RequestMapping(value = "/mail/oauth2/code", params = {"code", "state"}, method = RequestMethod.GET)
    /**
     * 方法说明：
     * 1. 职责：执行 `codeProcessingUrl` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void codeProcessingUrl(
            @RequestParam(value = "code") String code, @RequestParam(value = "state") String state,
            HttpServletRequest request, HttpServletResponse response) throws ThingsboardException, IOException {
        Optional<Cookie> prevUrlOpt = CookieUtils.getCookie(request, PREV_URI_COOKIE_NAME);
        Optional<Cookie> cookieState = CookieUtils.getCookie(request, STATE_COOKIE_NAME);

        String baseUrl = this.systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
        String prevUri = baseUrl + (prevUrlOpt.isPresent() ? prevUrlOpt.get().getValue(): "/settings/outgoing-mail");

        if (cookieState.isEmpty() || !cookieState.get().getValue().equals(state)) {
            CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
            throw new ThingsboardException("Refresh token was not generated, invalid state param", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, PREV_URI_COOKIE_NAME);

        AdminSettings adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_SETTINGS_KEY), "No Administration mail settings found");
        JsonNode jsonValue = adminSettings.getJsonValue();

        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String clientSecret = checkNotNull(jsonValue.get("clientSecret"), "No client secret was configured").asText();
        String clientRedirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        String tokenUri = checkNotNull(jsonValue.get("tokenUri"), "No authorization uri was configured").asText();

        TokenResponse tokenResponse;
        try {
            tokenResponse = new AuthorizationCodeTokenRequest(new NetHttpTransport(), new GsonFactory(), new GenericUrl(tokenUri), code)
                    .setRedirectUri(clientRedirectUri)
                    .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                    .execute();
        } catch (IOException e) {
            log.warn("Unable to retrieve refresh token: {}", e.getMessage());
            throw new ThingsboardException("Error while requesting access token: " + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
        ((ObjectNode)jsonValue).put("refreshToken", tokenResponse.getRefreshToken());
        ((ObjectNode)jsonValue).put("tokenGenerated", true);

        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
        response.sendRedirect(prevUri);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AdminController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
