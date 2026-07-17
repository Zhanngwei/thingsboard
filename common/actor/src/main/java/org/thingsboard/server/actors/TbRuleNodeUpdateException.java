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
package org.thingsboard.server.actors;

/**
 * 中文说明：
 * 1. `TbRuleNodeUpdateException` 是 ThingsBoard Actor 中表示规则节点失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `RuntimeException`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
public class TbRuleNodeUpdateException extends RuntimeException {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8209771144711980882L;

    /**
     * 功能：创建 `TbRuleNodeUpdateException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `cause`：`cause` 参数。
     * 返回：新创建的对象实例。
     */
    public TbRuleNodeUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
