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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.HomeDashboard;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
/**
 * 中文说明：
 * 1. 类目的：`DashboardController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class DashboardController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `tbDashboardService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbDashboardService tbDashboardService;
    private final ImageService imageService;
    /**
     * 字段说明：
     * 1. 保存 `DASHBOARD_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String DASHBOARD_ID = "dashboardId";

    /**
     * 字段说明：
     * 1. 保存 `HOME_DASHBOARD_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String HOME_DASHBOARD_ID = "homeDashboardId";
    private static final String HOME_DASHBOARD_HIDE_TOOLBAR = "homeDashboardHideToolbar";
    /**
     * 字段说明：
     * 1. 保存 `DASHBOARD_INFO_DEFINITION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String DASHBOARD_INFO_DEFINITION = "The Dashboard Info object contains lightweight information about the dashboard (e.g. title, image, assigned customers) but does not contain the heavyweight configuration JSON.";
    public static final String DASHBOARD_DEFINITION = "The Dashboard object is a heavyweight object that contains information about the dashboard (e.g. title, image, assigned customers) and also configuration JSON (e.g. layouts, widgets, entity aliases).";
    /**
     * 字段说明：
     * 1. 保存 `HIDDEN_FOR_MOBILE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String HIDDEN_FOR_MOBILE = "Exclude dashboards that are hidden for mobile";

    @Value("${ui.dashboard.max_datapoints_limit}")
    /**
     * 字段说明：
     * 1. 保存 `maxDatapointsLimit` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long maxDatapointsLimit;

    @ApiOperation(value = "Get server time (getServerTime)",
            notes = "Get the server time (milliseconds since January 1, 1970 UTC). " +
                    "Used to adjust view of the dashboards according to the difference between browser and server time.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/serverTime", method = RequestMethod.GET)
    @ResponseBody
    @ApiResponse(code = 200, message = "OK", examples = @Example(value = @ExampleProperty(value = "1636023857137", mediaType = "application/json")))
    /**
     * 方法说明：
     * 1. 职责：执行 `getServerTime` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public long getServerTime() throws ThingsboardException {
        return System.currentTimeMillis();
    }

    @ApiOperation(value = "Get max data points limit (getMaxDatapointsLimit)",
            notes = "Get the maximum number of data points that dashboard may request from the server per in a single subscription command. " +
                    "This value impacts the time window behavior. It impacts 'Max values' parameter in case user selects 'None' as 'Data aggregation function'. " +
                    "It also impacts the 'Grouping interval' in case of any other 'Data aggregation function' is selected. " +
                    "The actual value of the limit is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/maxDatapointsLimit", method = RequestMethod.GET)
    @ResponseBody
    @ApiResponse(code = 200, message = "OK", examples = @Example(value = @ExampleProperty(value = "5000", mediaType = "application/json")))
    /**
     * 方法说明：
     * 1. 职责：执行 `getMaxDatapointsLimit` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public long getMaxDatapointsLimit() throws ThingsboardException {
        return maxDatapointsLimit;
    }

    @ApiOperation(value = "Get Dashboard Info (getDashboardInfoById)",
            notes = "Get the information about the dashboard based on 'dashboardId' parameter. " + DASHBOARD_INFO_DEFINITION,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/info/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDashboardInfoById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DashboardInfo getDashboardInfoById(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        return checkDashboardInfoId(dashboardId, Operation.READ);
    }

    @ApiOperation(value = "Get Dashboard (getDashboardById)",
            notes = "Get the dashboard based on 'dashboardId' parameter. " + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDashboardById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard getDashboardById(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = INLINE_IMAGES_DESCRIPTION)
            @RequestParam(value = INLINE_IMAGES, required = false) boolean inlineImages) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        var result = checkDashboardId(dashboardId, Operation.READ);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (inlineImages) {
            imageService.inlineImages(result);
        }
        return result;
    }

    @ApiOperation(value = "Create Or Update Dashboard (saveDashboard)",
            notes = "Create or update the Dashboard. When creating dashboard, platform generates Dashboard Id as " + UUID_WIKI_LINK +
                    "The newly created Dashboard id will be present in the response. " +
                    "Specify existing Dashboard id to update the dashboard. " +
                    "Referencing non-existing dashboard Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Dashboard entity. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveDashboard` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard saveDashboard(
            @ApiParam(value = "A JSON value representing the dashboard.")
            @RequestBody Dashboard dashboard) throws Exception {
        dashboard.setTenantId(getTenantId());
        checkEntity(dashboard.getId(), dashboard, Resource.DASHBOARD);
        return tbDashboardService.save(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Delete the Dashboard (deleteDashboard)",
            notes = "Delete the Dashboard." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteDashboard` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteDashboard(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);
        tbDashboardService.delete(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Assign the Dashboard (assignDashboardToCustomer)",
            notes = "Assign the Dashboard to specified Customer or do nothing if the Dashboard is already assigned to that Customer. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignDashboardToCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard assignDashboardToCustomer(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);

        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDashboardService.assignDashboardToCustomer(dashboard, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign the Dashboard (unassignDashboardFromCustomer)",
            notes = "Unassign the Dashboard from specified Customer or do nothing if the Dashboard is already assigned to that Customer. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignDashboardFromCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard unassignDashboardFromCustomer(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DASHBOARD_ID, strDashboardId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        return tbDashboardService.unassignDashboardFromCustomer(dashboard, customer, getCurrentUser());
    }

    @ApiOperation(value = "Update the Dashboard Customers (updateDashboardCustomers)",
            notes = "Updates the list of Customers that this Dashboard is assigned to. Removes previous assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object. " + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}/customers", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `updateDashboardCustomers` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard updateDashboardCustomers(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = "JSON array with the list of customer ids, or empty to remove all customers")
            @RequestBody(required = false) String[] strCustomerIds) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.updateDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Adds the Dashboard Customers (addDashboardCustomers)",
            notes = "Adds the list of Customers to the existing list of assignments for the Dashboard. Keeps previous assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}/customers/add", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `addDashboardCustomers` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard addDashboardCustomers(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = "JSON array with the list of customer ids")
            @RequestBody String[] strCustomerIds) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.addDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Remove the Dashboard Customers (removeDashboardCustomers)",
            notes = "Removes the list of Customers from the existing list of assignments for the Dashboard. Keeps other assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}/customers/remove", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `removeDashboardCustomers` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard removeDashboardCustomers(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = "JSON array with the list of customer ids")
            @RequestBody String[] strCustomerIds) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.removeDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Assign the Dashboard to Public Customer (assignDashboardToPublicCustomer)",
            notes = "Assigns the dashboard to a special, auto-generated 'Public' Customer. Once assigned, unauthenticated users may browse the dashboard. " +
                    "This method is useful if you like to embed the dashboard on public web pages to be available for users that are not logged in. " +
                    "Be aware that making the dashboard public does not mean that it automatically makes all devices and assets you use in the dashboard to be public." +
                    "Use [assign Asset to Public Customer](#!/asset-controller/assignAssetToPublicCustomerUsingPOST) and " +
                    "[assign Device to Public Customer](#!/device-controller/assignDeviceToPublicCustomerUsingPOST) for this purpose. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignDashboardToPublicCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard assignDashboardToPublicCustomer(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDashboardService.assignDashboardToPublicCustomer(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Unassign the Dashboard from Public Customer (unassignDashboardFromPublicCustomer)",
            notes = "Unassigns the dashboard from a special, auto-generated 'Public' Customer. Once unassigned, unauthenticated users may no longer browse the dashboard. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignDashboardFromPublicCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard unassignDashboardFromPublicCustomer(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        return tbDashboardService.unassignDashboardFromPublicCustomer(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Dashboards by System Administrator (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by tenant. " + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS +
                    SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantDashboards` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DashboardInfo> getTenantDashboards(
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        checkTenantId(tenantId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Dashboards (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by the tenant of a current user. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantDashboards` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DashboardInfo> getTenantDashboards(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (mobile != null && mobile) {
            return checkNotNull(dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink));
        } else {
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Customer Dashboards (getCustomerDashboards)",
            notes = "Returns a page of dashboard info objects owned by the specified customer. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getCustomerDashboards` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DashboardInfo> getCustomerDashboards(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (mobile != null && mobile) {
            return checkNotNull(dashboardService.findMobileDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        } else {
            return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get Home Dashboard (getHomeDashboard)",
            notes = "Returns the home dashboard object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. "
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/home", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getHomeDashboard` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public HomeDashboard getHomeDashboard() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (securityUser.isSystemAdmin()) {
            return null;
        }
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        JsonNode additionalInfo = user.getAdditionalInfo();
        HomeDashboard homeDashboard;
        homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (homeDashboard == null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (securityUser.isCustomerUser()) {
                Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                additionalInfo = customer.getAdditionalInfo();
                homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (homeDashboard == null) {
                Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                additionalInfo = tenant.getAdditionalInfo();
                homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
            }
        }
        return homeDashboard;
    }

    @ApiOperation(value = "Get Home Dashboard Info (getHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getHomeDashboardInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public HomeDashboardInfo getHomeDashboardInfo() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (securityUser.isSystemAdmin()) {
            return null;
        }
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        JsonNode additionalInfo = user.getAdditionalInfo();
        HomeDashboardInfo homeDashboardInfo;
        homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (homeDashboardInfo == null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (securityUser.isCustomerUser()) {
                Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                additionalInfo = customer.getAdditionalInfo();
                homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (homeDashboardInfo == null) {
                Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                additionalInfo = tenant.getAdditionalInfo();
                homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
            }
        }
        return homeDashboardInfo;
    }

    @ApiOperation(value = "Get Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the corresponding tenant. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantHomeDashboardInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public HomeDashboardInfo getTenantHomeDashboardInfo() throws ThingsboardException {
        Tenant tenant = tenantService.findTenantById(getTenantId());
        JsonNode additionalInfo = tenant.getAdditionalInfo();
        DashboardId dashboardId = null;
        boolean hideDashboardToolbar = true;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
            String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
            dashboardId = new DashboardId(toUUID(strDashboardId));
            if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
            }
        }
        return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
    }

    @ApiOperation(value = "Update Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Update the home dashboard assignment for the current tenant. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `setTenantHomeDashboardInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setTenantHomeDashboardInfo(
            @ApiParam(value = "A JSON object that represents home dashboard id and other parameters", required = true)
            @RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {

        if (homeDashboardInfo.getDashboardId() != null) {
            checkDashboardId(homeDashboardInfo.getDashboardId(), Operation.READ);
        }
        Tenant tenant = tenantService.findTenantById(getTenantId());
        JsonNode additionalInfo = tenant.getAdditionalInfo();
        if (additionalInfo == null || !(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        if (homeDashboardInfo.getDashboardId() != null) {
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, homeDashboardInfo.getDashboardId().getId().toString());
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, homeDashboardInfo.isHideDashboardToolbar());
        } else {
            ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_ID);
            ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_HIDE_TOOLBAR);
        }
        tenant.setAdditionalInfo(additionalInfo);
        tenantService.saveTenant(tenant);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractHomeDashboardInfoFromAdditionalInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private HomeDashboardInfo extractHomeDashboardInfoFromAdditionalInfo(JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractHomeDashboardFromAdditionalInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private HomeDashboard extractHomeDashboardFromAdditionalInfo(JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboard(dashboard, hideDashboardToolbar);
            }
        } catch (Exception e) {
        }
        return null;
    }

    @ApiOperation(value = "Assign dashboard to edge (assignDashboardToEdge)",
            notes = "Creates assignment of an existing dashboard to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment dashboard " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once dashboard will be delivered to edge service, it's going to be available for usage on remote edge instance." +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignDashboardToEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard assignDashboardToEdge(@PathVariable("edgeId") String strEdgeId,
                                           @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        checkDashboardId(dashboardId, Operation.READ);
        return tbDashboardService.asignDashboardToEdge(getTenantId(), dashboardId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign dashboard from edge (unassignDashboardFromEdge)",
            notes = "Clears assignment of the dashboard to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove dashboard " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove dashboard locally." +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignDashboardFromEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Dashboard unassignDashboardFromEdge(@PathVariable("edgeId") String strEdgeId,
                                               @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);

        return tbDashboardService.unassignDashboardFromEdge(dashboard, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Edge Dashboards (getEdgeDashboards)",
            notes = "Returns a page of dashboard info objects assigned to the specified edge. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeDashboards` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DashboardInfo> getEdgeDashboards(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<DashboardInfo> nonFilteredResult = dashboardService.findDashboardsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
        List<DashboardInfo> filteredDashboards = nonFilteredResult.getData().stream().filter(dashboardInfo -> {
            try {
                accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ, dashboardInfo.getId(), dashboardInfo);
                return true;
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
        PageData<DashboardInfo> filteredResult = new PageData<>(filteredDashboards,
                nonFilteredResult.getTotalPages(),
                nonFilteredResult.getTotalElements(),
                nonFilteredResult.hasNext());
        return checkNotNull(filteredResult);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `customerIdFromStr` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Set<CustomerId> customerIdFromStr(String[] strCustomerIds) {
        Set<CustomerId> customerIds = new HashSet<>();
        if (strCustomerIds != null) {
            for (String strCustomerId : strCustomerIds) {
                customerIds.add(new CustomerId(UUID.fromString(strCustomerId)));
            }
        }
        return customerIds;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
