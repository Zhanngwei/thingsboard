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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.edge.EdgeEventService;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;

/**
 * 中文说明：
 * 1. `EdgeEventServiceTest` 是 ThingsBoard DAO 中验证 `EdgeEventService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class EdgeEventServiceTest extends AbstractServiceTest {

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    EdgeEventService edgeEventService;

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
        timeBeforeStartTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T11:30:00").getTime();
        startTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T12:00:00").getTime();
        eventTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T12:30:00").getTime();
        endTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T13:00:00").getTime();
        timeAfterEndTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T13:30:30").getTime();
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void saveEdgeEvent() throws Exception {
        EdgeId edgeId = new EdgeId(Uuids.timeBased());
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, deviceId);
        edgeEventService.saveAsync(edgeEvent).get();

        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, 0L, null, new TimePageLink(1));
        Assert.assertFalse(edgeEvents.getData().isEmpty());

        EdgeEvent saved = edgeEvents.getData().get(0);
        Assert.assertEquals(saved.getTenantId(), edgeEvent.getTenantId());
        Assert.assertEquals(saved.getEdgeId(), edgeEvent.getEdgeId());
        Assert.assertEquals(saved.getEntityId(), edgeEvent.getEntityId());
        Assert.assertEquals(saved.getType(), edgeEvent.getType());
        Assert.assertEquals(saved.getAction(), edgeEvent.getAction());
        Assert.assertEquals(saved.getBody(), edgeEvent.getBody());

        edgeEventService.cleanupEvents(1);
    }

    /**
     * 功能：执行 `generateEdgeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    protected EdgeEvent generateEdgeEvent(TenantId tenantId, EdgeId edgeId, EntityId entityId) throws IOException {
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(Uuids.timeBased());
        }
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setEntityId(entityId.getId());
        edgeEvent.setType(EdgeEventType.DEVICE);
        edgeEvent.setAction(EdgeEventActionType.ADDED);
        edgeEvent.setBody(readFromResource("TestJsonData.json"));
        return edgeEvent;
    }

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void findEdgeEventsByTimeDescOrder() throws Exception {
        EdgeId edgeId = new EdgeId(Uuids.timeBased());
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(saveEdgeEventWithProvidedTime(timeBeforeStartTime, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime + 1, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime + 2, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(timeAfterEndTime, edgeId, deviceId, tenantId));

        Futures.allAsList(futures).get();

        TimePageLink pageLink = new TimePageLink(2, 0, "", new SortOrder("createdTime", SortOrder.Direction.DESC), startTime, endTime);
        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, 0L, null, pageLink);

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(2, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime + 2), edgeEvents.getData().get(0).getUuidId());
        Assert.assertEquals(Uuids.startOf(eventTime + 1), edgeEvents.getData().get(1).getUuidId());
        Assert.assertTrue(edgeEvents.hasNext());
        Assert.assertNotNull(pageLink.nextPageLink());

        edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, 0L, null, pageLink.nextPageLink());

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(1, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime), edgeEvents.getData().get(0).getUuidId());
        Assert.assertFalse(edgeEvents.hasNext());

        edgeEventService.cleanupEvents(1);
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：
     * - `time`：`time` 参数。
     * - `edgeId`：边缘节点ID。
     * - `entityId`：实体IDID。
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> saveEdgeEventWithProvidedTime(long time, EdgeId edgeId, EntityId entityId, TenantId tenantId) throws Exception {
        EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, entityId);
        edgeEvent.setId(new EdgeEventId(Uuids.startOf(time)));
        return edgeEventService.saveAsync(edgeEvent);
    }
}