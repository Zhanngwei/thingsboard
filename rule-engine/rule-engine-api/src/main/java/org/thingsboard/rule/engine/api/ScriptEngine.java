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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. `ScriptEngine` 是 ThingsBoard Rule Engine API 中定义脚本执行能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface ScriptEngine {

    /**
     * 功能：执行`Update Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg);

    /**
     * 功能：执行`Generate Async`。
     * 参数：
     * - `prevMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg);

    /**
     * 功能：执行`Filter Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> executeFilterAsync(TbMsg msg);

    /**
     * 功能：执行`Switch Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg);

    /**
     * 功能：执行JSON。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg);

    /**
     * 功能：执行`To String Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<String> executeToStringAsync(TbMsg msg);

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void destroy();

}
