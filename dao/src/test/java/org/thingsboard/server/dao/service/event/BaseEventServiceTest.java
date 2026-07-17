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
package org.thingsboard.server.dao.service.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.text.ParseException;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

/**
 * 中文说明：
 * 1. `BaseEventServiceTest` 是 ThingsBoard DAO 中验证 `BaseEventService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public abstract class BaseEventServiceTest extends AbstractServiceTest {

    /**
     * 事件，提供当前类调用的业务操作。
     */
    @Autowired
    EventService eventService;

    /**
     * 开始时间戳，用于限定查询或统计的起点。
     */
    long timeBeforeStartTime;
    long startTime;
    /**
     * 事件，表示当前对象的对应属性。
     */
    long eventTime;
    long endTime;
    /**
     * 结束时间戳，用于限定查询或统计的终点。
     */
    long timeAfterEndTime;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() throws ParseException {
        timeBeforeStartTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T11:30:00Z").getTime();
        startTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:00:00Z").getTime();
        eventTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:30:00Z").getTime();
        endTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:00:00Z").getTime();
        timeAfterEndTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:30:30Z").getTime();
    }

    /**
     * 功能：保存或创建事件。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void saveEvent() throws Exception {
        TenantId tenantId = new TenantId(Uuids.timeBased());
        DeviceId devId = new DeviceId(Uuids.timeBased());
        RuleNodeDebugEvent event = generateEvent(tenantId, devId);
        eventService.saveAsync(event).get();
        List<EventInfo> loaded = eventService.findLatestEvents(event.getTenantId(), devId, event.getType(), 1);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(1, loaded.size());
        Assert.assertEquals(event.getData(), loaded.get(0).getBody().get("data").asText());
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void findEventsByTypeAndTimeAscOrder() throws Exception {
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        saveEventWithProvidedTime(timeBeforeStartTime, customerId, tenantId);
        Event savedEvent = saveEventWithProvidedTime(eventTime, customerId, tenantId);
        Event savedEvent2 = saveEventWithProvidedTime(eventTime + 1, customerId, tenantId);
        Event savedEvent3 = saveEventWithProvidedTime(eventTime + 2, customerId, tenantId);
        saveEventWithProvidedTime(timeAfterEndTime, customerId, tenantId);

        TimePageLink timePageLink = new TimePageLink(2, 0, "", new SortOrder("ts"), startTime, endTime);

        PageData<EventInfo> events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink);

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(2, events.getData().size());
        Assert.assertEquals(savedEvent.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertEquals(savedEvent2.getUuidId(), events.getData().get(1).getUuidId());
        Assert.assertTrue(events.hasNext());

        events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink.nextPageLink());

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(1, events.getData().size());
        Assert.assertEquals(savedEvent3.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertFalse(events.hasNext());

        eventService.cleanupEvents(timeBeforeStartTime - 1, timeAfterEndTime + 1, true);
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void findEventsByTypeAndTimeDescOrder() throws Exception {
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        saveEventWithProvidedTime(timeBeforeStartTime, customerId, tenantId);
        Event savedEvent = saveEventWithProvidedTime(eventTime, customerId, tenantId);
        Event savedEvent2 = saveEventWithProvidedTime(eventTime + 1, customerId, tenantId);
        Event savedEvent3 = saveEventWithProvidedTime(eventTime + 2, customerId, tenantId);
        saveEventWithProvidedTime(timeAfterEndTime, customerId, tenantId);

        TimePageLink timePageLink = new TimePageLink(2, 0, "", new SortOrder("ts", SortOrder.Direction.DESC), startTime, endTime);

        PageData<EventInfo> events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink);

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(2, events.getData().size());
        Assert.assertEquals(savedEvent3.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertEquals(savedEvent2.getUuidId(), events.getData().get(1).getUuidId());
        Assert.assertTrue(events.hasNext());

        events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink.nextPageLink());

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(1, events.getData().size());
        Assert.assertEquals(savedEvent.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertFalse(events.hasNext());

        eventService.cleanupEvents(timeBeforeStartTime - 1, timeAfterEndTime + 1, true);
    }

    /**
     * 功能：保存或创建事件。
     * 参数：
     * - `time`：`time` 参数。
     * - `entityId`：实体IDID。
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    private Event saveEventWithProvidedTime(long time, EntityId entityId, TenantId tenantId) throws Exception {
        RuleNodeDebugEvent event = generateEvent(tenantId, entityId);
        event.setId(new EventId(Uuids.timeBased()));
        event.setCreatedTime(time);
        eventService.saveAsync(event).get();
        return event;
    }
}
