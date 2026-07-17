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
package org.thingsboard.server.actors.shared;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 中文说明：
 * 1. `AbstractContextAwareMsgProcessor` 是 ThingsBoard Application 中处理消息的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class AbstractContextAwareMsgProcessor {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    protected final ActorSystemContext systemContext;

    /**
     * 功能：创建 `AbstractContextAwareMsgProcessor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * 返回：新创建的对象实例。
     */
    protected AbstractContextAwareMsgProcessor(ActorSystemContext systemContext) {
        super();
        this.systemContext = systemContext;
    }

    /**
     * 功能：获取调度器。
     * 参数：无。
     * 返回：处理结果。
     */
    private ScheduledExecutorService getScheduler() {
        return systemContext.getScheduler();
    }

    /**
     * 功能：执行 `schedulePeriodicMsgWithDelay` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `delayInMs`：`delayInMs` 参数。
     * - `periodInMs`：`periodInMs` 参数。
     * 返回：无。
     */
    protected void schedulePeriodicMsgWithDelay(TbActorCtx ctx, TbActorMsg msg, long delayInMs, long periodInMs) {
        systemContext.schedulePeriodicMsgWithDelay(ctx, msg, delayInMs, periodInMs);
    }

    /**
     * 功能：执行 `scheduleMsgWithDelay` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `delayInMs`：`delayInMs` 参数。
     * 返回：无。
     */
    protected void scheduleMsgWithDelay(TbActorCtx ctx, TbActorMsg msg, long delayInMs) {
        systemContext.scheduleMsgWithDelay(ctx, msg, delayInMs);
    }

}
