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
package org.thingsboard.server.service.notification;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`DefaultNotificationSchedulerService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationSchedulerService extends AbstractPartitionBasedService<NotificationRequestId> implements NotificationSchedulerService {

    /**
     * 通知，表示当前对象的对应属性。
     */
    private final NotificationCenter notificationCenter;
    private final NotificationRequestService notificationRequestService;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final NotificationExecutorService notificationExecutor;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("notification-scheduler"));

    private final Map<NotificationRequestId, ScheduledRequestMetadata> scheduledNotificationRequests = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    @PostConstruct
    public void init() {
        super.init();
    }

    /**
     * 功能：处理`on Added Partitions`。
     * 参数：
     * - `addedPartitions`：分区标识或分区信息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        PageDataIterable<NotificationRequest> notificationRequests = new PageDataIterable<>(pageLink -> {
            return notificationRequestService.findScheduledNotificationRequests(pageLink);
        }, 1000);
        for (NotificationRequest notificationRequest : notificationRequests) {
            TopicPartitionInfo requestPartition = partitionService.resolve(ServiceType.TB_CORE, notificationRequest.getTenantId(), notificationRequest.getId());
            if (addedPartitions.contains(requestPartition)) {
                partitionedEntities.computeIfAbsent(requestPartition, k -> ConcurrentHashMap.newKeySet()).add(notificationRequest.getId());
                if (!scheduledNotificationRequests.containsKey(notificationRequest.getId())) {
                    scheduleNotificationRequest(notificationRequest.getTenantId(), notificationRequest, notificationRequest.getCreatedTime());
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * 功能：执行 `scheduleNotificationRequest` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationRequestId`：请求ID。
     * - `requestTs`：时间戳。
     * 返回：无。
     */
    @Override
    public void scheduleNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId, long requestTs) {
        NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, notificationRequestId);
        scheduleNotificationRequest(tenantId, notificationRequest, requestTs);
    }

    /**
     * 功能：执行 `scheduleNotificationRequest` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `request`：请求对象。
     * - `requestTs`：时间戳。
     * 返回：无。
     */
    private void scheduleNotificationRequest(TenantId tenantId, NotificationRequest request, long requestTs) {
        int delayInSec = Optional.ofNullable(request)
                .map(NotificationRequest::getAdditionalConfig)
                .map(NotificationRequestConfig::getSendingDelayInSec)
                .orElse(0);
        if (delayInSec <= 0) return;
        long delayInMs = TimeUnit.SECONDS.toMillis(delayInSec) - (System.currentTimeMillis() - requestTs);
        if (delayInMs < 0) {
            delayInMs = 0;
        }

        ScheduledFuture<?> scheduledTask = scheduler.schedule(() -> {
            NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, request.getId());
            if (notificationRequest == null) return;

            notificationExecutor.executeAsync(() -> {
                try {
                    notificationCenter.processNotificationRequest(tenantId, notificationRequest, null);
                } catch (Exception e) {
                    log.error("Failed to process scheduled notification request {}", notificationRequest.getId(), e);
                    NotificationRequestStats stats = new NotificationRequestStats();
                    stats.setError(e.getMessage());
                    notificationRequestService.updateNotificationRequest(tenantId, request.getId(), NotificationRequestStatus.SENT, stats);
                }
            });
            scheduledNotificationRequests.remove(notificationRequest.getId());
        }, delayInMs, TimeUnit.MILLISECONDS);
        scheduledNotificationRequests.put(request.getId(), new ScheduledRequestMetadata(tenantId, scheduledTask));
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener(ComponentLifecycleMsg.class)
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
            EntityId entityId = event.getEntityId();
            switch (entityId.getEntityType()) {
                case NOTIFICATION_REQUEST:
                    cancelAndRemove((NotificationRequestId) entityId);
                    break;
                case TENANT:
                    Set<NotificationRequestId> toCancel = new HashSet<>();
                    scheduledNotificationRequests.forEach((notificationRequestId, scheduledRequestMetadata) -> {
                        if (scheduledRequestMetadata.getTenantId().equals(entityId)) {
                            toCancel.add(notificationRequestId);
                        }
                    });
                    toCancel.forEach(this::cancelAndRemove);
                    break;
            }
        }
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `notificationRequestId`：请求ID。
     * 返回：无。
     */
    @Override
    protected void cleanupEntityOnPartitionRemoval(NotificationRequestId notificationRequestId) {
        cancelAndRemove(notificationRequestId);
    }

    /**
     * 功能：执行 `cancelAndRemove` 对应的处理。
     * 参数：
     * - `notificationRequestId`：请求ID。
     * 返回：无。
     */
    private void cancelAndRemove(NotificationRequestId notificationRequestId) {
        ScheduledRequestMetadata md = scheduledNotificationRequests.remove(notificationRequestId);
        if (md != null) {
            md.getFuture().cancel(false);
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getServiceName() {
        return "Notifications scheduler";
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getSchedulerExecutorName() {
        return "notifications-scheduler";
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    @PreDestroy
    public void stop() {
        super.stop();
        scheduler.shutdownNow();
    }

    /**
     * 中文说明：
     * 1. 类目的：`ScheduledRequestMetadata` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    @Data
    private static class ScheduledRequestMetadata {
        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final ScheduledFuture<?> future;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNotificationSchedulerService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
