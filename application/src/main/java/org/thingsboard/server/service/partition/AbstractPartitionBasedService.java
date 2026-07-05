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
package org.thingsboard.server.service.partition;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. 类目的：`AbstractPartitionBasedService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public abstract class AbstractPartitionBasedService<T extends EntityId> extends TbApplicationEventListener<PartitionChangeEvent> {

    protected final ConcurrentMap<TopicPartitionInfo, Set<T>> partitionedEntities = new ConcurrentHashMap<>();
    protected final ConcurrentMap<TopicPartitionInfo, List<ListenableFuture<?>>> partitionedFetchTasks = new ConcurrentHashMap<>();
    final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Autowired
    protected PartitionService partitionService;
    protected ListeningScheduledExecutorService scheduledExecutor;

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    abstract protected String getServiceName();

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    abstract protected String getSchedulerExecutorName();

    /**
     * 功能：处理`on Added Partitions`。
     * 参数：
     * - `addedPartitions`：分区标识或分区信息。
     * 返回：匹配的数据集合。
     */
    abstract protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions);

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    abstract protected void cleanupEntityOnPartitionRemoval(T entityId);

    /**
     * 功能：获取`Partitioned Entities`。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * 返回：匹配的数据集合。
     */
    public Set<T> getPartitionedEntities(TopicPartitionInfo tpi) {
        return partitionedEntities.get(tpi);
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void init() {
        // Should be always single threaded due to absence of locks.
        scheduledExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(getSchedulerExecutorName())));
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void stop() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    /**
     * DiscoveryService will call this event from the single thread (one-by-one).
     * Events order is guaranteed by DiscoveryService.
     * The only concurrency is expected from the [main] thread on Application started.
     * Async implementation. Locks is not allowed by design.
     * Any locks or delays in this module will affect DiscoveryService and entire system
     */
    /**
     * 功能：处理事件。
     * 参数：
     * - `partitionChangeEvent`：分区标识或分区信息。
     * 返回：无。
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        log.debug("onTbApplicationEvent, processing event: {}", partitionChangeEvent);
        subscribeQueue.add(partitionChangeEvent.getPartitions());
        scheduledExecutor.submit(this::pollInitStateFromDB);
    }

    /**
     * 功能：执行 `filterTbApplicationEvent` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：判断结果。
     */
    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return getServiceType().equals(event.getServiceType());
    }

    /**
     * 功能：执行 `pollInitStateFromDB` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void pollInitStateFromDB() {
        final Set<TopicPartitionInfo> partitions = getLatestPartitions();
        if (partitions == null) {
            log.debug("Nothing to do. Partitions are empty.");
            return;
        }
        initStateFromDB(partitions);
    }

    /**
     * 功能：初始化或启动状态。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：无。
     */
    private void initStateFromDB(Set<TopicPartitionInfo> partitions) {
        try {
            log.info("[{}] CURRENT PARTITIONS: {}", getServiceName(), partitionedEntities.keySet());
            log.info("[{}] NEW PARTITIONS: {}", getServiceName(), partitions);

            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(partitionedEntities.keySet());

            log.info("[{}] ADDED PARTITIONS: {}", getServiceName(), addedPartitions);

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(partitionedEntities.keySet());
            removedPartitions.removeAll(partitions);

            log.info("[{}] REMOVED PARTITIONS: {}", getServiceName(), removedPartitions);

            boolean partitionListChanged = false;
            // We no longer manage current partition of entities;
            for (var partition : removedPartitions) {
                Set<T> entities = partitionedEntities.remove(partition);
                if (entities != null) {
                    entities.forEach(this::cleanupEntityOnPartitionRemoval);
                }
                List<ListenableFuture<?>> fetchTasks = partitionedFetchTasks.remove(partition);
                if (fetchTasks != null) {
                    fetchTasks.forEach(f -> f.cancel(false));
                }
                partitionListChanged = true;
            }

            onRepartitionEvent();

            addedPartitions.forEach(tpi -> partitionedEntities.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()));

            if (!addedPartitions.isEmpty()) {
                var fetchTasks = onAddedPartitions(addedPartitions);
                if (fetchTasks != null && !fetchTasks.isEmpty()) {
                    partitionedFetchTasks.putAll(fetchTasks);
                }
                partitionListChanged = true;
            }

            if (partitionListChanged) {
                List<ListenableFuture<?>> partitionFetchFutures = new ArrayList<>();
                partitionedFetchTasks.values().forEach(partitionFetchFutures::addAll);
                DonAsynchron.withCallback(Futures.allAsList(partitionFetchFutures), t -> logPartitions(), this::logFailure);
            }
        } catch (Throwable t) {
            log.warn("[{}] Failed to init entities state from DB", getServiceName(), t);
        }
    }

    /**
     * 功能：执行 `logFailure` 对应的处理。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    private void logFailure(Throwable e) {
        if (e instanceof CancellationException) {
            //Probably this is fine and happens due to re-balancing.
            log.trace("Partition fetch task error", e);
        } else {
            log.error("Partition fetch task error", e);
        }

    }

    /**
     * 功能：执行 `logPartitions` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void logPartitions() {
        log.info("[{}] Managing following partitions:", getServiceName());
        partitionedEntities.forEach((tpi, entities) -> {
            log.info("[{}][{}]: {} entities", getServiceName(), tpi.getFullTopicName(), entities.size());
        });
    }

    /**
     * 功能：处理事件。
     * 参数：无。
     * 返回：无。
     */
    protected void onRepartitionEvent() {
    }

    /**
     * 功能：获取`Latest Partitions`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private Set<TopicPartitionInfo> getLatestPartitions() {
        log.debug("getLatestPartitionsFromQueue, queue size {}", subscribeQueue.size());
        Set<TopicPartitionInfo> partitions = null;
        while (!subscribeQueue.isEmpty()) {
            partitions = subscribeQueue.poll();
            log.debug("polled from the queue partitions {}", partitions);
        }
        log.debug("getLatestPartitionsFromQueue, partitions {}", partitions);
        return partitions;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractPartitionBasedService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
