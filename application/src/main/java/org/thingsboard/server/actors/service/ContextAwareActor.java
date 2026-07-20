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
package org.thingsboard.server.actors.service;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.AbstractTbActor;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ProcessFailureStrategy;
import org.thingsboard.server.common.msg.TbActorMsg;

/**
 * 中文说明：
 * 1. `ContextAwareActor` 是 ThingsBoard Application 中处理 `Context Aware Actor` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `AbstractTbActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Slf4j
public abstract class ContextAwareActor extends AbstractTbActor {

    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final int ENTITY_PACK_LIMIT = 1024;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    protected final ActorSystemContext systemContext;

    /**
     * 功能：创建 `ContextAwareActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public ContextAwareActor(ActorSystemContext systemContext) {
        super();
        this.systemContext = systemContext;
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    public boolean process(TbActorMsg msg) {
        if (log.isDebugEnabled()) {
            log.debug("Processing msg: {}", msg);
        }
        if (!doProcess(msg)) {
            log.warn("Unprocessed message: {}!", msg);
        }
        return false;
    }

    /**
     * 功能：执行 `doProcess` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    protected abstract boolean doProcess(TbActorMsg msg);

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `msg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    @Override
    public ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable t) {
        log.debug("[{}] Processing failure for msg {}", getActorRef().getActorId(), msg, t);
        return doProcessFailure(t);
    }

    /**
     * 功能：执行 `doProcessFailure` 对应的处理。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    protected ProcessFailureStrategy doProcessFailure(Throwable t) {
        if (t instanceof Error) {
            return ProcessFailureStrategy.stop();
        } else {
            return ProcessFailureStrategy.resume();
        }
    }
}
