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
package org.thingsboard.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.service.security.exception.AuthMethodNotSupportedException;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.exception.UserPasswordExpiredException;
import org.thingsboard.server.service.security.exception.UserPasswordNotValidException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static javax.servlet.RequestDispatcher.ERROR_EXCEPTION;

@Slf4j
@Controller
@RestControllerAdvice
/**
 * 中文说明：
 * 1. 类目的：`ThingsboardErrorResponseHandler` 是ThingsBoard Application 模块中的异常与错误响应类型，用于统一表达 ThingsBoard Application 的异常状态和 HTTP 错误响应。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、Spring 异常处理器、认证模块和客户端响应序列化。
 * 4. 生命周期：在请求失败、认证失败或参数校验失败时创建并返回。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Adapter。
 */
public class ThingsboardErrorResponseHandler extends ResponseEntityExceptionHandler implements AccessDeniedHandler, ErrorController {

    private static final Map<HttpStatus, ThingsboardErrorCode> statusToErrorCodeMap = new HashMap<>();
    static {
        statusToErrorCodeMap.put(HttpStatus.BAD_REQUEST, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.UNAUTHORIZED, ThingsboardErrorCode.AUTHENTICATION);
        statusToErrorCodeMap.put(HttpStatus.FORBIDDEN, ThingsboardErrorCode.PERMISSION_DENIED);
        statusToErrorCodeMap.put(HttpStatus.NOT_FOUND, ThingsboardErrorCode.ITEM_NOT_FOUND);
        statusToErrorCodeMap.put(HttpStatus.METHOD_NOT_ALLOWED, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.NOT_ACCEPTABLE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.TOO_MANY_REQUESTS, ThingsboardErrorCode.TOO_MANY_REQUESTS);
        statusToErrorCodeMap.put(HttpStatus.INTERNAL_SERVER_ERROR, ThingsboardErrorCode.GENERAL);
        statusToErrorCodeMap.put(HttpStatus.SERVICE_UNAVAILABLE, ThingsboardErrorCode.GENERAL);
    }
    private static final Map<ThingsboardErrorCode, HttpStatus> errorCodeToStatusMap = new HashMap<>();
    static {
        errorCodeToStatusMap.put(ThingsboardErrorCode.GENERAL, HttpStatus.INTERNAL_SERVER_ERROR);
        errorCodeToStatusMap.put(ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED);
        errorCodeToStatusMap.put(ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
        errorCodeToStatusMap.put(ThingsboardErrorCode.CREDENTIALS_EXPIRED, HttpStatus.UNAUTHORIZED);
        errorCodeToStatusMap.put(ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN);
        errorCodeToStatusMap.put(ThingsboardErrorCode.INVALID_ARGUMENTS, HttpStatus.BAD_REQUEST);
        errorCodeToStatusMap.put(ThingsboardErrorCode.BAD_REQUEST_PARAMS, HttpStatus.BAD_REQUEST);
        errorCodeToStatusMap.put(ThingsboardErrorCode.ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
        errorCodeToStatusMap.put(ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS);
        errorCodeToStatusMap.put(ThingsboardErrorCode.TOO_MANY_UPDATES, HttpStatus.TOO_MANY_REQUESTS);
        errorCodeToStatusMap.put(ThingsboardErrorCode.SUBSCRIPTION_VIOLATION, HttpStatus.FORBIDDEN);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `statusToErrorCode` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static ThingsboardErrorCode statusToErrorCode(HttpStatus status) {
        return statusToErrorCodeMap.getOrDefault(status, ThingsboardErrorCode.GENERAL);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `errorCodeToStatus` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static HttpStatus errorCodeToStatus(ThingsboardErrorCode errorCode) {
        return errorCodeToStatusMap.getOrDefault(errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping("/error")
    /**
     * 方法说明：
     * 1. 职责：执行 `handleError` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ResponseEntity<Object> handleError(HttpServletRequest request) {
        HttpStatus httpStatus = Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
                .map(status -> HttpStatus.resolve(Integer.parseInt(status.toString())))
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        String errorMessage = Optional.ofNullable(request.getAttribute(ERROR_EXCEPTION))
                .map(e -> (ExceptionUtils.getMessage((Throwable) e)))
                .orElse(httpStatus.getReasonPhrase());
        return new ResponseEntity<>(ThingsboardErrorResponse.of(errorMessage, statusToErrorCode(httpStatus), httpStatus), httpStatus);
    }

    @Override
    @ExceptionHandler(AccessDeniedException.class)
    /**
     * 方法说明：
     * 1. 职责：执行 `handle` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException,
            ServletException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!response.isCommitted()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            JacksonUtil.writeValue(response.getWriter(),
                    ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                            ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));
        }
    }

    @ExceptionHandler(Exception.class)
    /**
     * 方法说明：
     * 1. 职责：执行 `handle` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handle(Exception exception, HttpServletResponse response) {
        log.debug("Processing exception {}", exception.getMessage(), exception);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!response.isCommitted()) {
            try {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (exception instanceof ThingsboardException) {
                    ThingsboardException thingsboardException = (ThingsboardException) exception;
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (thingsboardException.getErrorCode() == ThingsboardErrorCode.SUBSCRIPTION_VIOLATION) {
                        handleSubscriptionException((ThingsboardException) exception, response);
                    } else {
                        handleThingsboardException((ThingsboardException) exception, response);
                    }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (exception instanceof TbRateLimitsException) {
                    handleRateLimitException(response, (TbRateLimitsException) exception);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (exception instanceof AccessDeniedException) {
                    handleAccessDeniedException(response);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (exception instanceof AuthenticationException) {
                    handleAuthenticationException((AuthenticationException) exception, response);
                } else {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(exception.getMessage(),
                            ThingsboardErrorCode.GENERAL, HttpStatus.INTERNAL_SERVER_ERROR));
                }
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (IOException e) {
                log.error("Can't handle exception", e);
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleExceptionInternal` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatus status,
            WebRequest request) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        ThingsboardErrorCode errorCode = statusToErrorCode(status);
        return new ResponseEntity<>(ThingsboardErrorResponse.of(ex.getMessage(), errorCode, status), headers, status);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleThingsboardException` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleThingsboardException(ThingsboardException thingsboardException, HttpServletResponse response) throws IOException {
        ThingsboardErrorCode errorCode = thingsboardException.getErrorCode();
        HttpStatus status = errorCodeToStatus(errorCode);
        response.setStatus(status.value());
        JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(thingsboardException.getMessage(), errorCode, status));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleRateLimitException` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleRateLimitException(HttpServletResponse response, TbRateLimitsException exception) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        String message = "Too many requests for current " + exception.getEntityType().name().toLowerCase() + "!";
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of(message,
                        ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleSubscriptionException` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleSubscriptionException(ThingsboardException subscriptionException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        JacksonUtil.writeValue(response.getWriter(),
                JacksonUtil.fromBytes(((HttpClientErrorException) subscriptionException.getCause()).getResponseBodyAsByteArray(), Object.class));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleAccessDeniedException` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleAccessDeniedException(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));

    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleAuthenticationException` 对应的异常与错误响应类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：在请求失败、认证失败或参数校验失败时创建并返回时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：捕获异常后映射错误码、状态码和响应体。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleAuthenticationException(AuthenticationException authenticationException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (authenticationException instanceof BadCredentialsException || authenticationException instanceof UsernameNotFoundException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Invalid username or password", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (authenticationException instanceof DisabledException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("User account is not active", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (authenticationException instanceof LockedException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("User account is locked due to security policy", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof JwtExpiredTokenException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Token has expired", ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof AuthMethodNotSupportedException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(authenticationException.getMessage(), ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof UserPasswordExpiredException) {
            UserPasswordExpiredException expiredException = (UserPasswordExpiredException) authenticationException;
            String resetToken = expiredException.getResetToken();
            JacksonUtil.writeValue(response.getWriter(), ThingsboardCredentialsExpiredResponse.of(expiredException.getMessage(), resetToken));
        } else if (authenticationException instanceof UserPasswordNotValidException) {
            UserPasswordNotValidException expiredException = (UserPasswordNotValidException) authenticationException;
            JacksonUtil.writeValue(response.getWriter(), ThingsboardCredentialsViolationResponse.of(expiredException.getMessage()));
        } else {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsboardErrorResponseHandler` 在 ThingsBoard Application 模块 中承担异常与错误响应类型职责，核心目的是统一表达 ThingsBoard Application 的异常状态和 HTTP 错误响应。
 * 2. 核心流程：捕获异常后映射错误码、状态码和响应体。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、Spring 异常处理器、认证模块和客户端响应序列化。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
