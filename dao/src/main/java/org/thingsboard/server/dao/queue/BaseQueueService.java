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
package org.thingsboard.server.dao.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`BaseQueueService` 是 ThingsBoard DAO 模块 中的队列配置持久化类型，用于维护 ThingsBoard 队列配置、分区设置和队列服务需要的数据库契约。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括QueueService、Rule Engine、Transport、Application 配置和 DAO。
 * 4. 生命周期：由队列配置管理、启动加载或规则链路由流程读取和更新。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository。
 */
@Service("QueueDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseQueueService extends AbstractEntityService implements QueueService {

    /**
     * 队列，用于读取或保存对应领域对象。
     */
    @Autowired
    private QueueDao queueDao;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Lazy
    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    @Autowired
    private DataValidator<Queue> queueValidator;

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    @Override
    public Queue saveQueue(Queue queue) {
        log.trace("Executing createOrUpdateQueue [{}]", queue);
        queueValidator.validate(queue, Queue::getTenantId);
        Queue savedQueue = queueDao.save(queue.getTenantId(), queue);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedQueue.getTenantId())
                .entityId(savedQueue.getId()).created(queue.getId() == null).build());
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
        log.trace("Executing deleteQueue, queueId: [{}]", queueId);
        try {
            queueDao.removeById(tenantId, queueId.getId());
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(queueId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_queue_device_profile")) {
                throw new DataValidationException("The queue referenced by the device profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Queue> findQueuesByTenantId(TenantId tenantId) {
        log.trace("Executing findQueues, tenantId: [{}]", tenantId);
        return queueDao.findAllByTenantId(getSystemOrIsolatedTenantId(tenantId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Queue> findQueuesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findQueues pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return queueDao.findQueuesByTenantId(getSystemOrIsolatedTenantId(tenantId), pageLink);
    }

    /**
     * 功能：获取`All Queues`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Queue> findAllQueues() {
        log.trace("Executing findAllQueues");
        return queueDao.findAllQueues();
    }

    /**
     * 功能：获取队列。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueId`：队列ID。
     * 返回：处理结果。
     */
    @Override
    public Queue findQueueById(TenantId tenantId, QueueId queueId) {
        log.trace("Executing findQueueById, queueId: [{}]", queueId);
        return queueDao.findById(tenantId, queueId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueName`：队列名称或队列对象。
     * 返回：处理结果。
     */
    @Override
    public Queue findQueueByTenantIdAndName(TenantId tenantId, String queueName) {
        log.trace("Executing findQueueByTenantIdAndName, tenantId: [{}] queueName: [{}]", tenantId, queueName);
        return queueDao.findQueueByTenantIdAndName(getSystemOrIsolatedTenantId(tenantId), queueName);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueName`：队列名称或队列对象。
     * 返回：处理结果。
     */
    @Override
    public Queue findQueueByTenantIdAndNameInternal(TenantId tenantId, String queueName) {
        log.trace("Executing findQueueByTenantIdAndNameInternal, tenantId: [{}] queueName: [{}]", tenantId, queueName);
        return queueDao.findQueueByTenantIdAndName(tenantId, queueName);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteQueuesByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete queues request.");
        tenantQueuesRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findQueueById(tenantId, new QueueId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE;
    }

    private PaginatedRemover<TenantId, Queue> tenantQueuesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Queue> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return queueDao.findQueuesByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Queue entity) {
                    deleteQueue(tenantId, entity.getId());
                }
            };

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    private TenantId getSystemOrIsolatedTenantId(TenantId tenantId) {
        if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            if (tenantProfile.isIsolatedTbRuleEngine()) {
                return tenantId;
            }
        }

        return TenantId.SYS_TENANT_ID;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseQueueService` 在 ThingsBoard DAO 模块 中承担队列配置持久化类型职责，核心目的是维护 ThingsBoard 队列配置、分区设置和队列服务需要的数据库契约。
 * 2. 核心流程：从数据库读取队列配置并返回给队列路由、规则引擎或传输层使用。
 * 3. 关键依赖：主要依赖或协作对象包括QueueService、Rule Engine、Transport、Application 配置和 DAO。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
