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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`BaseEventService` 是 ThingsBoard DAO 模块 中的审计与事件持久化类型，用于记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 生命周期：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Observer / Event Sourcing / Repository。
 */
@Service
@Slf4j
public class BaseEventService implements EventService {

    /**
     * `ttlInSec` 字段，保存当前对象的对应属性。
     */
    @Value("${sql.ttl.events.events_ttl:0}")
    private long ttlInSec;
    /**
     * `debugTtlInSec` 字段，保存当前对象的对应属性。
     */
    @Value("${sql.ttl.events.debug_events_ttl:604800}")
    private long debugTtlInSec;

    /**
     * 事件，表示当前对象的对应属性。
     */
    @Value("${event.debug.max-symbols:4096}")
    private int maxDebugEventSymbols;

    /**
     * 事件，用于读取或保存对应领域对象。
     */
    @Autowired
    public EventDao eventDao;

    /**
     * 事件，表示当前对象的对应属性。
     */
    @Autowired
    private DataValidator<Event> eventValidator;

    /**
     * 功能：保存或创建`Async`。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAsync(Event event) {
        eventValidator.validate(event, Event::getTenantId);
        checkAndTruncateDebugEvent(event);
        return eventDao.saveAsync(event);
    }

    /**
     * 功能：校验事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    private void checkAndTruncateDebugEvent(Event event) {
        switch (event.getType()) {
            case DEBUG_RULE_NODE:
                RuleNodeDebugEvent rnEvent = (RuleNodeDebugEvent) event;
                truncateField(rnEvent, RuleNodeDebugEvent::getData, RuleNodeDebugEvent::setData);
                truncateField(rnEvent, RuleNodeDebugEvent::getMetadata, RuleNodeDebugEvent::setMetadata);
                truncateField(rnEvent, RuleNodeDebugEvent::getError, RuleNodeDebugEvent::setError);
                break;
            case DEBUG_RULE_CHAIN:
                RuleChainDebugEvent rcEvent = (RuleChainDebugEvent) event;
                truncateField(rcEvent, RuleChainDebugEvent::getMessage, RuleChainDebugEvent::setMessage);
                truncateField(rcEvent, RuleChainDebugEvent::getError, RuleChainDebugEvent::setError);
                break;
            case LC_EVENT:
                LifecycleEvent lcEvent = (LifecycleEvent) event;
                truncateField(lcEvent, LifecycleEvent::getError, LifecycleEvent::setError);
                break;
            case ERROR:
                ErrorEvent eEvent = (ErrorEvent) event;
                truncateField(eEvent, ErrorEvent::getError, ErrorEvent::setError);
                break;
        }
    }

    /**
     * 功能：执行 `truncateField` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * - `getter`：`getter` 参数。
     * - `setter`：`setter` 参数。
     * 返回：无。
     */
    private <T extends Event> void truncateField(T event, Function<T, String> getter, BiConsumer<T, String> setter) {
        var str = getter.apply(event);
        str = StringUtils.truncate(str, maxDebugEventSymbols);
        setter.accept(event, str);
    }

    /**
     * 功能：获取`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EventInfo> findEvents(TenantId tenantId, EntityId entityId, EventType eventType, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEvents(tenantId.getId(), entityId.getId(), eventType, pageLink));
    }

    /**
     * 功能：获取`Latest Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EventInfo> findLatestEvents(TenantId tenantId, EntityId entityId, EventType eventType, int limit) {
        return convert(entityId.getEntityType(), eventDao.findLatestEvents(tenantId.getId(), entityId.getId(), eventType, limit));
    }

    /**
     * 功能：获取`Events By Filter`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventFilter`：`eventFilter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EventInfo> findEventsByFilter(TenantId tenantId, EntityId entityId, EventFilter eventFilter, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEventByFilter(tenantId.getId(), entityId.getId(), eventFilter, pageLink));
    }

    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId) {
        removeEvents(tenantId, entityId, null, null, null);
    }

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
    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId, EventFilter eventFilter, Long startTime, Long endTime) {
        if (eventFilter == null) {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), startTime, endTime);
        } else {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), eventFilter, startTime, endTime);
        }
    }

    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `regularEventExpTs`：时间戳。
     * - `debugEventExpTs`：时间戳。
     * - `cleanupDb`：`cleanupDb` 参数。
     * 返回：无。
     */
    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {
        eventDao.cleanupEvents(regularEventExpTs, debugEventExpTs, cleanupDb);
    }

    /**
     * 功能：执行 `convert` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * - `pd`：`pd` 参数。
     * 返回：匹配的数据集合。
     */
    private PageData<EventInfo> convert(EntityType entityType, PageData<? extends Event> pd) {
        return new PageData<>(pd.getData() == null ? null :
                pd.getData().stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList())
                , pd.getTotalPages(), pd.getTotalElements(), pd.hasNext());
    }

    /**
     * 功能：执行 `convert` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * - `list`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<EventInfo> convert(EntityType entityType, List<? extends Event> list) {
        return list == null ? null : list.stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseEventService` 在 ThingsBoard DAO 模块 中承担审计与事件持久化类型职责，核心目的是记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 核心流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
 * 3. 关键依赖：主要依赖或协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
