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

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. `ActorTestCtx` 是 ThingsBoard Actor 中处理 `Actor Ctx` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 它直接协作于 Actor 上下文、消息类型和对应的消息处理器。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Data
@AllArgsConstructor
public class ActorTestCtx {

    /**
     * 等待器，用于在测试或异步流程中等待结果。
     */
    private volatile CountDownLatch latch;
    private final AtomicInteger invocationCount;
    /**
     * 期望调用次数，用于控制数量、位置或分页范围。
     */
    private final int expectedInvocationCount;
    private final AtomicLong actual;

    /**
     * 功能：执行 `clear` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void clear() {
        latch = new CountDownLatch(1);
        invocationCount.set(0);
        actual.set(0L);
    }
}
