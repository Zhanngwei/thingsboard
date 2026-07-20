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

/**
 * 中文说明：
 * 1. `ThingsboardErrorResponseHandler` 是 ThingsBoard Application 中处理响应的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `ResponseEntityExceptionHandler`、`AccessDeniedHandler`、`ErrorController`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
@Controller
@RestControllerAdvice
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
     * 功能：执行 `statusToErrorCode` 对应的处理。
     * 参数：
     * - `status`：`status` 参数。
     * 返回：处理结果。
     */
    private static ThingsboardErrorCode statusToErrorCode(HttpStatus status) {
        return statusToErrorCodeMap.getOrDefault(status, ThingsboardErrorCode.GENERAL);
    }

    /**
     * 功能：执行 `errorCodeToStatus` 对应的处理。
     * 参数：
     * - `errorCode`：错误信息。
     * 返回：处理结果。
     */
    private static HttpStatus errorCodeToStatus(ThingsboardErrorCode errorCode) {
        return errorCodeToStatusMap.getOrDefault(errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `request`：请求对象。
     * 返回：响应结果。
     */
    @RequestMapping("/error")
    public ResponseEntity<Object> handleError(HttpServletRequest request) {
        HttpStatus httpStatus = Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
                .map(status -> HttpStatus.resolve(Integer.parseInt(status.toString())))
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        String errorMessage = Optional.ofNullable(request.getAttribute(ERROR_EXCEPTION))
                .map(e -> (ExceptionUtils.getMessage((Throwable) e)))
                .orElse(httpStatus.getReasonPhrase());
        return new ResponseEntity<>(ThingsboardErrorResponse.of(errorMessage, statusToErrorCode(httpStatus), httpStatus), httpStatus);
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * - `accessDeniedException`：`accessDeniedException` 参数。
     * 返回：无。
     */
    @Override
    @ExceptionHandler(AccessDeniedException.class)
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException,
            ServletException {
        if (!response.isCommitted()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            JacksonUtil.writeValue(response.getWriter(),
                    ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                            ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));
        }
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `exception`：`exception` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    @ExceptionHandler(Exception.class)
    public void handle(Exception exception, HttpServletResponse response) {
        log.debug("Processing exception {}", exception.getMessage(), exception);
        if (!response.isCommitted()) {
            try {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                if (exception instanceof ThingsboardException) {
                    ThingsboardException thingsboardException = (ThingsboardException) exception;
                    if (thingsboardException.getErrorCode() == ThingsboardErrorCode.SUBSCRIPTION_VIOLATION) {
                        handleSubscriptionException((ThingsboardException) exception, response);
                    } else {
                        handleThingsboardException((ThingsboardException) exception, response);
                    }
                } else if (exception instanceof TbRateLimitsException) {
                    handleRateLimitException(response, (TbRateLimitsException) exception);
                } else if (exception instanceof AccessDeniedException) {
                    handleAccessDeniedException(response);
                } else if (exception instanceof AuthenticationException) {
                    handleAuthenticationException((AuthenticationException) exception, response);
                } else {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(exception.getMessage(),
                            ThingsboardErrorCode.GENERAL, HttpStatus.INTERNAL_SERVER_ERROR));
                }
            } catch (IOException e) {
                log.error("Can't handle exception", e);
            }
        }
    }

    /**
     * 功能：处理`Exception Internal`。
     * 参数：
     * - `ex`：`ex` 参数。
     * - `body`：`body` 参数。
     * - `headers`：`headers` 参数。
     * - `status`：`status` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatus status,
            WebRequest request) {
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        ThingsboardErrorCode errorCode = statusToErrorCode(status);
        return new ResponseEntity<>(ThingsboardErrorResponse.of(ex.getMessage(), errorCode, status), headers, status);
    }

    /**
     * 功能：处理`Thingsboard Exception`。
     * 参数：
     * - `thingsboardException`：`thingsboardException` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    private void handleThingsboardException(ThingsboardException thingsboardException, HttpServletResponse response) throws IOException {
        ThingsboardErrorCode errorCode = thingsboardException.getErrorCode();
        HttpStatus status = errorCodeToStatus(errorCode);
        response.setStatus(status.value());
        JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(thingsboardException.getMessage(), errorCode, status));
    }

    /**
     * 功能：处理数量限制。
     * 参数：
     * - `response`：响应对象。
     * - `exception`：`exception` 参数。
     * 返回：无。
     */
    private void handleRateLimitException(HttpServletResponse response, TbRateLimitsException exception) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        String message = "Too many requests for current " + exception.getEntityType().name().toLowerCase() + "!";
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of(message,
                        ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS));
    }

    /**
     * 功能：处理订阅。
     * 参数：
     * - `subscriptionException`：`subscriptionException` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    private void handleSubscriptionException(ThingsboardException subscriptionException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        JacksonUtil.writeValue(response.getWriter(),
                JacksonUtil.fromBytes(((HttpClientErrorException) subscriptionException.getCause()).getResponseBodyAsByteArray(), Object.class));
    }

    /**
     * 功能：处理`Access Denied Exception`。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    private void handleAccessDeniedException(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));

    }

    /**
     * 功能：处理`Authentication Exception`。
     * 参数：
     * - `authenticationException`：`authenticationException` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    private void handleAuthenticationException(AuthenticationException authenticationException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        if (authenticationException instanceof BadCredentialsException || authenticationException instanceof UsernameNotFoundException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Invalid username or password", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof DisabledException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("User account is not active", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
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
