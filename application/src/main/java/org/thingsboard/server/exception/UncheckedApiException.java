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

import org.springframework.http.ResponseEntity;

import java.util.Objects;

/**
 * 中文说明：
 * 1. `UncheckedApiException` 是 ThingsBoard Application 中表示 `Unchecked Api` 失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `RuntimeException`、`ToErrorResponseEntity`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
public class UncheckedApiException extends RuntimeException implements ToErrorResponseEntity {

    /**
     * `cause` 字段，保存当前对象的对应属性。
     */
    private final ToErrorResponseEntity cause;

    public <T extends Exception & ToErrorResponseEntity> UncheckedApiException(T cause) {
        super(cause.getMessage(), Objects.requireNonNull(cause));
        this.cause = cause;
    }

    /**
     * 功能：执行 `toErrorResponseEntity` 对应的处理。
     * 参数：无。
     * 返回：响应结果。
     */
    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return cause.toErrorResponseEntity();
    }
}
