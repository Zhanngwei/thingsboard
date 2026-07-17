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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TbCallback;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `TbPackCallback` 是 ThingsBoard Application 中处理 `Tb Pack Callback` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `TbCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class TbPackCallback<T> implements TbCallback {
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final TbPackProcessingContext<T> ctx;
    private final UUID id;

    /**
     * 功能：创建 `TbPackCallback` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `ctx`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public TbPackCallback(UUID id, TbPackProcessingContext<T> ctx) {
        this.id = id;
        this.ctx = ctx;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onSuccess() {
        log.trace("[{}] ON SUCCESS", id);
        ctx.onSuccess(id);
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(Throwable t) {
        log.trace("[{}] ON FAILURE", id, t);
        ctx.onFailure(id, t);
    }
}
