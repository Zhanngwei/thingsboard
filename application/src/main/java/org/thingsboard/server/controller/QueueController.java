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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_NAME_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_QUEUE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SERVICE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`QueueController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class QueueController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `tbQueueService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbQueueService tbQueueService;

    @ApiOperation(value = "Get Queues (getTenantQueuesByServiceType)",
            notes = "Returns a page of queues registered in the platform. " +
                    PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/queues", params = {"serviceType", "pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantQueuesByServiceType` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<Queue> getTenantQueuesByServiceType(@ApiParam(value = QUEUE_SERVICE_TYPE_DESCRIPTION, allowableValues = QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES, required = true)
                                                        @RequestParam String serviceType,
                                                        @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                        @RequestParam int pageSize,
                                                        @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                        @RequestParam int page,
                                                        @ApiParam(value = QUEUE_QUEUE_TEXT_SEARCH_DESCRIPTION)
                                                        @RequestParam(required = false) String textSearch,
                                                        @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = QUEUE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                        @RequestParam(required = false) String sortProperty,
                                                        @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                        @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        ServiceType type = ServiceType.of(serviceType);
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (type) {
            case TB_RULE_ENGINE:
                return queueService.findQueuesByTenantId(getTenantId(), pageLink);
            default:
                return new PageData<>();
        }
    }

    @ApiOperation(value = "Get Queue (getQueueById)",
            notes = "Fetch the Queue object based on the provided Queue Id. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/queues/{queueId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getQueueById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Queue getQueueById(@ApiParam(value = QUEUE_ID_PARAM_DESCRIPTION)
                              @PathVariable("queueId") String queueIdStr) throws ThingsboardException {
        checkParameter("queueId", queueIdStr);
        QueueId queueId = new QueueId(UUID.fromString(queueIdStr));
        checkQueueId(queueId, Operation.READ);
        return checkNotNull(queueService.findQueueById(getTenantId(), queueId));
    }

    @ApiOperation(value = "Get Queue (getQueueByName)",
            notes = "Fetch the Queue object based on the provided Queue name. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/queues/name/{queueName}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getQueueByName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Queue getQueueByName(@ApiParam(value = QUEUE_NAME_PARAM_DESCRIPTION)
                                @PathVariable("queueName") String queueName) throws ThingsboardException {
        checkParameter("queueName", queueName);
        return checkNotNull(queueService.findQueueByTenantIdAndName(getTenantId(), queueName));
    }

    @ApiOperation(value = "Create Or Update Queue (saveQueue)",
            notes = "Create or update the Queue. When creating queue, platform generates Queue Id as " + UUID_WIKI_LINK +
                    "Specify existing Queue id to update the queue. " +
                    "Referencing non-existing Queue Id will cause 'Not Found' error." +
                    "\n\nQueue name is unique in the scope of sysadmin. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Queue entity. " +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/queues", params = {"serviceType"}, method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveQueue` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Queue saveQueue(@ApiParam(value = "A JSON value representing the queue.")
                           @RequestBody Queue queue,
                           @ApiParam(value = QUEUE_SERVICE_TYPE_DESCRIPTION, allowableValues = QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES, required = true)
                           @RequestParam String serviceType) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        queue.setTenantId(getCurrentUser().getTenantId());

        checkEntity(queue.getId(), queue, Resource.QUEUE);

        ServiceType type = ServiceType.of(serviceType);
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (type) {
            case TB_RULE_ENGINE:
                queue.setTenantId(getTenantId());
                Queue savedQueue = tbQueueService.saveQueue(queue);
                checkNotNull(savedQueue);
                return savedQueue;
            default:
                return null;
        }
    }

    @ApiOperation(value = "Delete Queue (deleteQueue)", notes = "Deletes the Queue. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/queues/{queueId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteQueue` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteQueue(@ApiParam(value = QUEUE_ID_PARAM_DESCRIPTION)
                            @PathVariable("queueId") String queueIdStr) throws ThingsboardException {
        checkParameter("queueId", queueIdStr);
        QueueId queueId = new QueueId(toUUID(queueIdStr));
        checkQueueId(queueId, Operation.DELETE);
        tbQueueService.deleteQueue(getTenantId(), queueId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`QueueController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
