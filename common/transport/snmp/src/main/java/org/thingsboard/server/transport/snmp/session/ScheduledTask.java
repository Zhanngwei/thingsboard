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
package org.thingsboard.server.transport.snmp.session;

import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `ScheduledTask` 是 ThingsBoard Common Transport 中负责 `Scheduled Task` 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
@Slf4j
public class ScheduledTask {
    /**
     * 异步结果列表，用于保存一组待处理对象。
     */
    private ListenableFuture<?> scheduledFuture;
    private boolean stopped = false;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * - `delayMs`：`delayMs` 参数。
     * - `scheduler`：`scheduler` 参数。
     * 返回：无。
     */
    public void init(AsyncCallable<Void> task, long delayMs, ScheduledExecutorService scheduler) {
        schedule(task, delayMs, scheduler);
    }

    /**
     * 功能：执行 `schedule` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * - `delayMs`：`delayMs` 参数。
     * - `scheduler`：`scheduler` 参数。
     * 返回：无。
     */
    private void schedule(AsyncCallable<Void> task, long delayMs, ScheduledExecutorService scheduler) {
        scheduledFuture = Futures.scheduleAsync(() -> {
            if (stopped) {
                return Futures.immediateCancelledFuture();
            }
            try {
                return task.call();
            } catch (Throwable t) {
                log.error("Unhandled error in scheduled task", t);
                return Futures.immediateFailedFuture(t);
            }
        }, delayMs, TimeUnit.MILLISECONDS, scheduler);
        if (!stopped) {
            scheduledFuture.addListener(() -> schedule(task, delayMs, scheduler), MoreExecutors.directExecutor());
        }
    }

    /**
     * 功能：执行 `cancel` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void cancel() {
        stopped = true;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

}
