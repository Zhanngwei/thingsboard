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
package org.thingsboard.server.service.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 中文说明：
 * 1. `ValidationResult` 是 ThingsBoard Application 中承载 `Validation Result` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@AllArgsConstructor
public class ValidationResult<V> {

    /**
     * 编码，表示当前对象的对应属性。
     */
    private final ValidationResultCode resultCode;
    private final String message;
    /**
     * `v` 字段，保存当前对象的对应属性。
     */
    private final V v;

    /**
     * 功能：执行 `ok` 对应的处理。
     * 参数：
     * - `v`：`v` 参数。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> ok(V v) {
        return new ValidationResult<>(ValidationResultCode.OK, "Ok", v);
    }

    /**
     * 功能：执行 `accessDenied` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> accessDenied(String message) {
        return new ValidationResult<>(ValidationResultCode.ACCESS_DENIED, message, null);
    }

    /**
     * 功能：执行 `entityNotFound` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> entityNotFound(String message) {
        return new ValidationResult<>(ValidationResultCode.ENTITY_NOT_FOUND, message, null);
    }

    /**
     * 功能：执行 `unauthorized` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> unauthorized(String message) {
        return new ValidationResult<>(ValidationResultCode.UNAUTHORIZED, message, null);
    }

    /**
     * 功能：执行 `internalError` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public static <V> ValidationResult<V> internalError(String message) {
        return new ValidationResult<>(ValidationResultCode.INTERNAL_ERROR, message, null);
    }

}
