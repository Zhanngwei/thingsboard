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

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.thingsboard.rule.engine.flow.TbRuleChainInputNode;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeBulkImportService;
import org.thingsboard.server.service.edge.instructions.EdgeInstallInstructionsService;
import org.thingsboard.server.service.edge.instructions.EdgeUpgradeInstructionsService;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`EdgeController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class EdgeController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `edgeBulkImportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final EdgeBulkImportService edgeBulkImportService;
    private final TbEdgeService tbEdgeService;
    /**
     * 字段说明：
     * 1. 保存 `edgeRpcServiceOpt` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final Optional<EdgeRpcService> edgeRpcServiceOpt;
    private final Optional<EdgeInstallInstructionsService> edgeInstallServiceOpt;
    /**
     * 字段说明：
     * 1. 保存 `edgeUpgradeServiceOpt` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final Optional<EdgeUpgradeInstructionsService> edgeUpgradeServiceOpt;

    /**
     * 字段说明：
     * 1. 保存 `EDGE_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String EDGE_ID = "edgeId";
    public static final String EDGE_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the edge is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the edge is assigned to the same customer.";

    @ApiOperation(value = "Is edges support enabled (isEdgesSupportEnabled)",
            notes = "Returns 'true' if edges support enabled on server, 'false' - otherwise.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges/enabled", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `isEdgesSupportEnabled` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean isEdgesSupportEnabled() {
        return edgesEnabled;
    }

    @ApiOperation(value = "Get Edge (getEdgeById)",
            notes = "Get the Edge object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Edge getEdgeById(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                            @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        return checkEdgeId(edgeId, Operation.READ);
    }

    @ApiOperation(value = "Get Edge Info (getEdgeInfoById)",
            notes = "Get the Edge Info object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/info/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeInfoById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EdgeInfo getEdgeInfoById(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                    @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        return checkEdgeInfoId(edgeId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Edge (saveEdge)",
            notes = "Create or update the Edge. When creating edge, platform generates Edge Id as " + UUID_WIKI_LINK +
                    "The newly created edge id will be present in the response. " +
                    "Specify existing Edge id to update the edge. " +
                    "Referencing non-existing Edge Id will cause 'Not Found' error." +
                    "\n\nEdge name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the edge names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Edge entity. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Edge saveEdge(@ApiParam(value = "A JSON value representing the edge.", required = true)
                         @RequestBody Edge edge) throws Exception {
        TenantId tenantId = getTenantId();
        edge.setTenantId(tenantId);
        boolean created = edge.getId() == null;

        RuleChain edgeTemplateRootRuleChain = null;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (created) {
            edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(tenantId);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (edgeTemplateRootRuleChain == null) {
                throw new DataValidationException("Root edge rule chain is not available!");
            }
        }

        Operation operation = created ? Operation.CREATE : Operation.WRITE;

        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, operation, edge.getId(), edge);

        return tbEdgeService.save(edge, edgeTemplateRootRuleChain, getCurrentUser());
    }

    @ApiOperation(value = "Delete edge (deleteEdge)",
            notes = "Deletes the edge. Referencing non-existing edge Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                           @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.DELETE);
        tbEdgeService.delete(edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Edges (getEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdges` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<Edge> getEdges(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                   @RequestParam int pageSize,
                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                   @RequestParam int page,
                                   @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
                                   @RequestParam(required = false) String textSearch,
                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                   @RequestParam(required = false) String sortProperty,
                                   @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Assign edge to customer (assignEdgeToCustomer)",
            notes = "Creates assignment of the edge to customer. Customer will be able to query edge afterwards." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/edge/{edgeId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignEdgeToCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Edge assignEdgeToCustomer(@ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable("customerId") String strCustomerId,
                                     @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(EDGE_ID, strEdgeId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);
        return tbEdgeService.assignEdgeToCustomer(getTenantId(), edgeId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign edge from customer (unassignEdgeFromCustomer)",
            notes = "Clears assignment of the edge to customer. Customer will not be able to query edge afterwards." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/edge/{edgeId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignEdgeFromCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Edge unassignEdgeFromCustomer(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                         @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.UNASSIGN_FROM_CUSTOMER);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (edge.getCustomerId() == null || edge.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new IncorrectParameterException("Edge isn't assigned to any customer!");
        }
        Customer customer = checkCustomerId(edge.getCustomerId(), Operation.READ);

        return tbEdgeService.unassignEdgeFromCustomer(edge, customer, getCurrentUser());
    }

    @ApiOperation(value = "Make edge publicly available (assignEdgeToPublicCustomer)",
            notes = "Edge will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/edge/{edgeId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignEdgeToPublicCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Edge assignEdgeToPublicCustomer(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                           @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);
        return tbEdgeService.assignEdgeToPublicCustomer(getTenantId(), edgeId, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Edges (getTenantEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantEdges` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<Edge> getTenantEdges(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(edgeService.findEdgesByTenantIdAndType(tenantId, type, pageLink));
        } else {
            return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Tenant Edge Infos (getTenantEdgeInfos)",
            notes = "Returns a page of edges info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + EDGE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edgeInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantEdgeInfos` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<EdgeInfo> getTenantEdgeInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(edgeService.findEdgeInfosByTenantIdAndType(tenantId, type, pageLink));
        } else {
            return checkNotNull(edgeService.findEdgeInfosByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Tenant Edge (getTenantEdge)",
            notes = "Requested edge must be owned by tenant or customer that the user belongs to. " +
                    "Edge name is an unique property of edge. So it can be used to identify the edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"edgeName"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Edge getTenantEdge(@ApiParam(value = "Unique name of the edge", required = true)
                              @RequestParam String edgeName) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(edgeService.findEdgeByTenantIdAndName(tenantId, edgeName));
    }

    @ApiOperation(value = "Set root rule chain for provided edge (setEdgeRootRuleChain)",
            notes = "Change root rule chain of the edge to the new provided rule chain. \n" +
                    "This operation will send a notification to update root rule chain on remote edge service." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/{ruleChainId}/root", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `setEdgeRootRuleChain` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Edge setEdgeRootRuleChain(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable(EDGE_ID) String strEdgeId,
                                     @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable("ruleChainId") String strRuleChainId) throws Exception {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter("ruleChainId", strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.WRITE);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.WRITE, edge.getId(), edge);
        return tbEdgeService.setEdgeRootRuleChain(edge, ruleChainId, getCurrentUser());
    }

    @ApiOperation(value = "Get Customer Edges (getCustomerEdges)",
            notes = "Returns a page of edges objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getCustomerEdges` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<Edge> getCustomerEdges(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<Edge> result;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            result = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink);
        } else {
            result = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        }
        return checkNotNull(result);
    }

    @ApiOperation(value = "Get Customer Edge Infos (getCustomerEdgeInfos)",
            notes = "Returns a page of edges info objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + EDGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/edgeInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getCustomerEdgeInfos` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<EdgeInfo> getCustomerEdgeInfos(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<EdgeInfo> result;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            result = edgeService.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink);
        } else {
            result = edgeService.findEdgeInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        }
        return checkNotNull(result);
    }

    @ApiOperation(value = "Get Edges By Ids (getEdgesByIds)",
            notes = "Requested edges must be owned by tenant or assigned to customer which user is performing the request." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", params = {"edgeIds"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgesByIds` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<Edge> getEdgesByIds(
            @ApiParam(value = "A list of edges ids, separated by comma ','", required = true)
            @RequestParam("edgeIds") String[] strEdgeIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("edgeIds", strEdgeIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = user.getCustomerId();
        List<EdgeId> edgeIds = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (String strEdgeId : strEdgeIds) {
            edgeIds.add(new EdgeId(toUUID(strEdgeId)));
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        ListenableFuture<List<Edge>> edgesFuture;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (customerId == null || customerId.isNullUid()) {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            edgesFuture = edgeService.findEdgesByTenantIdAndIdsAsync(tenantId, edgeIds);
        } else {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            edgesFuture = edgeService.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, edgeIds);
        }
        List<Edge> edges = edgesFuture.get();
        return checkNotNull(edges);
    }

    @ApiOperation(value = "Find related edges (findByQuery)",
            notes = "Returns all edges that are related to the specific entity. " +
                    "The entity id, relation type, edge types, depth of the search, and other query parameters defined using complex 'EdgeSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findByQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<Edge> findByQuery(@RequestBody EdgeSearchQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEdgeTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<Edge> edges = checkNotNull(edgeService.findEdgesByQuery(tenantId, query).get());
        edges = edges.stream().filter(edge -> {
            try {
                accessControlService.checkPermission(user, Resource.EDGE, Operation.READ, edge.getId(), edge);
                return true;
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
        return edges;
    }

    @ApiOperation(value = "Get Edge Types (getEdgeTypes)",
            notes = "Returns a set of unique edge types based on edges that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/types", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeTypes` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntitySubtype> getEdgeTypes() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        ListenableFuture<List<EntitySubtype>> edgeTypes = edgeService.findEdgeTypesByTenantId(tenantId);
        return checkNotNull(edgeTypes.get());
    }

    @ApiOperation(value = "Sync edge (syncEdge)",
            notes = "Starts synchronization process between edge and cloud. \n" +
                    "All entities that are assigned to particular edge are going to be send to remote edge service." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/sync/{edgeId}", method = RequestMethod.POST)
    /**
     * 方法说明：
     * 1. 职责：执行 `syncEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<ResponseEntity> syncEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                         @PathVariable("edgeId") String strEdgeId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        final DeferredResult<ResponseEntity> response = new DeferredResult<>();
        if (isEdgesEnabled() && edgeRpcServiceOpt.isPresent()) {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ToEdgeSyncRequest request = new ToEdgeSyncRequest(UUID.randomUUID(), tenantId, edgeId);
            edgeRpcServiceOpt.get().processSyncRequest(request, fromEdgeSyncResponse -> reply(response, fromEdgeSyncResponse));
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
        return response;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reply` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void reply(DeferredResult<ResponseEntity> response, FromEdgeSyncResponse fromEdgeSyncResponse) {
        if (fromEdgeSyncResponse.isSuccess()) {
            response.setResult(new ResponseEntity<>(HttpStatus.OK));
        } else {
            response.setErrorResult(new ThingsboardException("Edge is not connected", ThingsboardErrorCode.GENERAL));
        }
    }

    @ApiOperation(value = "Find missing rule chains (findMissingToRelatedRuleChains)",
            notes = "Returns list of rule chains ids that are not assigned to particular edge, but these rule chains are present in the already assigned rule chains to edge." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/missingToRelatedRuleChains/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findMissingToRelatedRuleChains` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String findMissingToRelatedRuleChains(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                                 @PathVariable("edgeId") String strEdgeId) throws ThingsboardException {
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        edgeId = checkNotNull(edgeId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        return edgeService.findMissingToRelatedRuleChains(tenantId, edgeId, TbRuleChainInputNode.class.getName());
    }

    @ApiOperation(value = "Import the bulk of edges (processEdgesBulkImport)",
            notes = "There's an ability to import the bulk of edges using the only .csv file." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping("/edge/bulk_import")
    /**
     * 方法说明：
     * 1. 职责：执行 `processEdgesBulkImport` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BulkImportResult<Edge> processEdgesBulkImport(@RequestBody BulkImportRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(user.getTenantId());
        if (edgeTemplateRootRuleChain == null) {
            throw new DataValidationException("Root edge rule chain is not available!");
        }

        return edgeBulkImportService.processBulkImport(request, user);
    }

    @ApiOperation(value = "Get Edge Install Instructions (getEdgeInstallInstructions)",
            notes = "Get an install instructions for provided edge id." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/instructions/install/{edgeId}/{method}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeInstallInstructions` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EdgeInstructions getEdgeInstallInstructions(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("edgeId") String strEdgeId,
            @ApiParam(value = "Installation method ('docker', 'ubuntu' or 'centos')", allowableValues = "docker, ubuntu, centos")
            @PathVariable("method") String installationMethod,
            HttpServletRequest request) throws ThingsboardException {
        if (isEdgesEnabled() && edgeInstallServiceOpt.isPresent()) {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            Edge edge = checkEdgeId(edgeId, Operation.READ);
            return checkNotNull(edgeInstallServiceOpt.get().getInstallInstructions(edge, installationMethod, request));
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
    }

    @ApiOperation(value = "Get Edge Upgrade Instructions (getEdgeUpgradeInstructions)",
            notes = "Get an upgrade instructions for provided edge version." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/instructions/upgrade/{edgeVersion}/{method}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeUpgradeInstructions` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EdgeInstructions getEdgeUpgradeInstructions(
            @ApiParam(value = "Edge version", required = true)
            @PathVariable("edgeVersion") String edgeVersion,
            @ApiParam(value = "Upgrade method ('docker', 'ubuntu' or 'centos')", allowableValues = "docker, ubuntu, centos")
            @PathVariable("method") String method) throws Exception {
        if (isEdgesEnabled() && edgeUpgradeServiceOpt.isPresent()) {
            return checkNotNull(edgeUpgradeServiceOpt.get().getUpgradeInstructions(edgeVersion, method));
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
    }

    @ApiOperation(value = "Is edge upgrade enabled (isEdgeUpgradeAvailable)",
            notes = "Returns 'true' if upgrade available for connected edge, 'false' - otherwise.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/upgrade/available", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `isEdgeUpgradeAvailable` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean isEdgeUpgradeAvailable(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("edgeId") String strEdgeId) throws Exception {
        if (isEdgesEnabled() && edgeUpgradeServiceOpt.isPresent()) {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            Edge edge = checkEdgeId(edgeId, Operation.READ);
            return edgeUpgradeServiceOpt.get().isUpgradeAvailable(edge.getTenantId(), edge.getId());
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
