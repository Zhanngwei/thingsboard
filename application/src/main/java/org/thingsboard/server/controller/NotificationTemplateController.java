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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.service.security.permission.Resource.NOTIFICATION;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/notification")
/**
 * 中文说明：
 * 1. 类目的：`NotificationTemplateController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class NotificationTemplateController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `notificationTemplateService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationSettingsService notificationSettingsService;
    /**
     * 字段说明：
     * 1. 保存 `slackService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final SlackService slackService;

    @ApiOperation(value = "Save notification template (saveNotificationTemplate)",
            notes = "Creates or updates notification template." + NEW_LINE +
                    "Here is an example of template to send notification via Web, SMS and Slack:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Greetings\",\n" +
                    "  \"notificationType\": \"GENERAL\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"deliveryMethodsTemplates\": {\n" +
                    "      \"WEB\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"subject\": \"Greetings\",\n" +
                    "        \"body\": \"Hi there, ${recipientTitle}\",\n" +
                    "        \"additionalConfig\": {\n" +
                    "          \"icon\": {\n" +
                    "            \"enabled\": true,\n" +
                    "            \"icon\": \"back_hand\",\n" +
                    "            \"color\": \"#757575\"\n" +
                    "          },\n" +
                    "          \"actionButtonConfig\": {\n" +
                    "            \"enabled\": false\n" +
                    "          }\n" +
                    "        },\n" +
                    "        \"method\": \"WEB\"\n" +
                    "      },\n" +
                    "      \"SMS\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"body\": \"Hi there, ${recipientTitle}\",\n" +
                    "        \"method\": \"SMS\"\n" +
                    "      },\n" +
                    "      \"SLACK\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"body\": \"Hi there, @${recipientTitle}\",\n" +
                    "        \"method\": \"SLACK\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/template")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `saveNotificationTemplate` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public NotificationTemplate saveNotificationTemplate(@RequestBody @Valid NotificationTemplate notificationTemplate) throws Exception {
        notificationTemplate.setTenantId(getTenantId());
        checkEntity(notificationTemplate.getId(), notificationTemplate, NOTIFICATION);
        return doSaveAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::saveNotificationTemplate);
    }

    @ApiOperation(value = "Get notification template by id (getNotificationTemplateById)",
            notes = "Fetches notification template by id." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `getNotificationTemplateById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public NotificationTemplate getNotificationTemplateById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        return checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.READ);
    }

    @ApiOperation(value = "Get notification templates (getNotificationTemplates)",
            notes = "Returns the page of notification templates owned by sysadmin or tenant." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `getNotificationTemplates` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<NotificationTemplate> getNotificationTemplates(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                   @RequestParam int pageSize,
                                                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                   @RequestParam int page,
                                                                   @ApiParam(value = "Case-insensitive 'substring' filter based on template's name and notification type")
                                                                   @RequestParam(required = false) String textSearch,
                                                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION)
                                                                   @RequestParam(required = false) String sortProperty,
                                                                   @ApiParam(value = SORT_ORDER_DESCRIPTION)
                                                                   @RequestParam(required = false) String sortOrder,
                                                                   @ApiParam(value = "Comma-separated list of notification types to filter the templates")
                                                                   @RequestParam(required = false) NotificationType[] notificationTypes,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (notificationTypes == null || notificationTypes.length == 0) {
            notificationTypes = NotificationType.values();
        }
        return notificationTemplateService.findNotificationTemplatesByTenantIdAndNotificationTypes(user.getTenantId(),
                List.of(notificationTypes), pageLink);
    }

    @ApiOperation(value = "Delete notification template by id (deleteNotificationTemplateById",
            notes = "Deletes notification template by its id." + NEW_LINE +
                    "This template cannot be referenced by existing scheduled notification requests or any notification rules." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @DeleteMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteNotificationTemplateById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteNotificationTemplateById(@PathVariable UUID id) throws Exception {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        NotificationTemplate notificationTemplate = checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::deleteNotificationTemplateById);
    }

    @ApiOperation(value = "List Slack conversations (listSlackConversations)",
            notes = "List available Slack conversations by type." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/slack/conversations")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `listSlackConversations` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<SlackConversation> listSlackConversations(@RequestParam SlackConversationType type,
                                                          @ApiParam(value = "Slack bot token. If absent - system Slack settings will be used")
                                                          @RequestParam(required = false) String token,
                                                          @AuthenticationPrincipal SecurityUser user) {
        // PE: generic permission
        if (StringUtils.isEmpty(token)) {
            NotificationSettings settings = notificationSettingsService.findNotificationSettings(user.getTenantId());
            SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                    settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (slackConfig == null) {
                throw new IllegalArgumentException("Slack is not configured");
            }
            token = slackConfig.getBotToken();
        }

        return slackService.listConversations(user.getTenantId(), token, type);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationTemplateController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
