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
 * 1. `JpaBaseEventDaoTest` 是 ThingsBoard DAO 中验证 `JpaBaseEventDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
