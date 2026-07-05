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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.AUDIT_LOG_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.AUDIT_LOG_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID_PARAM_DESCRIPTION;

/**
 * 中文说明：
 * 1. 类目的：`AuditLogController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class AuditLogController extends BaseController {

    /**
     * `field` 类，封装当前模块中的一组相关职责。
     */
    private static final String AUDIT_LOG_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the AuditLog class field: 'createdTime'.";
    private static final String AUDIT_LOG_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the AuditLog class field: 'createdTime'.";
    private static final String AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION = "A String value representing comma-separated list of action types. " +
            "This parameter is optional, but it can be used to filter results to fetch only audit logs of specific action types. " +
            "For example, 'LOGIN', 'LOGOUT'. See the 'Model' tab of the Response Class for more details.";
    private static final String AUDIT_LOG_SORT_PROPERTY_DESCRIPTION = "Property of audit log to sort by. " +
            "See the 'Model' tab of the Response Class for more details. " +
            "Note: entityType sort property is not defined in the AuditLog class, however, it can be used to sort audit logs by types of entities that were logged.";


    /**
     * 功能：获取客户ID。
     * 参数：
     * - `strCustomerId`：客户IDID。
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get audit logs by customer id (getAuditLogsByCustomerId)",
            notes = "Returns a page of audit logs related to the targeted customer entities (devices, assets, etc.), " +
                    "and users actions (login, logout, etc.) that belong to this customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/customer/{customerId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByCustomerId(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, allowableValues = AUDIT_LOG_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        checkParameter("CustomerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        return checkNotNull(auditLogService.findAuditLogsByTenantIdAndCustomerId(tenantId, new CustomerId(UUID.fromString(strCustomerId)), actionTypes, pageLink));
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `strUserId`：用户ID。
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get audit logs by user id (getAuditLogsByUserId)",
            notes = "Returns a page of audit logs related to the actions of targeted user. " +
                    "For example, RPC call to a particular device, or alarm acknowledgment for a specific device, etc. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/user/{userId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByUserId(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable("userId") String strUserId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, allowableValues = AUDIT_LOG_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        checkParameter("UserId", strUserId);
        TenantId tenantId = getCurrentUser().getTenantId();
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        return checkNotNull(auditLogService.findAuditLogsByTenantIdAndUserId(tenantId, new UserId(UUID.fromString(strUserId)), actionTypes, pageLink));
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `ENTITY_TYPE_PARAM_DESCRIPTION`：实体对象。
     * - `true`：`true` 参数。
     * - `strEntityType`：实体对象。
     * - `ENTITY_ID_PARAM_DESCRIPTION`：实体对象。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get audit logs by entity id (getAuditLogsByEntityId)",
            notes = "Returns a page of audit logs related to the actions on the targeted entity. " +
                    "Basically, this API call is used to get the full lifecycle of some specific entity. " +
                    "For example to see when a device was created, updated, assigned to some customer, or even deleted from the system. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/entity/{entityType}/{entityId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByEntityId(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable("entityType") String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String strEntityId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, allowableValues = AUDIT_LOG_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = getCurrentUser().getTenantId();
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        return checkNotNull(auditLogService.findAuditLogsByTenantIdAndEntityId(tenantId, EntityIdFactory.getByTypeAndId(strEntityType, strEntityId), actionTypes, pageLink));
    }

    /**
     * 功能：获取`Audit Logs`。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `AUDIT_LOG_SORT_PROPERTY_DESCRIPTION`：`AUDIT_LOG_SORT_PROPERTY_DESCRIPTION` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get all audit logs (getAuditLogs)",
            notes = "Returns a page of audit logs related to all entities in the scope of the current user's Tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogs(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, allowableValues = AUDIT_LOG_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        return checkNotNull(auditLogService.findAuditLogsByTenantId(tenantId, actionTypes, pageLink));
    }

    /**
     * 功能：解析`Action Types Str`。
     * 参数：
     * - `actionTypesStr`：类型。
     * 返回：匹配的数据集合。
     */
    private List<ActionType> parseActionTypesStr(String actionTypesStr) {
        List<ActionType> result = null;
        if (StringUtils.isNoneBlank(actionTypesStr)) {
            String[] tmp = actionTypesStr.split(",");
            result = Arrays.stream(tmp).map(at -> ActionType.valueOf(at.toUpperCase())).collect(Collectors.toList());
        }
        return result;
    }

    /**
     * 功能：获取时间。
     * 参数：
     * - `startTime`：开始时间戳。
     * 返回：数值结果。
     */
    private Long getStartTime(Long startTime) {
        if (startTime == null) {
            return 1L;
        }
        return startTime;
    }

    /**
     * 功能：获取时间。
     * 参数：
     * - `endTime`：结束时间戳。
     * 返回：数值结果。
     */
    private Long getEndTime(Long endTime) {
        if (endTime == null) {
            return System.currentTimeMillis();
        }
        return endTime;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AuditLogController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
