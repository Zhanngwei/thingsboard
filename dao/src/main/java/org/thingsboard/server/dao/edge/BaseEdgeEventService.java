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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.service.DataValidator;

/**
 * 中文说明：
 * 1. 类目的：`BaseEdgeEventService` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Service
@Slf4j
@AllArgsConstructor
public class BaseEdgeEventService implements EdgeEventService {

    /**
     * 边缘节点，用于读取或保存对应领域对象。
     */
    private final EdgeEventDao edgeEventDao;
    private final RateLimitService rateLimitService;
    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    private final DataValidator<EdgeEvent> edgeEventValidator;

    /**
     * 功能：保存或创建`Async`。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        if (!rateLimitService.checkRateLimit(LimitedApi.EDGE_EVENTS, edgeEvent.getTenantId())) {
            throw new TbRateLimitsException(EntityType.TENANT);
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.EDGE_EVENTS_PER_EDGE, edgeEvent.getTenantId(), edgeEvent.getEdgeId())) {
            throw new TbRateLimitsException(EntityType.EDGE);
        }
        edgeEventValidator.validate(edgeEvent, EdgeEvent::getTenantId);
        return edgeEventDao.saveAsync(edgeEvent);
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `seqIdStart`：`seqIdStart` 参数。
     * - `seqIdEnd`：`seqIdEnd` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return edgeEventDao.findEdgeEvents(tenantId.getId(), edgeId, seqIdStart, seqIdEnd, pageLink);
    }

    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `ttl`：`ttl` 参数。
     * 返回：无。
     */
    @Override
    public void cleanupEvents(long ttl) {
        edgeEventDao.cleanupEvents(ttl);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseEdgeEventService` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
