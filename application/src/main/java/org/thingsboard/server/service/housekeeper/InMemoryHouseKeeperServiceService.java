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
package org.thingsboard.server.service.housekeeper;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.housekeeper.HouseKeeperService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `InMemoryHouseKeeperServiceService` 是 ThingsBoard Application 中负责 `In Memory House` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `HouseKeeperService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryHouseKeeperServiceService implements HouseKeeperService {

    /**
     * 告警，提供当前类调用的业务操作。
     */
    final TbAlarmService alarmService;

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    ListeningExecutorService executor;

    AtomicInteger queueSize = new AtomicInteger();
    AtomicInteger totalProcessedCounter = new AtomicInteger();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        log.debug("Starting HouseKeeper service");
        executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper")));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (executor != null) {
            log.debug("Stopping HouseKeeper service");
            executor.shutdown();
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        log.trace("[{}] DeleteEntityEvent handler: {}", event.getTenantId(), event);
        EntityId entityId = event.getEntityId();
        if (EntityType.USER.equals(entityId.getEntityType())) {
            unassignDeletedUserAlarms(event.getTenantId(), (User) event.getEntity(), event.getTs());
        }
    }

    /**
     * 功能：执行 `unassignDeletedUserAlarms` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * - `unassignTs`：时间戳。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<AlarmId>> unassignDeletedUserAlarms(TenantId tenantId, User user, long unassignTs) {
        log.debug("[{}][{}] unassignDeletedUserAlarms submitting, pending queue size: {} ", tenantId, user.getId().getId(), queueSize.get());
        queueSize.incrementAndGet();
        ListenableFuture<List<AlarmId>> future = executor.submit(() -> alarmService.unassignDeletedUserAlarms(tenantId, user, unassignTs));
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(List<AlarmId> alarmIds) {
                queueSize.decrementAndGet();
                totalProcessedCounter.incrementAndGet();
                log.debug("[{}][{}] unassignDeletedUserAlarms finished, pending queue size: {}, total processed count: {} ",
                        tenantId, user.getId().getId(), queueSize.get(), totalProcessedCounter.get());
            }

            @Override
            public void onFailure(Throwable throwable) {
                queueSize.decrementAndGet();
                totalProcessedCounter.incrementAndGet();
                log.error("[{}][{}] unassignDeletedUserAlarms failed, pending queue size: {}, total processed count: {}",
                        tenantId, user.getId().getId(), queueSize.get(), totalProcessedCounter.get(), throwable);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

}
