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
 * 1. 类目的：`EdgeEventServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
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

/*
 * 本类总结：
 * 1. 核心职责：`EdgeEventServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
}