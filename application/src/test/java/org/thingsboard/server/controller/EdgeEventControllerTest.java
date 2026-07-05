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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.service.ttl.EdgeEventsCleanUpService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. 类目的：`EdgeEventControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
@Slf4j
@DaoSqlTest
public class EdgeEventControllerTest extends AbstractControllerTest {

    /**
     * 边缘节点，用于读取或保存对应领域对象。
     */
    @Autowired
    private EdgeEventDao edgeEventDao;
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @SpyBean
    private SqlPartitioningRepository partitioningRepository;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    private EdgeEventsCleanUpService edgeEventsCleanUpService;

    /**
     * 分区，用于定位消息或数据所属分区。
     */
    @Value("#{${sql.edge_events.partition_size} * 60 * 60 * 1000}")
    private long partitionDurationInMs;
    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    @Value("${sql.ttl.edge_events.edge_event_ttl}")
    private long edgeEventTtlInSec;

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    /**
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws Exception {
    }

    /**
     * 功能：验证边缘节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetEdgeEvents() throws Exception {
        Edge edge = constructEdge("TestEdge", "default");
        edge = doPost("/api/edge", edge, Edge.class);

        // simulate edge activation
        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributes);

        Device device = constructDevice("TestDevice", "default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        final EdgeId edgeId = edge.getId();
        doPost("/api/edge/" + edgeId.toString() + "/device/" + savedDevice.getId().toString(), Device.class);

        Asset asset = constructAsset("TestAsset", "default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        doPost("/api/edge/" + edgeId.toString() + "/asset/" + savedAsset.getId().toString(), Asset.class);

        EntityRelation relation = new EntityRelation(savedAsset.getId(), savedDevice.getId(), EntityRelation.CONTAINS_TYPE);

        awaitForNumberOfEdgeEvents(edgeId, 2);

        doPost("/api/relation", relation);

        awaitForNumberOfEdgeEvents(edgeId, 3);

        List<EdgeEvent> edgeEvents = findEdgeEvents(edgeId);
        Assert.assertTrue(popEdgeEvent(edgeEvents, EdgeEventType.DEVICE)); // TestDevice
        Assert.assertTrue(popEdgeEvent(edgeEvents, EdgeEventType.ASSET)); // TestAsset
        Assert.assertTrue(popEdgeEvent(edgeEvents, EdgeEventType.RELATION));
        Assert.assertTrue(edgeEvents.isEmpty());
    }

    /**
     * 功能：执行 `popEdgeEvent` 对应的处理。
     * 参数：
     * - `edgeEvents`：数据列表。
     * - `edgeEventType`：类型。
     * 返回：判断结果。
     */
    private boolean popEdgeEvent(List<EdgeEvent> edgeEvents, EdgeEventType edgeEventType) {
        for (EdgeEvent edgeEvent : edgeEvents) {
            if (edgeEventType.equals(edgeEvent.getType())) {
                edgeEvents.remove(edgeEvent);
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：执行 `awaitForNumberOfEdgeEvents` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `expectedNumber`：`expectedNumber` 参数。
     * 返回：无。
     */
    private void awaitForNumberOfEdgeEvents(EdgeId edgeId, int expectedNumber) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EdgeEvent> edgeEvents = findEdgeEvents(edgeId);
                    return edgeEvents.size() == expectedNumber;
                });
    }

    /**
     * 功能：验证 `saveEdgeEvent_thenCreatePartitionIfNotExist` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void saveEdgeEvent_thenCreatePartitionIfNotExist() {
        reset(partitioningRepository);
        EdgeEvent edgeEvent = createEdgeEvent();
        verify(partitioningRepository).createPartitionIfNotExists(eq("edge_event"), eq(edgeEvent.getCreatedTime()), eq(partitionDurationInMs));
        List<Long> partitions = partitioningRepository.fetchPartitions("edge_event");
        assertThat(partitions).singleElement().satisfies(partitionStartTs -> {
            assertThat(partitionStartTs).isEqualTo(partitioningRepository.calculatePartitionStartTime(edgeEvent.getCreatedTime(), partitionDurationInMs));
        });
    }

    /**
     * 功能：删除或清理边缘节点。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void cleanUpEdgeEventByTtl_dropOldPartitions() {
        long oldEdgeEventTs = LocalDate.of(2020, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long partitionStartTs = partitioningRepository.calculatePartitionStartTime(oldEdgeEventTs, partitionDurationInMs);
        partitioningRepository.createPartitionIfNotExists("edge_event", oldEdgeEventTs, partitionDurationInMs);
        List<Long> partitions = partitioningRepository.fetchPartitions("edge_event");
        assertThat(partitions).contains(partitionStartTs);

        edgeEventsCleanUpService.cleanUp();
        partitions = partitioningRepository.fetchPartitions("edge_event");
        assertThat(partitions).doesNotContain(partitionStartTs);
        assertThat(partitions).allSatisfy(partitionsStart -> {
            long partitionEndTs = partitionsStart + partitionDurationInMs;
            assertThat(partitionEndTs).isGreaterThan(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(edgeEventTtlInSec));
        });
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    private List<EdgeEvent> findEdgeEvents(EdgeId edgeId) throws Exception {
        return doGetTypedWithTimePageLink("/api/edge/" + edgeId.toString() + "/events?",
                new TypeReference<PageData<EdgeEvent>>() {
                }, new TimePageLink(10)).getData();
    }

    /**
     * 功能：执行 `constructDevice` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `type`：类型。
     * 返回：处理结果。
     */
    private Device constructDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return device;
    }

    /**
     * 功能：执行 `constructAsset` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    private Asset constructAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return asset;
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：无。
     * 返回：处理结果。
     */
    private EdgeEvent createEdgeEvent() {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setCreatedTime(System.currentTimeMillis());
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(EdgeEventActionType.ADDED);
        edgeEvent.setEntityId(tenantAdminUser.getUuidId());
        edgeEvent.setType(EdgeEventType.ALARM);
        try {
            edgeEventDao.saveAsync(edgeEvent).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return edgeEvent;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeEventControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
