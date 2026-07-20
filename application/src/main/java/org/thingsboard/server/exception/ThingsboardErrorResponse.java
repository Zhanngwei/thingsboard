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
 * 1. `ThingsboardErrorResponse` 是 ThingsBoard Application 中承载响应信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
