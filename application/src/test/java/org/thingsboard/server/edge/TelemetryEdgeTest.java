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

/**
 * 中文说明：
 * 1. `TelemetryEdgeTest` 是 ThingsBoard Application 中验证 `TelemetryEdge` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractEdgeTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class TelemetryEdgeTest extends AbstractEdgeTest {

    /**
     * 功能：验证时序数据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testTimeseriesWithFailures() throws Exception {
        int numberOfTimeseriesToSend = 333;

        Device device = findDeviceByName("Edge Device 1");

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
        // imitator will generate failure in 5% of cases
        edgeImitator.setFailureProbability(5.0);
        edgeImitator.expectMessageAmount(numberOfTimeseriesToSend);
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

        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            Assert.assertTrue(isIdxExistsInTheDownlinkList(idx, allTelemetryMsgs));
        }

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(false);
    }

    /**
     * 功能：验证`Attributes`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAttributes() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        testAttributesUpdatedMsg(device.getId());
        testPostAttributesMsg(device);
        testAttributesDeleteMsg(device);
    }

    /**
     * 功能：验证消息相关场景。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：无。
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

        TransportProtos.PostAttributeMsg postAttributesMsg = latestEntityDataMsg.getPostAttributesMsg();
        Assert.assertEquals(1, postAttributesMsg.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = postAttributesMsg.getKv(0);
        Assert.assertEquals("key2", keyValueProto.getKey());
        Assert.assertEquals("value2", keyValueProto.getStringV());
    }

    /**
     * 功能：验证消息相关场景。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：无。
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

    /**
     * 功能：验证时序数据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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

        TransportProtos.PostTelemetryMsg postTelemetryMsg = latestEntityDataMsg.getPostTelemetryMsg();
        Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
        TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
        Assert.assertEquals(timeseriesEntityData.get("ts").asLong(), tsKvListProto.getTs());
        Assert.assertEquals(1, tsKvListProto.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
        Assert.assertEquals("temperature", keyValueProto.getKey());
        Assert.assertEquals(25, keyValueProto.getLongV());
    }

    /**
     * 功能：判断`Idx Exists In The Downlink List`。
     * 参数：
     * - `idx`：`idx` 参数。
     * - `allTelemetryMsgs`：待处理消息。
     * 返回：判断结果。
     */
    private boolean isIdxExistsInTheDownlinkList(int idx, List<EntityDataProto> allTelemetryMsgs) {
        for (EntityDataProto proto : allTelemetryMsgs) {
            TransportProtos.PostTelemetryMsg postTelemetryMsg = proto.getPostTelemetryMsg();
            Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
            TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
            Assert.assertEquals(1, tsKvListProto.getKvCount());
            TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
            Assert.assertEquals("idx", keyValueProto.getKey());
            if (keyValueProto.getLongV() == idx) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAttributesUpdatedMsg_userEntity() throws Exception {
        testAttributesUpdatedMsg(tenantAdminUserId);
    }

    /**
     * 功能：验证消息相关场景。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
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

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
