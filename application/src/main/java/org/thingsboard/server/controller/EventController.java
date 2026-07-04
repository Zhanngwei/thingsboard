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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_DEBUG_RULE_CHAIN_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_DEBUG_RULE_NODE_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_END_TIME_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_ERROR_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_LC_EVENT_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_START_TIME_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_STATS_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
/**
 * 中文说明：
 * 1. 类目的：`EventController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class EventController extends BaseController {

    private static final String EVENT_FILTER_DEFINITION = "# Event Filter Definition" + NEW_LINE +
            "5 different eventFilter objects could be set for different event types. " +
            "The eventType field is required. Others are optional. If some of them are set, the filtering will be applied according to them. " +
            "See the examples below for all the fields used for each event type filtering. " + NEW_LINE +
            "Note," + NEW_LINE +
            " * 'server' - string value representing the server name, identifier or ip address where the platform is running;\n" +
            " * 'errorStr' - the case insensitive 'contains' filter based on error message." + NEW_LINE +
            "## Error Event Filter" + NEW_LINE +
            EVENT_ERROR_FILTER_OBJ + NEW_LINE +
            " * 'method' - string value representing the method name when the error happened." + NEW_LINE +
            "## Lifecycle Event Filter" + NEW_LINE +
            EVENT_LC_EVENT_FILTER_OBJ + NEW_LINE +
            " * 'event' - string value representing the lifecycle event type;\n" +
            " * 'status' - string value representing status of the lifecycle event." + NEW_LINE +
            "## Statistics Event Filter" + NEW_LINE +
            EVENT_STATS_FILTER_OBJ + NEW_LINE +
            " * 'messagesProcessed' - the minimum number of successfully processed messages;\n" +
            " * 'errorsOccurred' - the minimum number of errors occurred during messages processing." + NEW_LINE +
            "## Debug Rule Node Event Filter" + NEW_LINE +
            EVENT_DEBUG_RULE_NODE_FILTER_OBJ + NEW_LINE +
            "## Debug Rule Chain Event Filter" + NEW_LINE +
            EVENT_DEBUG_RULE_CHAIN_FILTER_OBJ + NEW_LINE +
            " * 'msgDirectionType' - string value representing msg direction type (incoming to entity or outcoming from entity);\n" +
            " * 'dataSearch' - the case insensitive 'contains' filter based on data (key and value) for the message;\n" +
            " * 'metadataSearch' - the case insensitive 'contains' filter based on metadata (key and value) for the message;\n" +
            " * 'entityName' - string value representing the entity type;\n" +
            " * 'relationType' - string value representing the type of message routing;\n" +
            " * 'entityId' - string value representing the entity id in the event body (originator of the message);\n" +
            " * 'msgType' - string value representing the message type;\n" +
            " * 'isError' - boolean value to filter the errors." + NEW_LINE;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `eventService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EventService eventService;

    @ApiOperation(value = "Get Events by type (getEvents)",
            notes = "Returns a page of events for specified entity by specifying event type. " +
                    PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}/{eventType}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEvents` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<EventInfo> getEvents(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = "A string value representing event type", example = "STATS", required = true)
            @PathVariable("eventType") String eventType,
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EVENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));

        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        return checkNotNull(eventService.findEvents(tenantId, entityId, resolveEventType(eventType), pageLink));
    }

    @ApiOperation(value = "Get Events (Deprecated)",
            notes = "Returns a page of events for specified entity. Deprecated and will be removed in next minor release. " +
                    "The call was deprecated to improve the performance of the system. " +
                    "Current implementation will return 'Lifecycle' events only. " +
                    "Use 'Get events by type' or 'Get events by filter' instead. " +
                    PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEvents` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<EventInfo> getEvents(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam("tenantId") String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EVENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));

        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);

        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        return checkNotNull(eventService.findEvents(tenantId, entityId, EventType.LC_EVENT, pageLink));
    }

    @ApiOperation(value = "Get Events by event filter (getEvents)",
            notes = "Returns a page of events for the chosen entity by specifying the event filter. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE +
                    EVENT_FILTER_DEFINITION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEvents` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<EventInfo> getEvents(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = "A JSON value representing the event filter.", required = true)
            @RequestBody EventFilter eventFilter,
            @ApiParam(value = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EVENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));

        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);

        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        return checkNotNull(eventService.findEventsByFilter(tenantId, entityId, eventFilter, pageLink));
    }

    @ApiOperation(value = "Clear Events (clearEvents)", notes = "Clears events by filter for specified entity.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}/clear", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `clearEvents` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void clearEvents(@ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
                            @PathVariable(ENTITY_TYPE) String strEntityType,
                            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
                            @PathVariable(ENTITY_ID) String strEntityId,
                            @ApiParam(value = EVENT_START_TIME_DESCRIPTION)
                            @RequestParam(required = false) Long startTime,
                            @ApiParam(value = EVENT_END_TIME_DESCRIPTION)
                            @RequestParam(required = false) Long endTime,
                            @ApiParam(value = EVENT_FILTER_DEFINITION)
                            @RequestBody EventFilter eventFilter) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.WRITE);

        eventService.removeEvents(getTenantId(), entityId, eventFilter, startTime, endTime);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `resolveEventType` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static EventType resolveEventType(String eventType) throws ThingsboardException {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (var et : EventType.values()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (et.name().equalsIgnoreCase(eventType) || et.getOldName().equalsIgnoreCase(eventType)) {
                return et;
            }
        }
        throw new ThingsboardException("Event type: '" + eventType + "' is not supported!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EventController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
