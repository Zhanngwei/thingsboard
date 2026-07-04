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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.concurrent.TimeUnit;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`TelemetryEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
public class TelemetryEdgeTest extends AbstractEdgeTest {

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTimeseriesWithFailures` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTimeseriesWithFailures() throws Exception {
        int numberOfTimeseriesToSend = 333;

        Device device = findDeviceByName("Edge Device 1");

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
        // imitator will generate failure in 5% of cases
        edgeImitator.setFailureProbability(5.0);
        edgeImitator.expectMessageAmount(numberOfTimeseriesToSend);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            String timeseriesData = "{\"data\":{\"idx\":" + idx + "},\"ts\":" + System.currentTimeMillis() + "}";
            JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
            EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
            edgeEventService.saveAsync(edgeEvent).get();
            clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        }

        Assert.assertTrue(edgeImitator.waitForMessages(120));

        List<EntityDataProto> allTelemetryMsgs = edgeImitator.findAllMessagesByType(EntityDataProto.class);
        Assert.assertEquals(numberOfTimeseriesToSend, allTelemetryMsgs.size());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            Assert.assertTrue(isIdxExistsInTheDownlinkList(idx, allTelemetryMsgs));
        }

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(false);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAttributes` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAttributes() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        testAttributesUpdatedMsg(device.getId());
        testPostAttributesMsg(device);
        testAttributesDeleteMsg(device);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testPostAttributesMsg` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testPostAttributesMsg(Device device) throws Exception {
        String postAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key2\":\"value2\"}}";
        JsonNode postAttributesEntityData = JacksonUtil.toJsonNode(postAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.POST_ATTRIBUTES, device.getId().getId(), EdgeEventType.DEVICE, postAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasPostAttributesMsg());

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.PostAttributeMsg postAttributesMsg = latestEntityDataMsg.getPostAttributesMsg();
        Assert.assertEquals(1, postAttributesMsg.getKvCount());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.KeyValueProto keyValueProto = postAttributesMsg.getKv(0);
        Assert.assertEquals("key2", keyValueProto.getKey());
        Assert.assertEquals("value2", keyValueProto.getStringV());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testAttributesDeleteMsg` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testAttributesDeleteMsg(Device device) throws Exception {
        String deleteAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"keys\":[\"key1\",\"key2\"]}";
        JsonNode deleteAttributesEntityData = JacksonUtil.toJsonNode(deleteAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_DELETED, device.getId().getId(), EdgeEventType.DEVICE, deleteAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());

        Assert.assertTrue(latestEntityDataMsg.hasAttributeDeleteMsg());

        AttributeDeleteMsg attributeDeleteMsg = latestEntityDataMsg.getAttributeDeleteMsg();
        Assert.assertEquals(attributeDeleteMsg.getScope(), deleteAttributesEntityData.get("scope").asText());

        Assert.assertEquals(2, attributeDeleteMsg.getAttributeNamesCount());
        Assert.assertEquals("key1", attributeDeleteMsg.getAttributeNames(0));
        Assert.assertEquals("key2", attributeDeleteMsg.getAttributeNames(1));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTimeseries` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTimeseries() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        String timeseriesData = "{\"data\":{\"temperature\":25},\"ts\":" + System.currentTimeMillis() + "}";
        JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
        edgeImitator.expectMessageAmount(1);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
        edgeEventService.saveAsync(edgeEvent).get();
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(latestEntityDataMsg.getEntityIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(latestEntityDataMsg.getEntityIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(latestEntityDataMsg.getEntityType(), device.getId().getEntityType().name());
        Assert.assertTrue(latestEntityDataMsg.hasPostTelemetryMsg());

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.PostTelemetryMsg postTelemetryMsg = latestEntityDataMsg.getPostTelemetryMsg();
        Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
        Assert.assertEquals(timeseriesEntityData.get("ts").asLong(), tsKvListProto.getTs());
        Assert.assertEquals(1, tsKvListProto.getKvCount());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
        Assert.assertEquals("temperature", keyValueProto.getKey());
        Assert.assertEquals(25, keyValueProto.getLongV());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isIdxExistsInTheDownlinkList` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isIdxExistsInTheDownlinkList(int idx, List<EntityDataProto> allTelemetryMsgs) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (EntityDataProto proto : allTelemetryMsgs) {
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.PostTelemetryMsg postTelemetryMsg = proto.getPostTelemetryMsg();
            Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
            Assert.assertEquals(1, tsKvListProto.getKvCount());
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
            Assert.assertEquals("idx", keyValueProto.getKey());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (keyValueProto.getLongV() == idx) {
                return true;
            }
        }
        return false;
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTimeseriesDeliveryFailuresForever_deliverOnlyDeviceUpdateMsgs` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTimeseriesDeliveryFailuresForever_deliverOnlyDeviceUpdateMsgs() throws Exception {
        int numberOfMsgsToSend = 100;

        Device device = findDeviceByName("Edge Device 1");

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
        // imitator will generate failure in 100% of timeseries cases
        edgeImitator.setFailureProbability(100);
        edgeImitator.expectMessageAmount(numberOfMsgsToSend);
        for (int idx = 1; idx <= numberOfMsgsToSend; idx++) {
            String timeseriesData = "{\"data\":{\"idx\":" + idx + "},\"ts\":" + System.currentTimeMillis() + "}";
            JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
            EdgeEvent failedEdgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
            edgeEventService.saveAsync(failedEdgeEvent).get();

            EdgeEvent successEdgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, null);
            edgeEventService.saveAsync(successEdgeEvent).get();

            clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        }

        Assert.assertTrue(edgeImitator.waitForMessages(120));

        List<EntityDataProto> allTelemetryMsgs = edgeImitator.findAllMessagesByType(EntityDataProto.class);
        Assert.assertTrue(allTelemetryMsgs.isEmpty());

        List<DeviceUpdateMsg> deviceUpdateMsgs = edgeImitator.findAllMessagesByType(DeviceUpdateMsg.class);
        Assert.assertEquals(numberOfMsgsToSend, deviceUpdateMsgs.size());

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(false);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAttributesUpdatedMsg_userEntity` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAttributesUpdatedMsg_userEntity() throws Exception {
        testAttributesUpdatedMsg(tenantAdminUserId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testAttributesUpdatedMsg` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testAttributesUpdatedMsg(EntityId entityId) throws Exception {
        String attributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key1\":\"value1\"}}";
        JsonNode attributesEntityData = JacksonUtil.toJsonNode(attributesData);
        EdgeEvent edgeEvent1 = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_UPDATED, entityId.getId(), EdgeEventType.valueOf(entityId.getEntityType().name()), attributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent1).get();
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(entityId.getId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(entityId.getId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(entityId.getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());

        TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();
        Assert.assertEquals(1, attributesUpdatedMsg.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = attributesUpdatedMsg.getKv(0);
        Assert.assertEquals("key1", keyValueProto.getKey());
        Assert.assertEquals("value1", keyValueProto.getStringV());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSendAttributesDeleteRequestToCloud_nonDeviceEntity` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSendAttributesDeleteRequestToCloud_nonDeviceEntity() throws Exception {
        edgeImitator.expectMessageAmount(2);
        Asset savedAsset = saveAsset("Delete Attribute Test");
        doPost("/api/edge/" + edge.getUuidId() + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        final String attributeKey = "key1";
        ObjectNode attributesData = JacksonUtil.newObjectNode();
        attributesData.put(attributeKey, "value1");
        doPost("/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributesData);

        // Wait before device attributes saved to database before deleting them
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/keys/attributes/" + DataConstants.SERVER_SCOPE;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && !actualKeys.isEmpty() && actualKeys.contains(attributeKey);
                });

        EntityDataProto.Builder builder = EntityDataProto.newBuilder()
                .setEntityIdMSB(savedAsset.getUuidId().getMostSignificantBits())
                .setEntityIdLSB(savedAsset.getUuidId().getLeastSignificantBits())
                .setEntityType(savedAsset.getId().getEntityType().name());
        AttributeDeleteMsg.Builder attributeDeleteMsg = AttributeDeleteMsg.newBuilder();
        attributeDeleteMsg.setScope(DataConstants.SERVER_SCOPE);
        attributeDeleteMsg.addAllAttributeNames(List.of(attributeKey));
        attributeDeleteMsg.build();
        builder.setAttributeDeleteMsg(attributeDeleteMsg);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addEntityData(builder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/keys/attributes/" + DataConstants.SERVER_SCOPE;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && actualKeys.isEmpty();
                });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TelemetryEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
