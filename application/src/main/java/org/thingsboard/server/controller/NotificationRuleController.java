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
import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import javax.validation.Valid;
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
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`NotificationRuleController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class NotificationRuleController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `notificationRuleService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationRuleService notificationRuleService;

    @ApiOperation(value = "Save notification rule (saveNotificationRule)",
            notes = "Creates or updates notification rule. " + NEW_LINE +
                    "Mandatory properties are `name`, `templateId` (of a template with `notificationType` matching to rule's `triggerType`), " +
                    "`triggerType`, `triggerConfig` and `recipientConfig`. Additionally, you may specify rule `description` " +
                    "inside of `additionalConfig`." + NEW_LINE +
                    "Trigger type of the rule cannot be changed. " +
                    "Available trigger types for tenant: `ENTITY_ACTION`, `ALARM`, `ALARM_COMMENT`, `ALARM_ASSIGNMENT`, " +
                    "`DEVICE_ACTIVITY`, `RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT`.\n" +
                    "For sysadmin, there are following trigger types available: `ENTITIES_LIMIT`, `API_USAGE_LIMIT`, " +
                    "`NEW_PLATFORM_VERSION`." + NEW_LINE +
                    "Here is an example of notification rule to send notification when a " +
                    "device, asset or customer is created or deleted:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Entity action\",\n" +
                    "  \"templateId\": {\n" +
                    "    \"entityType\": \"NOTIFICATION_TEMPLATE\",\n" +
                    "    \"id\": \"32117320-d785-11ed-a06c-21dd57dd88ca\"\n" +
                    "  },\n" +
                    "  \"triggerType\": \"ENTITY_ACTION\",\n" +
                    "  \"triggerConfig\": {\n" +
                    "    \"entityTypes\": [\n" +
                    "      \"CUSTOMER\",\n" +
                    "      \"DEVICE\",\n" +
                    "      \"ASSET\"\n" +
                    "    ],\n" +
                    "    \"created\": true,\n" +
                    "    \"updated\": false,\n" +
                    "    \"deleted\": true,\n" +
                    "    \"triggerType\": \"ENTITY_ACTION\"\n" +
                    "  },\n" +
                    "  \"recipientsConfig\": {\n" +
                    "    \"targets\": [\n" +
                    "      \"320f2930-d785-11ed-a06c-21dd57dd88ca\"\n" +
                    "    ],\n" +
                    "    \"triggerType\": \"ENTITY_ACTION\"\n" +
                    "  },\n" +
                    "  \"additionalConfig\": {\n" +
                    "    \"description\": \"Send notification to tenant admins or customer users when a device, asset or customer is created\"\n" +
                    "  },\n" +
                    "  \"templateName\": \"Entity action notification\",\n" +
                    "  \"deliveryMethods\": [\n" +
                    "    \"WEB\"\n" +
                    "  ]\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/rule")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `saveNotificationRule` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public NotificationRule saveNotificationRule(@RequestBody @Valid NotificationRule notificationRule,
                                                 @AuthenticationPrincipal SecurityUser user) throws Exception {
        notificationRule.setTenantId(user.getTenantId());
        checkEntity(notificationRule.getId(), notificationRule, NOTIFICATION);

        NotificationRuleTriggerType triggerType = notificationRule.getTriggerType();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if ((user.isTenantAdmin() && !triggerType.isTenantLevel()) || (user.isSystemAdmin() && triggerType.isTenantLevel())) {
            throw new IllegalArgumentException("Trigger type " + triggerType + " is not available");
        }

        boolean created = notificationRule.getId() == null;
        notificationRule = doSaveAndLog(EntityType.NOTIFICATION_RULE, notificationRule, notificationRuleService::saveNotificationRule);
        tbClusterService.broadcastEntityStateChangeEvent(user.getTenantId(), notificationRule.getId(), created ?
                ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        return notificationRule;
    }

    @ApiOperation(value = "Get notification rule by id (getNotificationRuleById)",
            notes = "Fetches notification rule info by rule's id.\n" +
                    "In addition to regular notification rule fields, " +
                    "there are `templateName` and `deliveryMethods` in the response." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/rule/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `getNotificationRuleById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public NotificationRuleInfo getNotificationRuleById(@PathVariable UUID id) throws ThingsboardException {
        NotificationRuleId notificationRuleId = new NotificationRuleId(id);
        return checkEntityId(notificationRuleId, notificationRuleService::findNotificationRuleInfoById, Operation.READ);
    }

    @ApiOperation(value = "Get notification rules (getNotificationRules)",
            notes = "Returns the page of notification rules." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/rules")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `getNotificationRules` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<NotificationRuleInfo> getNotificationRules(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                               @RequestParam int pageSize,
                                                               @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                               @RequestParam int page,
                                                               @ApiParam(value = "Case-insensitive 'substring' filter based on rule's name")
                                                               @RequestParam(required = false) String textSearch,
                                                               @ApiParam(value = SORT_PROPERTY_DESCRIPTION)
                                                               @RequestParam(required = false) String sortProperty,
                                                               @ApiParam(value = SORT_ORDER_DESCRIPTION)
                                                               @RequestParam(required = false) String sortOrder,
                                                               @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationRuleService.findNotificationRulesInfosByTenantId(user.getTenantId(), pageLink);
    }

    @ApiOperation(value = "Delete notification rule (deleteNotificationRule)",
            notes = "Deletes notification rule by id.\n" +
                    "Cancels all related scheduled notification requests (e.g. due to escalation table)" +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @DeleteMapping("/rule/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteNotificationRule` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteNotificationRule(@PathVariable UUID id,
                                       @AuthenticationPrincipal SecurityUser user) throws Exception {
        NotificationRuleId notificationRuleId = new NotificationRuleId(id);
        NotificationRule notificationRule = checkEntityId(notificationRuleId, notificationRuleService::findNotificationRuleById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_RULE, notificationRule, notificationRuleService::deleteNotificationRuleById);
        tbClusterService.broadcastEntityStateChangeEvent(user.getTenantId(), notificationRuleId, ComponentLifecycleEvent.DELETED);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationRuleController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
