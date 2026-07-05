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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

import java.util.Date;

/**
 * 中文说明：
 * 1. 类目的：`ThingsboardErrorResponse` 是ThingsBoard Application 模块中的异常与错误响应类型，用于统一表达 ThingsBoard Application 的异常状态和 HTTP 错误响应。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、Spring 异常处理器、认证模块和客户端响应序列化。
 * 4. 生命周期：在请求失败、认证失败或参数校验失败时创建并返回。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Adapter。
 */
@ApiModel
public class ThingsboardErrorResponse {
    // HTTP Response Status Code
    /**
     * 状态，表示当前对象所处状态。
     */
    private final HttpStatus status;

    // General Error message
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final String message;

    // Error code
    /**
     * 错误码，记录当前处理过程中的失败原因。
     */
    private final ThingsboardErrorCode errorCode;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final long timestamp;

    /**
     * 功能：创建 `ThingsboardErrorResponse` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `errorCode`：错误信息。
     * - `status`：`status` 参数。
     * 返回：新创建的对象实例。
     */
    protected ThingsboardErrorResponse(final String message, final ThingsboardErrorCode errorCode, HttpStatus status) {
        this.message = message;
        this.errorCode = errorCode;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 功能：执行 `of` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * - `errorCode`：错误信息。
     * - `status`：`status` 参数。
     * 返回：处理结果。
     */
    public static ThingsboardErrorResponse of(final String message, final ThingsboardErrorCode errorCode, HttpStatus status) {
        return new ThingsboardErrorResponse(message, errorCode, status);
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 1, value = "HTTP Response Status Code", example = "401", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Integer getStatus() {
        return status.value();
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 2, value = "Error message", example = "Authentication failed", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getMessage() {
        return message;
    }

    /**
     * 功能：获取错误码。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "Platform error code:" +
            "\n* `2` - General error (HTTP: 500 - Internal Server Error)" +
            "\n\n* `10` - Authentication failed (HTTP: 401 - Unauthorized)" +
            "\n\n* `11` - JWT token expired (HTTP: 401 - Unauthorized)" +
            "\n\n* `15` - Credentials expired (HTTP: 401 - Unauthorized)" +
            "\n\n* `20` - Permission denied (HTTP: 403 - Forbidden)" +
            "\n\n* `30` - Invalid arguments (HTTP: 400 - Bad Request)" +
            "\n\n* `31` - Bad request params (HTTP: 400 - Bad Request)" +
            "\n\n* `32` - Item not found (HTTP: 404 - Not Found)" +
            "\n\n* `33` - Too many requests (HTTP: 429 - Too Many Requests)" +
            "\n\n* `34` - Too many updates (Too many updates over Websocket session)" +
            "\n\n* `40` - Subscription violation (HTTP: 403 - Forbidden)",
            example = "10", dataType = "integer",
            accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public ThingsboardErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 功能：获取当前对象记录的时间戳。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 4, value = "Timestamp", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public long getTimestamp() {
        return timestamp;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsboardErrorResponse` 在 ThingsBoard Application 模块 中承担异常与错误响应类型职责，核心目的是统一表达 ThingsBoard Application 的异常状态和 HTTP 错误响应。
 * 2. 核心流程：捕获异常后映射错误码、状态码和响应体。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、Spring 异常处理器、认证模块和客户端响应序列化。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
