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
package org.thingsboard.script.api;

import lombok.Getter;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `TbScriptException` 是 ThingsBoard Common 中表示脚本执行失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `RuntimeException`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
public class TbScriptException extends RuntimeException {
    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -1958193538782818284L;

    /**
     * 中文说明：
     * 1. `ErrorCode` 是 ThingsBoard Common 中定义 `Error Code` 固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
     */
    public static enum ErrorCode {COMPILATION, TIMEOUT, RUNTIME, OTHER}

    /**
     * `scriptId`ID，用于定位对应业务对象。
     */
    @Getter
    private final UUID scriptId;
    /**
     * 错误码，记录当前处理过程中的失败原因。
     */
    @Getter
    private final ErrorCode errorCode;
    /**
     * `body` 字段，保存当前对象的对应属性。
     */
    @Getter
    private final String body;

    /**
     * 功能：创建 `TbScriptException` 实例，并初始化必要字段。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `errorCode`：错误信息。
     * - `body`：`body` 参数。
     * - `cause`：`cause` 参数。
     * 返回：新创建的对象实例。
     */
    public TbScriptException(UUID scriptId, ErrorCode errorCode, String body, Exception cause) {
        super(cause);
        this.scriptId = scriptId;
        this.errorCode = errorCode;
        this.body = body;
    }
}
