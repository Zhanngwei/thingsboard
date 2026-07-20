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

import lombok.Getter;
import lombok.ToString;

/**
 * 中文说明：
 * 1. `ProcessFailureStrategy` 是 ThingsBoard Actor 中处理 `Process Failure Strategy` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@ToString
public class ProcessFailureStrategy {

    /**
     * 是否满足`stop`条件。
     */
    @Getter
    private boolean stop;

    /**
     * 功能：创建 `ProcessFailureStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `stop`：`stop` 参数。
     * 返回：新创建的对象实例。
     */
    private ProcessFailureStrategy(boolean stop) {
        this.stop = stop;
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static ProcessFailureStrategy stop() {
        return new ProcessFailureStrategy(true);
    }

    /**
     * 功能：执行 `resume` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static ProcessFailureStrategy resume() {
        return new ProcessFailureStrategy(false);
    }
}
