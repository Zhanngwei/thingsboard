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

/**
 * 中文说明：
 * 1. `AbstractTbActor` 是 ThingsBoard Actor 中处理 `Tb Actor` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `TbActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
public abstract class AbstractTbActor implements TbActor {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Getter
    protected TbActorCtx ctx;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        this.ctx = ctx;
    }

    /**
     * 功能：获取Actor 实例。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbActorRef getActorRef() {
        return ctx;
    }
}
