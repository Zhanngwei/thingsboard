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
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `EventsCleanUpService` 是 ThingsBoard Application 中负责 `Events Clean Up` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCleanUpService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
public class EventsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.events.execution_interval_ms})}";

    /**
     * `ttlInSec` 字段，保存当前对象的对应属性。
     */
    @Value("${sql.ttl.events.events_ttl}")
    private long ttlInSec;

    /**
     * `debugTtlInSec` 字段，保存当前对象的对应属性。
     */
    @Value("${sql.ttl.events.debug_events_ttl}")
    private long debugTtlInSec;

    /**
     * 是否启用`ttl task execution`。
     */
    @Value("${sql.ttl.events.enabled}")
    private boolean ttlTaskExecutionEnabled;

    /**
     * 事件，提供当前类调用的业务操作。
     */
    private final EventService eventService;

    /**
     * 功能：创建 `EventsCleanUpService` 实例，并初始化必要字段。
     * 参数：
     * - `partitionService`：服务对象。
     * - `eventService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public EventsCleanUpService(PartitionService partitionService, EventService eventService) {
        super(partitionService);
        this.eventService = eventService;
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.events.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled) {
            long ts = System.currentTimeMillis();
            long regularEventExpTs = ttlInSec > 0 ? ts - TimeUnit.SECONDS.toMillis(ttlInSec) : 0;
            long debugEventExpTs = debugTtlInSec > 0 ? ts - TimeUnit.SECONDS.toMillis(debugTtlInSec) : 0;
            eventService.cleanupEvents(regularEventExpTs, debugEventExpTs, isSystemTenantPartitionMine());
        }
    }
}