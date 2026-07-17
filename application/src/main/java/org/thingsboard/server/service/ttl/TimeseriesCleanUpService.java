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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `TimeseriesCleanUpService` 是 ThingsBoard Application 中负责时序数据的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCleanUpService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@TbCoreComponent
@Slf4j
@Service
public class TimeseriesCleanUpService extends AbstractCleanUpService {

    /**
     * Actor 系统，表示当前对象的对应属性。
     */
    @Value("${sql.ttl.ts.ts_key_value_ttl}")
    protected long systemTtl;

    /**
     * 是否启用`ttl task execution`。
     */
    @Value("${sql.ttl.ts.enabled}")
    private boolean ttlTaskExecutionEnabled;

    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    private final TimeseriesService timeseriesService;

    /**
     * 功能：创建 `TimeseriesCleanUpService` 实例，并初始化必要字段。
     * 参数：
     * - `partitionService`：服务对象。
     * - `timeseriesService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public TimeseriesCleanUpService(PartitionService partitionService, TimeseriesService timeseriesService) {
        super(partitionService);
        this.timeseriesService = timeseriesService;
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(initialDelayString = "${sql.ttl.ts.execution_interval_ms}", fixedDelayString = "${sql.ttl.ts.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            timeseriesService.cleanup(systemTtl);
        }
    }
}