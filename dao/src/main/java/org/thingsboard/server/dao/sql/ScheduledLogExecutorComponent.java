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
package org.thingsboard.server.dao.sql;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `ScheduledLogExecutorComponent` 是 ThingsBoard DAO 中负责 `Scheduled Log Executor` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Component
public class ScheduledLogExecutorComponent {

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private ScheduledExecutorService schedulerLogExecutor;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        schedulerLogExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("sql-log"));
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void stop() {
        if (schedulerLogExecutor != null) {
            schedulerLogExecutor.shutdownNow();
        }
    }

    /**
     * 功能：执行 `scheduleAtFixedRate` 对应的处理。
     * 参数：
     * - `command`：`command` 参数。
     * - `initialDelay`：`initialDelay` 参数。
     * - `period`：`period` 参数。
     * - `unit`：`unit` 参数。
     * 返回：无。
     */
    public void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        schedulerLogExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
}
