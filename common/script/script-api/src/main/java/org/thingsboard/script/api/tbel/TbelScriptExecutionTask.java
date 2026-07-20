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
package org.thingsboard.script.api.tbel;

import com.google.common.util.concurrent.ListenableFuture;
import org.mvel2.ExecutionContext;
import org.thingsboard.script.api.TbScriptExecutionTask;


/**
 * 中文说明：
 * 1. `TbelScriptExecutionTask` 是 ThingsBoard Common 中围绕脚本执行提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbScriptExecutionTask`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class TbelScriptExecutionTask extends TbScriptExecutionTask {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final ExecutionContext context;

    /**
     * 功能：创建 `TbelScriptExecutionTask` 实例，并初始化必要字段。
     * 参数：
     * - `context`：处理上下文。
     * - `resultFuture`：数据列表。
     * 返回：新创建的对象实例。
     */
    public TbelScriptExecutionTask(ExecutionContext context, ListenableFuture<Object> resultFuture) {
        super(resultFuture);
        this.context = context;
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop(){
        context.stop();
    }
}
