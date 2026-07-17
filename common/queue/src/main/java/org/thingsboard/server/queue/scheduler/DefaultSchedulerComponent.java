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
package org.thingsboard.server.queue.scheduler;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultSchedulerComponent` 是 ThingsBoard Common Queue 中负责 `Scheduler Component` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `SchedulerComponent`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Component
public class DefaultSchedulerComponent implements SchedulerComponent {

    /**
     * 执行器，负责处理对应任务或消息。
     */
    protected ScheduledExecutorService schedulerExecutor;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("queue-scheduler"));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdownNow();
        }
    }

    /**
     * 功能：执行 `schedule` 对应的处理。
     * 参数：
     * - `command`：`command` 参数。
     * - `delay`：`delay` 参数。
     * - `unit`：`unit` 参数。
     * 返回：异步处理结果。
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedulerExecutor.schedule(command, delay, unit);
    }

    /**
     * 功能：执行 `schedule` 对应的处理。
     * 参数：
     * - `callable`：`callable` 参数。
     * - `delay`：`delay` 参数。
     * - `unit`：`unit` 参数。
     * 返回：异步处理结果。
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return schedulerExecutor.schedule(callable, delay, unit);
    }

    /**
     * 功能：执行 `scheduleAtFixedRate` 对应的处理。
     * 参数：
     * - `command`：`command` 参数。
     * - `initialDelay`：`initialDelay` 参数。
     * - `period`：`period` 参数。
     * - `unit`：`unit` 参数。
     * 返回：异步处理结果。
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return schedulerExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * 功能：执行 `scheduleWithFixedDelay` 对应的处理。
     * 参数：
     * - `command`：`command` 参数。
     * - `initialDelay`：`initialDelay` 参数。
     * - `delay`：`delay` 参数。
     * - `unit`：`unit` 参数。
     * 返回：异步处理结果。
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return schedulerExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
