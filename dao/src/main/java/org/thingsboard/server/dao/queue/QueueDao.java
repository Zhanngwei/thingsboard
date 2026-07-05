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

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.Dao;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`QueueDao` 是 ThingsBoard DAO 模块 中的队列配置持久化类型，用于维护 ThingsBoard 队列配置、分区设置和队列服务需要的数据库契约。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括QueueService、Rule Engine、Transport、Application 配置和 DAO。
 * 4. 生命周期：由队列配置管理、启动加载或规则链路由流程读取和更新。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository。
 */
public interface QueueDao extends Dao<Queue> {
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    Queue findQueueByTenantIdAndTopic(TenantId tenantId, String topic);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    Queue findQueueByTenantIdAndName(TenantId tenantId, String name);

    /**
     * 功能：获取`All Main Queues`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    List<Queue> findAllMainQueues();

    /**
     * 功能：获取`All Queues`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    List<Queue> findAllQueues();

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    List<Queue> findAllByTenantId(TenantId tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Queue> findQueuesByTenantId(TenantId tenantId, PageLink pageLink);

/*
 * 本类总结：
 * 1. 核心职责：`QueueDao` 在 ThingsBoard DAO 模块 中承担队列配置持久化类型职责，核心目的是维护 ThingsBoard 队列配置、分区设置和队列服务需要的数据库契约。
 * 2. 核心流程：从数据库读取队列配置并返回给队列路由、规则引擎或传输层使用。
 * 3. 关键依赖：主要依赖或协作对象包括QueueService、Rule Engine、Transport、Application 配置和 DAO。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
}