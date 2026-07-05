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
package org.thingsboard.server.dao.sql.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.event.EventDao;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 中文说明：
 * 1. 类目的：`JpaBaseEventDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Slf4j
public class JpaBaseEventDaoTest extends AbstractJpaDaoTest {

    /**
     * 事件，用于读取或保存对应领域对象。
     */
    @Autowired
    private EventDao eventDao;
    UUID tenantId = Uuids.timeBased();


    /**
     * 功能：获取事件。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void findEvent() throws InterruptedException, ExecutionException, TimeoutException {
        UUID entityId = Uuids.timeBased();

        Event event1 = getStatsEvent(Uuids.timeBased(), tenantId, entityId);
        eventDao.saveAsync(event1).get(1, TimeUnit.MINUTES);
        Thread.sleep(2);
        Event event2 = getStatsEvent(Uuids.timeBased(), tenantId, entityId);
        eventDao.saveAsync(event2).get(1, TimeUnit.MINUTES);

        List<? extends Event> foundEvents = eventDao.findLatestEvents(tenantId, entityId, EventType.STATS, 1);
        assertNotNull("Events expected to be not null", foundEvents);
        assertEquals(1, foundEvents.size());
        assertEquals(event2, foundEvents.get(0));
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void findEventsByEntityIdAndPageLink() throws Exception {
        UUID entityId1 = Uuids.timeBased();
        UUID entityId2 = Uuids.timeBased();
        long startTime = System.currentTimeMillis();

        Event event1 = getStatsEvent(Uuids.timeBased(), tenantId, entityId1);
        eventDao.saveAsync(event1).get(1, TimeUnit.MINUTES);
        Thread.sleep(2);
        Event event2 = getStatsEvent(Uuids.timeBased(), tenantId, entityId2);
        eventDao.saveAsync(event2).get(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        PageData<? extends Event> events1 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, new TimePageLink(30));
        assertEquals(1, events1.getData().size());

        PageData<? extends Event> events2 = eventDao.findEvents(tenantId, entityId2, EventType.STATS, new TimePageLink(30));
        assertEquals(1, events2.getData().size());

        PageData<? extends Event> events3 = eventDao.findEvents(tenantId, Uuids.timeBased(), EventType.STATS, new TimePageLink(30));
        assertEquals(0, events3.getData().size());


        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
        PageData<? extends Event> events12 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink2);
        assertEquals(1, events12.getData().size());
        assertEquals(event1, events12.getData().get(0));

        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
        PageData<? extends Event> events13 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink3);
        assertEquals(1, events13.getData().size());
        assertEquals(event1, events13.getData().get(0));

        TimePageLink pageLink4 = new TimePageLink(5, 0, "", null, startTime, endTime);
        PageData<? extends Event> events14 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink4);
        assertEquals(1, events14.getData().size());
        assertEquals(event1, events14.getData().get(0));

        pageLink4 = pageLink4.nextPageLink();
        PageData<? extends Event> events6 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink4);
        assertEquals(0, events6.getData().size());

    }

    /**
     * 功能：获取事件。
     * 参数：
     * - `eventId`：事件ID。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private Event getStatsEvent(UUID eventId, UUID tenantId, UUID entityId) {
        StatisticsEvent.StatisticsEventBuilder event = StatisticsEvent.builder();
        event.id(eventId);
        event.ts(System.currentTimeMillis());
        event.tenantId(new TenantId(tenantId));
        event.entityId(entityId);
        event.serviceId("server A");
        event.messagesProcessed(1);
        event.errorsOccurred(0);
        return event.build();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaBaseEventDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
