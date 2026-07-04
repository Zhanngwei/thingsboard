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
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.exception.ToErrorResponseEntity;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rpc.LocalRequestMetaData;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by ashvayka on 22.03.18.
 */
@TbCoreComponent
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractRpcController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public abstract class AbstractRpcController extends BaseController {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceRpcService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected TbCoreDeviceRpcService deviceRpcService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `accessValidator` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected AccessValidator accessValidator;

    @Value("${server.rest.server_side_rpc.min_timeout:5000}")
    /**
     * 字段说明：
     * 1. 保存 `minTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected long minTimeout;

    @Value("${server.rest.server_side_rpc.default_timeout:10000}")
    /**
     * 字段说明：
     * 1. 保存 `defaultTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected long defaultTimeout;

    /**
     * 方法说明：
     * 1. 职责：执行 `handleDeviceRPCRequest` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected DeferredResult<ResponseEntity> handleDeviceRPCRequest(boolean oneWay, DeviceId deviceId, String requestBody, HttpStatus timeoutStatus, HttpStatus noActiveConnectionStatus) throws ThingsboardException {
        try {
            JsonNode rpcRequestBody = JacksonUtil.toJsonNode(requestBody);
            ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(rpcRequestBody.get("method").asText(), JacksonUtil.toString(rpcRequestBody.get("params")));
            SecurityUser currentUser = getCurrentUser();
            TenantId tenantId = currentUser.getTenantId();
            final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            long timeout = rpcRequestBody.has(DataConstants.TIMEOUT) ? rpcRequestBody.get(DataConstants.TIMEOUT).asLong() : defaultTimeout;
            long expTime = rpcRequestBody.has(DataConstants.EXPIRATION_TIME) ? rpcRequestBody.get(DataConstants.EXPIRATION_TIME).asLong() : System.currentTimeMillis() + Math.max(minTimeout, timeout);
            UUID rpcRequestUUID = rpcRequestBody.has("requestUUID") ? UUID.fromString(rpcRequestBody.get("requestUUID").asText()) : UUID.randomUUID();
            boolean persisted = rpcRequestBody.has(DataConstants.PERSISTENT) && rpcRequestBody.get(DataConstants.PERSISTENT).asBoolean();
            String additionalInfo =  JacksonUtil.toString(rpcRequestBody.get(DataConstants.ADDITIONAL_INFO));
            Integer retries = rpcRequestBody.has(DataConstants.RETRIES) ? rpcRequestBody.get(DataConstants.RETRIES).asInt() : null;
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            accessValidator.validate(currentUser, Operation.RPC_CALL, deviceId, new HttpValidationCallback(response, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                    ToDeviceRpcRequest rpcRequest = new ToDeviceRpcRequest(rpcRequestUUID,
                            tenantId,
                            deviceId,
                            oneWay,
                            expTime,
                            body,
                            persisted,
                            retries,
                            additionalInfo
                    );
                    deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> reply(new LocalRequestMetaData(rpcRequest, currentUser, result), fromDeviceRpcResponse, timeoutStatus, noActiveConnectionStatus), currentUser);
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
                    logRpcCall(currentUser, deviceId, body, oneWay, Optional.empty(), e);
                    response.setResult(entity);
                }
            }));
            return response;
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (IllegalArgumentException ioe) {
            throw new ThingsboardException("Invalid request body", ioe, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
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
    public void reply(LocalRequestMetaData rpcRequest, FromDeviceRpcResponse response, HttpStatus timeoutStatus, HttpStatus noActiveConnectionStatus) {
        Optional<RpcError> rpcError = response.getError();
        DeferredResult<ResponseEntity> responseWriter = rpcRequest.getResponseWriter();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (rpcError.isPresent()) {
            logRpcCall(rpcRequest, rpcError, null);
            RpcError error = rpcError.get();
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (error) {
                case TIMEOUT:
                    responseWriter.setResult(new ResponseEntity<>(timeoutStatus));
                    break;
                case NO_ACTIVE_CONNECTION:
                    responseWriter.setResult(new ResponseEntity<>(noActiveConnectionStatus));
                    break;
                default:
                    responseWriter.setResult(new ResponseEntity<>(timeoutStatus));
                    break;
            }
        } else {
            Optional<String> responseData = response.getResponse();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (responseData.isPresent() && !StringUtils.isEmpty(responseData.get())) {
                String data = responseData.get();
                try {
                    logRpcCall(rpcRequest, rpcError, null);
                    responseWriter.setResult(new ResponseEntity<>(JacksonUtil.toJsonNode(data), HttpStatus.OK));
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                } catch (IllegalArgumentException e) {
                    log.debug("Failed to decode device response: {}", data, e);
                    logRpcCall(rpcRequest, rpcError, e);
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                }
            } else {
                logRpcCall(rpcRequest, rpcError, null);
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logRpcCall` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logRpcCall(LocalRequestMetaData rpcRequest, Optional<RpcError> rpcError, Throwable e) {
        logRpcCall(rpcRequest.getUser(), rpcRequest.getRequest().getDeviceId(), rpcRequest.getRequest().getBody(), rpcRequest.getRequest().isOneway(), rpcError, null);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `logRpcCall` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logRpcCall(SecurityUser user, EntityId entityId, ToDeviceRpcRequestBody body, boolean oneWay, Optional<RpcError> rpcError, Throwable e) {
        String rpcErrorStr = "";
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (rpcError.isPresent()) {
            rpcErrorStr = "RPC Error: " + rpcError.get().name();
        }
        String method = body.getMethod();
        String params = body.getParams();

        auditLogService.logEntityAction(
                user.getTenantId(),
                user.getCustomerId(),
                user.getId(),
                user.getName(),
                (UUIDBased & EntityId) entityId,
                null,
                ActionType.RPC_CALL,
                BaseController.toException(e),
                rpcErrorStr,
                oneWay,
                method,
                params);
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractRpcController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
