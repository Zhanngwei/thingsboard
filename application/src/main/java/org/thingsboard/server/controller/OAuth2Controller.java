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
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.utils.MiscUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`OAuth2Controller` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class OAuth2Controller extends BaseController {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `oAuth2Configuration` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private OAuth2Configuration oAuth2Configuration;


    @ApiOperation(value = "Get OAuth2 clients (getOAuth2Clients)", notes = "Get the list of OAuth2 clients " +
            "to log in with, available for such domain scheme (HTTP or HTTPS) (if x-forwarded-proto request header is present - " +
            "the scheme is known from it) and domain name and port (port may be known from x-forwarded-port header)")
    @RequestMapping(value = "/noauth/oauth2Clients", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getOAuth2Clients` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<OAuth2ClientInfo> getOAuth2Clients(HttpServletRequest request,
                                                   @ApiParam(value = "Mobile application package name, to find OAuth2 clients " +
                                                           "where there is configured mobile application with such package name")
                                                   @RequestParam(required = false) String pkgName,
                                                   @ApiParam(value = "Platform type to search OAuth2 clients for which " +
                                                           "the usage with this platform type is allowed in the settings. " +
                                                           "If platform type is not one of allowable values - it will just be ignored",
                                                           allowableValues = "WEB, ANDROID, IOS")
                                                   @RequestParam(required = false) String platform) throws ThingsboardException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (log.isDebugEnabled()) {
            log.debug("Executing getOAuth2Clients: [{}][{}][{}]", request.getScheme(), request.getServerName(), request.getServerPort());
            Enumeration<String> headerNames = request.getHeaderNames();
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                log.debug("Header: {} {}", header, request.getHeader(header));
            }
        }
        PlatformType platformType = null;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isNotEmpty(platform)) {
            try {
                platformType = PlatformType.valueOf(platform);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {}
        }
        return oAuth2Service.getOAuth2Clients(MiscUtils.getScheme(request), MiscUtils.getDomainNameAndPort(request), pkgName, platformType);
    }

    @ApiOperation(value = "Get current OAuth2 settings (getCurrentOAuth2Info)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getCurrentOAuth2Info` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public OAuth2Info getCurrentOAuth2Info() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_INFO, Operation.READ);
        return oAuth2Service.findOAuth2Info();
    }

    @ApiOperation(value = "Save OAuth2 settings (saveOAuth2Info)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `saveOAuth2Info` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public OAuth2Info saveOAuth2Info(@RequestBody OAuth2Info oauth2Info) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_INFO, Operation.WRITE);
        oAuth2Service.saveOAuth2Info(oauth2Info);
        return oAuth2Service.findOAuth2Info();
    }

    @ApiOperation(value = "Get OAuth2 log in processing URL (getLoginProcessingUrl)", notes = "Returns the URL enclosed in " +
            "double quotes. After successful authentication with OAuth2 provider, it makes a redirect to this path so that the platform can do " +
            "further log in processing. This URL may be configured as 'security.oauth2.loginProcessingUrl' property in yml configuration file, or " +
            "as 'SECURITY_OAUTH2_LOGIN_PROCESSING_URL' env variable. By default it is '/login/oauth2/code/'" + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/loginProcessingUrl", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getLoginProcessingUrl` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getLoginProcessingUrl() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_INFO, Operation.READ);
        return "\"" + oAuth2Configuration.getLoginProcessingUrl() + "\"";
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2Controller` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
