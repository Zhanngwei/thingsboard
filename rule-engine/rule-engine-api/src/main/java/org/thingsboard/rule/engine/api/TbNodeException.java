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
package org.thingsboard.rule.engine.api;

import lombok.Getter;
import org.thingsboard.server.common.msg.TbActorError;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. `TbNodeException` 是 ThingsBoard Rule Engine API 中表示规则节点失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `Exception`、`TbActorError`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
public class TbNodeException extends Exception implements TbActorError {

    /**
     * 是否满足`unrecoverable`条件。
     */
    @Getter
    private final boolean unrecoverable;

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(String message) {
        this(message, false);
    }

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `unrecoverable`：`unrecoverable` 参数。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(String message, boolean unrecoverable) {
        super(message);
        this.unrecoverable = unrecoverable;
    }

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(Exception e) {
        this(e, false);
    }

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `e`：`e` 参数。
     * - `unrecoverable`：`unrecoverable` 参数。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(Exception e, boolean unrecoverable) {
        super(e);
        this.unrecoverable = unrecoverable;
    }

}
