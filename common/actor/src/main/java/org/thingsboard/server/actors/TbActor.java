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

import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

/**
 * 中文说明：
 * 1. `TbActor` 是 ThingsBoard Actor 中定义 `Tb Actor` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbActor {

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    boolean process(TbActorMsg msg);

    /**
     * 功能：获取Actor 实例。
     * 参数：无。
     * 返回：处理结果。
     */
    TbActorRef getActorRef();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    default void init(TbActorCtx ctx) throws TbActorException {
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：
     * - `stopReason`：`stopReason` 参数。
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    default void destroy(TbActorStopReason stopReason, Throwable cause) throws TbActorException {
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `attempt`：`attempt` 参数。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    default InitFailureStrategy onInitFailure(int attempt, Throwable t) {
        return InitFailureStrategy.retryWithDelay(5000L * attempt);
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `msg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    default ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable t) {
        if (t instanceof Error) {
            return ProcessFailureStrategy.stop();
        } else {
            return ProcessFailureStrategy.resume();
        }
    }
}
