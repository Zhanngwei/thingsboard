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
package org.thingsboard.server.common.data.exception;

/**
 * 中文说明：
 * 1. `ThingsboardException` 是 ThingsBoard Common Data 中表示 `Thingsboard` 失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `Exception`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
public class ThingsboardException extends Exception {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 错误码，记录当前处理过程中的失败原因。
     */
    private ThingsboardErrorCode errorCode;

    /**
     * 功能：创建 `ThingsboardException` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public ThingsboardException() {
        super();
    }

    /**
     * 功能：创建 `ThingsboardException` 实例，并初始化必要字段。
     * 参数：
     * - `errorCode`：错误信息。
     * 返回：新创建的对象实例。
     */
    public ThingsboardException(ThingsboardErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 功能：创建 `ThingsboardException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `errorCode`：错误信息。
     * 返回：新创建的对象实例。
     */
    public ThingsboardException(String message, ThingsboardErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 功能：创建 `ThingsboardException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `cause`：`cause` 参数。
     * - `errorCode`：错误信息。
     * 返回：新创建的对象实例。
     */
    public ThingsboardException(String message, Throwable cause, ThingsboardErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 功能：创建 `ThingsboardException` 实例，并初始化必要字段。
     * 参数：
     * - `cause`：`cause` 参数。
     * - `errorCode`：错误信息。
     * 返回：新创建的对象实例。
     */
    public ThingsboardException(Throwable cause, ThingsboardErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    /**
     * 功能：获取错误码。
     * 参数：无。
     * 返回：处理结果。
     */
    public ThingsboardErrorCode getErrorCode() {
        return errorCode;
    }

}
