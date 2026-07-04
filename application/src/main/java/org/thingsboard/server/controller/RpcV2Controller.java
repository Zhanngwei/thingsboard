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

import com.google.common.util.concurrent.FutureCallback;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.ToErrorResponseEntity;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.service.security.permission.Operation;

import javax.annotation.Nullable;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RPC_ID;
import static org.thingsboard.server.controller.ControllerConstants.RPC_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RPC_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RPC_STATUS_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RPC_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.RPC_V2_URL_PREFIX)
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`RpcV2Controller` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class RpcV2Controller extends AbstractRpcController {

    private static final String RPC_REQUEST_DESCRIPTION = "Sends the one-way remote-procedure call (RPC) request to device. " +
            "The RPC call is A JSON that contains the method name ('method'), parameters ('params') and multiple optional fields. " +
            "See example below. We will review the properties of the RPC call one-by-one below. " +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"method\": \"setGpio\",\n" +
            "  \"params\": {\n" +
            "    \"pin\": 7,\n" +
            "    \"value\": 1\n" +
            "  },\n" +
            "  \"persistent\": false,\n" +
            "  \"timeout\": 5000\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n### Server-side RPC structure\n" +
            "\n" +
            "The body of server-side RPC request consists of multiple fields:\n" +
            "\n" +
            "* **method** - mandatory, name of the method to distinct the RPC calls.\n" +
            "  For example, \"getCurrentTime\" or \"getWeatherForecast\". The value of the parameter is a string.\n" +
            "* **params** - mandatory, parameters used for processing of the request. The value is a JSON. Leave empty JSON \"{}\" if no parameters needed.\n" +
            "* **timeout** - optional, value of the processing timeout in milliseconds. The default value is 10000 (10 seconds). The minimum value is 5000 (5 seconds).\n" +
            "* **expirationTime** - optional, value of the epoch time (in milliseconds, UTC timezone). Overrides **timeout** if present.\n" +
            "* **persistent** - optional, indicates persistent RPC. The default value is \"false\".\n" +
            "* **retries** - optional, defines how many times persistent RPC will be re-sent in case of failures on the network and/or device side.\n" +
            "* **additionalInfo** - optional, defines metadata for the persistent RPC that will be added to the persistent RPC events.";

    private static final String ONE_WAY_RPC_RESULT = "\n\n### RPC Result\n" +
            "In case of persistent RPC, the result of this call is 'rpcId' UUID. In case of lightweight RPC, " +
            "the result of this call is either 200 OK if the message was sent to device, or 504 Gateway Timeout if device is offline.";

    private static final String TWO_WAY_RPC_RESULT = "\n\n### RPC Result\n" +
            "In case of persistent RPC, the result of this call is 'rpcId' UUID. In case of lightweight RPC, " +
            "the result of this call is the response from device, or 504 Gateway Timeout if device is offline.";

    /**
     * 字段说明：
     * 1. 保存 `ONE_WAY_RPC_REQUEST_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ONE_WAY_RPC_REQUEST_DESCRIPTION = "Sends the one-way remote-procedure call (RPC) request to device. " + RPC_REQUEST_DESCRIPTION + ONE_WAY_RPC_RESULT + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

    /**
     * 字段说明：
     * 1. 保存 `TWO_WAY_RPC_REQUEST_DESCRIPTION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String TWO_WAY_RPC_REQUEST_DESCRIPTION = "Sends the two-way remote-procedure call (RPC) request to device. " + RPC_REQUEST_DESCRIPTION + TWO_WAY_RPC_RESULT + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

    @ApiOperation(value = "Send one-way RPC request", notes = ONE_WAY_RPC_REQUEST_DESCRIPTION)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Persistent RPC request was saved to the database or lightweight RPC request was sent to the device."),
            @ApiResponse(code = 400, message = "Invalid structure of the request."),
            @ApiResponse(code = 401, message = "User is not authorized to send the RPC request. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(code = 504, message = "Timeout to process the RPC call. Most likely, device is offline."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/oneway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `handleOneWayDeviceRPCRequest` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<ResponseEntity> handleOneWayDeviceRPCRequest(
            @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
            @PathVariable("deviceId") String deviceIdStr,
            @ApiParam(value = "A JSON value representing the RPC request.")
            @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(true, new DeviceId(UUID.fromString(deviceIdStr)), requestBody, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ApiOperation(value = "Send two-way RPC request", notes = TWO_WAY_RPC_REQUEST_DESCRIPTION)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Persistent RPC request was saved to the database or lightweight RPC response received."),
            @ApiResponse(code = 400, message = "Invalid structure of the request."),
            @ApiResponse(code = 401, message = "User is not authorized to send the RPC request. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(code = 504, message = "Timeout to process the RPC call. Most likely, device is offline."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/twoway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `handleTwoWayDeviceRPCRequest` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<ResponseEntity> handleTwoWayDeviceRPCRequest(
            @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_ID) String deviceIdStr,
            @ApiParam(value = "A JSON value representing the RPC request.")
            @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(false, new DeviceId(UUID.fromString(deviceIdStr)), requestBody, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ApiOperation(value = "Get persistent RPC request", notes = "Get information about the status of the RPC call." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/persistent/{rpcId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getPersistedRpc` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Rpc getPersistedRpc(
            @ApiParam(value = RPC_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(RPC_ID) String strRpc) throws ThingsboardException {
        checkParameter("RpcId", strRpc);
        RpcId rpcId = new RpcId(UUID.fromString(strRpc));
        return checkRpcId(rpcId, Operation.READ);
    }

    @ApiOperation(value = "Get persistent RPC requests", notes = "Allows to query RPC calls for specific device using pagination." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/persistent/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getPersistedRpcByDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<ResponseEntity> getPersistedRpcByDevice(
            @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(DEVICE_ID) String strDeviceId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = "Status of the RPC", allowableValues = RPC_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) RpcStatus rpcStatus,
            @ApiParam(value = RPC_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RPC_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("DeviceId", strDeviceId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (rpcStatus != null && rpcStatus.equals(RpcStatus.DELETED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RpcStatus: DELETED");
        }

        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        DeviceId deviceId = new DeviceId(UUID.fromString(strDeviceId));
        final DeferredResult<ResponseEntity> response = new DeferredResult<>();

        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        accessValidator.validate(getCurrentUser(), Operation.RPC_CALL, deviceId, new HttpValidationCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                PageData<Rpc> rpcCalls;
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (rpcStatus != null) {
                    rpcCalls = rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
                } else {
                    rpcCalls = rpcService.findAllByDeviceId(tenantId, deviceId, pageLink);
                }
                response.setResult(new ResponseEntity<>(rpcCalls, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                ResponseEntity entity;
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (e instanceof ToErrorResponseEntity) {
                    entity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
                } else {
                    entity = new ResponseEntity(HttpStatus.UNAUTHORIZED);
                }
                response.setResult(entity);
            }
        }));
        return response;
    }

    @ApiOperation(value = "Delete persistent RPC", notes = "Deletes the persistent RPC request." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/persistent/{rpcId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteRpc` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteRpc(
            @ApiParam(value = RPC_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(RPC_ID) String strRpc) throws ThingsboardException {
        checkParameter("RpcId", strRpc);
        RpcId rpcId = new RpcId(UUID.fromString(strRpc));
        Rpc rpc = checkRpcId(rpcId, Operation.DELETE);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (rpc != null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (rpc.getStatus().isPushDeleteNotificationToCore()) {
                // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
                RemoveRpcActorMsg removeMsg = new RemoveRpcActorMsg(getTenantId(), rpc.getDeviceId(), rpc.getUuidId());
                log.trace("[{}] Forwarding msg {} to queue actor!", rpc.getDeviceId(), rpc);
                tbClusterService.pushMsgToCore(removeMsg, null);
            }

            rpcService.deleteRpc(getTenantId(), rpcId);
            rpc.setStatus(RpcStatus.DELETED);

            TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_DELETED, rpc.getDeviceId(), TbMsgMetaData.EMPTY, JacksonUtil.toString(rpc));
            tbClusterService.pushMsgToRuleEngine(getTenantId(), rpc.getDeviceId(), msg, null);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RpcV2Controller` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
