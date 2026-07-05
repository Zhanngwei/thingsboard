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
package org.thingsboard.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.UUID;

/**
 * The Interface EventDao.
 */
/**
 * 中文说明：
 * 1. 类目的：`EventDao` 是 ThingsBoard DAO 模块 中的审计与事件持久化类型，用于记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 生命周期：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Observer / Event Sourcing / Repository。
 */
public interface EventDao {

    /**
     * Save or update event object async
     *
     * @param event the event object
     * @return saved event object future
     */
    /**
     * 功能：保存或创建`Async`。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAsync(Event event);

    /**
     * Find events by tenantId, entityId, eventType and pageLink.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param pageLink the pageLink
     * @return the event list
     */
    /**
     * 功能：获取`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, TimePageLink pageLink);

    /**
     * 功能：获取事件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventFilter`：`eventFilter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, EventFilter eventFilter, TimePageLink pageLink);

    /**
     * Find latest events by tenantId, entityId and eventType.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param limit the limit
     * @return the event list
     */
    /**
     * 功能：获取`Latest Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `limit`：数量限制。
     * 返回：处理结果。
     */
    List<? extends Event> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit);

    /**
     * Executes stored procedure to cleanup old events. Uses separate ttl for debug and other events.
     * @param regularEventExpTs the expiration time of the regular events
     * @param debugEventExpTs the expiration time of the debug events
     * @param cleanupDb
     */
    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `regularEventExpTs`：时间戳。
     * - `debugEventExpTs`：时间戳。
     * - `cleanupDb`：`cleanupDb` 参数。
     * 返回：无。
     */
    void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb);

    /**
     * Removes all events for the specified entity and time interval
     *
     * @param tenantId
     * @param entityId
     * @param startTime
     * @param endTime
     */
    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * 返回：无。
     */
    void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime);

    /**
     *
     * Removes all events for the specified entity, event filter and time interval
     *
     * @param tenantId
     * @param entityId
     * @param eventFilter
     * @param startTime
     * @param endTime
     */
    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventFilter`：`eventFilter` 参数。
     * - `startTime`：开始时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void removeEvents(UUID tenantId, UUID entityId, EventFilter eventFilter, Long startTime, Long endTime);

    /**
     * 功能：执行 `migrateEvents` 对应的处理。
     * 参数：
     * - `regularEventTs`：时间戳。
     * - `debugEventTs`：时间戳。
     * 返回：无。
     */
    void migrateEvents(long regularEventTs, long debugEventTs);
}

/*
 * 本类总结：
 * 1. 核心职责：`EventDao` 在 ThingsBoard DAO 模块 中承担审计与事件持久化类型职责，核心目的是记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 核心流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
 * 3. 关键依赖：主要依赖或协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
