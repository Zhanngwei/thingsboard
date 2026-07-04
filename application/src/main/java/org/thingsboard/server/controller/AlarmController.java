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
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ASSIGNEE_ID;
import static org.thingsboard.server.controller.ControllerConstants.ASSIGN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
/**
 * 中文说明：
 * 1. 类目的：`AlarmController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class AlarmController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `tbAlarmService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbAlarmService tbAlarmService;

    /**
     * 字段说明：
     * 1. 保存 `ALARM_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String ALARM_ID = "alarmId";
    private static final String ALARM_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the originator of alarm is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the originator of alarm belongs to the customer. ";
    /**
     * 字段说明：
     * 1. 保存 `ALARM_QUERY_SEARCH_STATUS_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ALARM_QUERY_SEARCH_STATUS_DESCRIPTION = "A string value representing one of the AlarmSearchStatus enumeration value";

    /**
     * 字段说明：
     * 1. 保存 `ALARM_QUERY_SEARCH_STATUS_ARRAY_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ALARM_QUERY_SEARCH_STATUS_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing one of the AlarmSearchStatus enumeration value";
    private static final String ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES = "ANY, ACTIVE, CLEARED, ACK, UNACK";
    /**
     * 字段说明：
     * 1. 保存 `ALARM_QUERY_STATUS_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ALARM_QUERY_STATUS_DESCRIPTION = "A string value representing one of the AlarmStatus enumeration value";
    private static final String ALARM_QUERY_STATUS_ALLOWABLE_VALUES = "ACTIVE_UNACK, ACTIVE_ACK, CLEARED_UNACK, CLEARED_ACK";
    /**
     * 字段说明：
     * 1. 保存 `ALARM_QUERY_SEVERITY_ARRAY_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ALARM_QUERY_SEVERITY_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing one of the AlarmSeverity enumeration value";
    private static final String ALARM_QUERY_SEVERITY_ALLOWABLE_VALUES = "CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE";

    /**
     * 字段说明：
     * 1. 保存 `ALARM_QUERY_TYPE_ARRAY_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ALARM_QUERY_TYPE_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing alarm types";
    private static final String ALARM_QUERY_ASSIGNEE_DESCRIPTION = "A string value representing the assignee user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    /**
     * 字段说明：
     * 1. 保存 `ALARM_QUERY_TEXT_SEARCH_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ALARM_QUERY_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on of next alarm fields: type, severity or status";
    private static final String ALARM_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    /**
     * 字段说明：
     * 1. 保存 `ALARM_QUERY_END_TIME_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ALARM_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION = "A boolean value to specify if the alarm originator name will be " +
            "filled in the AlarmInfo object  field: 'originatorName' or will returns as null.";

    @ApiOperation(value = "Get Alarm (getAlarmById)",
            notes = "Fetch the Alarm object based on the provided Alarm Id. " + ALARM_SECURITY_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAlarmById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Alarm getAlarmById(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                              @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        return checkAlarmId(alarmId, Operation.READ);
    }

    @ApiOperation(value = "Get Alarm Info (getAlarmInfoById)",
            notes = "Fetch the Alarm Info object based on the provided Alarm Id. " +
                    ALARM_SECURITY_CHECK + ALARM_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/info/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAlarmInfoById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AlarmInfo getAlarmInfoById(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                                      @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        return checkAlarmInfoId(alarmId, Operation.READ);
    }

    @ApiOperation(value = "Create or Update Alarm (saveAlarm)",
            notes = "Creates or Updates the Alarm. " +
                    "When creating alarm, platform generates Alarm Id as " + UUID_WIKI_LINK +
                    "The newly created Alarm id will be present in the response. Specify existing Alarm id to update the alarm. " +
                    "Referencing non-existing Alarm Id will cause 'Not Found' error. " +
                    "\n\nPlatform also deduplicate the alarms based on the entity id of originator and alarm 'type'. " +
                    "For example, if the user or system component create the alarm with the type 'HighTemperature' for device 'Device A' the new active alarm is created. " +
                    "If the user tries to create 'HighTemperature' alarm for the same device again, the previous alarm will be updated (the 'end_ts' will be set to current timestamp). " +
                    "If the user clears the alarm (see 'Clear Alarm(clearAlarm)'), than new alarm with the same type and same device may be created. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Alarm entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Alarm saveAlarm(@ApiParam(value = "A JSON value representing the alarm.") @RequestBody Alarm alarm) throws ThingsboardException {
        alarm.setTenantId(getTenantId());
        checkNotNull(alarm.getOriginator());
        checkEntity(alarm.getId(), alarm, Resource.ALARM);
        checkEntityId(alarm.getOriginator(), Operation.READ);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (alarm.getAssigneeId() != null) {
            checkUserId(alarm.getAssigneeId(), Operation.READ);
        }
        return tbAlarmService.save(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Delete Alarm (deleteAlarm)",
            notes = "Deletes the Alarm. Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Boolean deleteAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.DELETE);
        return tbAlarmService.delete(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Acknowledge Alarm (ackAlarm)",
            notes = "Acknowledge the Alarm. " +
                    "Once acknowledged, the 'ack_ts' field will be set to current timestamp and special rule chain event 'ALARM_ACK' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/ack", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `ackAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AlarmInfo ackAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        //TODO: return correct error code if the alarm is not found or already cleared
        return tbAlarmService.ack(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Clear Alarm (clearAlarm)",
            notes = "Clear the Alarm. " +
                    "Once cleared, the 'clear_ts' field will be set to current timestamp and special rule chain event 'ALARM_CLEAR' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/clear", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `clearAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AlarmInfo clearAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        //TODO: return correct error code if the alarm is not found or already cleared
        return tbAlarmService.clear(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Assign/Reassign Alarm (assignAlarm)",
            notes = "Assign the Alarm. " +
                    "Once assigned, the 'assign_ts' field will be set to current timestamp and special rule chain event 'ALARM_ASSIGNED' " +
                    "(or ALARM_REASSIGNED in case of assigning already assigned alarm) will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/assign/{assigneeId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `assignAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Alarm assignAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                             @PathVariable(ALARM_ID) String strAlarmId,
                             @ApiParam(value = ASSIGN_ID_PARAM_DESCRIPTION)
                             @PathVariable(ASSIGNEE_ID) String strAssigneeId
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        checkParameter(ASSIGNEE_ID, strAssigneeId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        UserId assigneeId = new UserId(UUID.fromString(strAssigneeId));
        checkUserId(assigneeId, Operation.READ);
        return tbAlarmService.assign(alarm, assigneeId, System.currentTimeMillis(), getCurrentUser());
    }

    @ApiOperation(value = "Unassign Alarm (unassignAlarm)",
            notes = "Unassign the Alarm. " +
                    "Once unassigned, the 'assign_ts' field will be set to current timestamp and special rule chain event 'ALARM_UNASSIGNED' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/assign", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Alarm unassignAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                               @PathVariable(ALARM_ID) String strAlarmId
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        return tbAlarmService.unassign(alarm, System.currentTimeMillis(), getCurrentUser());
    }

    @ApiOperation(value = "Get Alarms (getAlarms)",
            notes = "Returns a page of alarms for the selected entity. Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAlarms` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<AlarmInfo> getAlarms(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
    ) throws ThingsboardException, ExecutionException, InterruptedException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        checkEntityId(entityId, Operation.READ);
        UserId assigneeUserId = null;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(entityId, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)));
    }

    @ApiOperation(value = "Get All Alarms (getAllAlarms)",
            notes = "Returns a page of alarms that belongs to the current user owner. " +
                    "If the user has the authority of 'Tenant Administrator', the server returns alarms that belongs to the tenant of current user. " +
                    "If the user has the authority of 'Customer User', the server returns alarms that belongs to the customer of current user. " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarms", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAllAlarms` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<AlarmInfo> getAllAlarms(
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
    ) throws ThingsboardException, ExecutionException, InterruptedException {
        AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        UserId assigneeUserId = null;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (getCurrentUser().isCustomerUser()) {
            return checkNotNull(alarmService.findCustomerAlarms(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)));
        } else {
            return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)));
        }
    }

    @ApiOperation(value = "Get Alarms (getAlarmsV2)",
            notes = "Returns a page of alarms for the selected entity. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/v2/alarm/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAlarmsV2` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<AlarmInfo> getAlarmsV2(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_ARRAY_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String[] statusList,
            @ApiParam(value = ALARM_QUERY_SEVERITY_ARRAY_DESCRIPTION, allowableValues = ALARM_QUERY_SEVERITY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String[] severityList,
            @ApiParam(value = ALARM_QUERY_TYPE_ARRAY_DESCRIPTION)
            @RequestParam(required = false) String[] typeList,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException, ExecutionException, InterruptedException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);
        List<AlarmSearchStatus> alarmStatusList = new ArrayList<>();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (statusList != null) {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (String strStatus : statusList) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!StringUtils.isEmpty(strStatus)) {
                    alarmStatusList.add(AlarmSearchStatus.valueOf(strStatus));
                }
            }
        }
        List<AlarmSeverity> alarmSeverityList = new ArrayList<>();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (severityList != null) {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (String strSeverity : severityList) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!StringUtils.isEmpty(strSeverity)) {
                    alarmSeverityList.add(AlarmSeverity.valueOf(strSeverity));
                }
            }
        }
        List<String> alarmTypeList = typeList != null ? Arrays.asList(typeList) : Collections.emptyList();
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        return checkNotNull(alarmService.findAlarmsV2(getCurrentUser().getTenantId(), new AlarmQueryV2(entityId, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, assigneeUserId)));
    }

    @ApiOperation(value = "Get All Alarms (getAllAlarmsV2)",
            notes = "Returns a page of alarms that belongs to the current user owner. " +
                    "If the user has the authority of 'Tenant Administrator', the server returns alarms that belongs to the tenant of current user. " +
                    "If the user has the authority of 'Customer User', the server returns alarms that belongs to the customer of current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/v2/alarms", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAllAlarmsV2` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<AlarmInfo> getAllAlarmsV2(
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_ARRAY_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String[] statusList,
            @ApiParam(value = ALARM_QUERY_SEVERITY_ARRAY_DESCRIPTION, allowableValues = ALARM_QUERY_SEVERITY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String[] severityList,
            @ApiParam(value = ALARM_QUERY_TYPE_ARRAY_DESCRIPTION)
            @RequestParam(required = false) String[] typeList,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException, ExecutionException, InterruptedException {
        List<AlarmSearchStatus> alarmStatusList = new ArrayList<>();
        if (statusList != null) {
            for (String strStatus : statusList) {
                if (!StringUtils.isEmpty(strStatus)) {
                    alarmStatusList.add(AlarmSearchStatus.valueOf(strStatus));
                }
            }
        }
        List<AlarmSeverity> alarmSeverityList = new ArrayList<>();
        if (severityList != null) {
            for (String strSeverity : severityList) {
                if (!StringUtils.isEmpty(strSeverity)) {
                    alarmSeverityList.add(AlarmSeverity.valueOf(strSeverity));
                }
            }
        }
        List<String> alarmTypeList = typeList != null ? Arrays.asList(typeList) : Collections.emptyList();
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        if (getCurrentUser().isCustomerUser()) {
            return checkNotNull(alarmService.findCustomerAlarmsV2(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), new AlarmQueryV2(null, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, assigneeUserId)));
        } else {
            return checkNotNull(alarmService.findAlarmsV2(getCurrentUser().getTenantId(), new AlarmQueryV2(null, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, assigneeUserId)));
        }
    }

    @ApiOperation(value = "Get Highest Alarm Severity (getHighestAlarmSeverity)",
            notes = "Search the alarms by originator ('entityType' and entityId') and optional 'status' or 'searchStatus' filters and returns the highest AlarmSeverity(CRITICAL, MAJOR, MINOR, WARNING or INDETERMINATE). " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/highestSeverity/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getHighestAlarmSeverity` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AlarmSeverity getHighestAlarmSeverity(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId
    ) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        checkEntityId(entityId, Operation.READ);
        return alarmService.findHighestAlarmSeverity(getCurrentUser().getTenantId(), entityId, alarmSearchStatus,
                alarmStatus, assigneeId);
    }

    @ApiOperation(value = "Get Alarm Types (getAlarmTypes)",
            notes = "Returns a set of unique alarm types based on alarms that are either owned by the tenant or assigned to the customer which user is performing the request.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/types", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAlarmTypes` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<EntitySubtype> getAlarmTypes(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                 @RequestParam int pageSize,
                                                 @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                 @RequestParam int page,
                                                 @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
                                                 @RequestParam(required = false) String textSearch,
                                                 @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String sortOrder) throws ThingsboardException, ExecutionException, InterruptedException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, "type", sortOrder);
        return checkNotNull(alarmService.findAlarmTypesByTenantId(getTenantId(), pageLink));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
