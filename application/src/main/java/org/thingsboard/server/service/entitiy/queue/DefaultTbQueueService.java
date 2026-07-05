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
package org.thingsboard.server.service.entitiy.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbQueueService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbQueueService extends AbstractTbEntityService implements TbQueueService {

    /**
     * 队列，提供当前类调用的业务操作。
     */
    private final QueueService queueService;
    private final TbClusterService tbClusterService;
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private final TbQueueAdmin tbQueueAdmin;

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    @Override
    public Queue saveQueue(Queue queue) {
        boolean create = queue.getId() == null;
        Queue oldQueue;
        if (create) {
            oldQueue = null;
        } else {
            oldQueue = queueService.findQueueById(queue.getTenantId(), queue.getId());
        }

        Queue savedQueue = queueService.saveQueue(queue);
        createTopicsIfNeeded(savedQueue, oldQueue);
        tbClusterService.onQueuesUpdate(List.of(savedQueue));
        return savedQueue;
    }

    /**
     * 功能：删除或清理队列。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueId`：队列ID。
     * 返回：无。
     */
    @Override
    public void deleteQueue(TenantId tenantId, QueueId queueId) {
        Queue queue = queueService.findQueueById(tenantId, queueId);
        queueService.deleteQueue(tenantId, queueId);
        tbClusterService.onQueuesDelete(List.of(queue));
    }

    /**
     * 功能：删除或清理队列名称。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueName`：队列名称或队列对象。
     * 返回：无。
     */
    @Override
    public void deleteQueueByQueueName(TenantId tenantId, String queueName) {
        Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, queueName);
        queueService.deleteQueue(tenantId, queue.getId());
        tbClusterService.onQueuesDelete(List.of(queue));
    }

    /**
     * 功能：更新`Queues By Tenants`。
     * 参数：
     * - `tenantIds`：租户信息或租户标识。
     * - `newTenantProfile`：租户信息或租户标识。
     * - `oldTenantProfile`：租户信息或租户标识。
     * 返回：无。
     */
    @Override
    public void updateQueuesByTenants(List<TenantId> tenantIds, TenantProfile newTenantProfile, TenantProfile
            oldTenantProfile) {
        boolean oldIsolated = oldTenantProfile != null && oldTenantProfile.isIsolatedTbRuleEngine();
        boolean newIsolated = newTenantProfile.isIsolatedTbRuleEngine();

        if (!oldIsolated && !newIsolated) {
            return;
        }

        if (newTenantProfile.equals(oldTenantProfile)) {
            return;
        }

        Map<String, TenantProfileQueueConfiguration> oldQueues;
        Map<String, TenantProfileQueueConfiguration> newQueues;

        if (oldIsolated) {
            oldQueues = oldTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            oldQueues = Collections.emptyMap();
        }

        if (newIsolated) {
            newQueues = newTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            newQueues = Collections.emptyMap();
        }

        List<String> toRemove = new ArrayList<>();
        List<String> toCreate = new ArrayList<>();
        List<String> toUpdate = new ArrayList<>();

        for (String oldQueue : oldQueues.keySet()) {
            if (!newQueues.containsKey(oldQueue)) {
                toRemove.add(oldQueue);
            }
        }

        for (String newQueue : newQueues.keySet()) {
            if (oldQueues.containsKey(newQueue)) {
                toUpdate.add(newQueue);
            } else {
                toCreate.add(newQueue);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] Handling profile queue config update: creating queues {}, updating {}, deleting {}. Affected tenants: {}",
                    newTenantProfile.getUuidId(), toCreate, toUpdate, toRemove, tenantIds);
        }

        List<Queue> updated = new ArrayList<>();
        List<Queue> deleted = new ArrayList<>();
        for (TenantId tenantId : tenantIds) {
            for (String name : toCreate) {
                updated.add(new Queue(tenantId, newQueues.get(name)));
            }

            for (String name : toUpdate) {
                Queue queue = new Queue(tenantId, newQueues.get(name));
                Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, name);
                if (foundQueue != null) {
                    queue.setId(foundQueue.getId());
                    queue.setCreatedTime(foundQueue.getCreatedTime());
                }
                if (!queue.equals(foundQueue)) {
                    updated.add(queue);
                    createTopicsIfNeeded(queue, foundQueue);
                }
            }

            for (String name : toRemove) {
                Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, name);
                deleted.add(queue);
            }
        }

        if (!updated.isEmpty()) {
            updated = updated.stream()
                    .map(queueService::saveQueue)
                    .collect(Collectors.toList());
            tbClusterService.onQueuesUpdate(updated);
        }
        if (!deleted.isEmpty()) {
            deleted.forEach(queue -> {
                queueService.deleteQueue(queue.getTenantId(), queue.getId());
            });
            tbClusterService.onQueuesDelete(deleted);
        }
    }

    /**
     * 功能：保存或创建`Topics If Needed`。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * - `oldQueue`：队列名称或队列对象。
     * 返回：无。
     */
    private void createTopicsIfNeeded(Queue queue, Queue oldQueue) {
        int newPartitions = queue.getPartitions();
        int oldPartitions = oldQueue != null ? oldQueue.getPartitions() : 0;
        for (int i = oldPartitions; i < newPartitions; i++) {
            tbQueueAdmin.createTopicIfNotExists(
                    new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName(),
                    queue.getCustomProperties()
            );
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbQueueService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
