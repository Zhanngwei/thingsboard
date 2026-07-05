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

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`RelationEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@DaoSqlTest
public class RelationEdgeTest extends AbstractEdgeTest {


    /**
     * 功能：验证`Relations`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRelations() throws Exception {
        // create relation
        Device device = findDeviceByName("Edge Device 1");
        Asset asset = findAssetByName("Edge Asset 1");
        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        edgeImitator.expectMessageAmount(1);
        doPost("/api/relation", relation);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        RelationUpdateMsg relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        EntityRelation entityRelation = JacksonUtil.fromString(relationUpdateMsg.getEntity(), EntityRelation.class, true);
        Assert.assertNotNull(entityRelation);
        Assert.assertEquals(relation, entityRelation);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, relationUpdateMsg.getMsgType());

        // delete relation
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/relation?" +
                "fromId=" + relation.getFrom().getId().toString() +
                "&fromType=" + relation.getFrom().getEntityType().name() +
                "&relationType=" + relation.getType() +
                "&relationTypeGroup=" + relation.getTypeGroup().name() +
                "&toId=" + relation.getTo().getId().toString() +
                "&toType=" + relation.getTo().getEntityType().name())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        entityRelation = JacksonUtil.fromString(relationUpdateMsg.getEntity(), EntityRelation.class, true);
        Assert.assertNotNull(entityRelation);
        Assert.assertEquals(relation, entityRelation);
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendRelationToCloud() throws Exception {
        Device device1 = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        Device device2 = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        EntityRelation entityRelation = buildEntityRelationForUplinkMsg(device1.getId(), device2.getId());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RelationUpdateMsg.Builder relationUpdateMsgBuilder = RelationUpdateMsg.newBuilder();
        relationUpdateMsgBuilder.setEntity(JacksonUtil.toString(entityRelation));
        testAutoGeneratedCodeByProtobuf(relationUpdateMsgBuilder);
        uplinkMsgBuilder.addRelationUpdateMsg(relationUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        EntityRelation relation = doGet("/api/relation?" +
                "&fromId=" + device2.getUuidId() +
                "&fromType=" + device2.getId().getEntityType().name() +
                "&relationType=" + "test" +
                "&relationTypeGroup=" + RelationTypeGroup.COMMON.name() +
                "&toId=" + device1.getUuidId() +
                "&toType=" + device1.getId().getEntityType().name(), EntityRelation.class);
        Assert.assertNotNull(relation);
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendRelationRequestToCloud() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        Asset asset = findAssetByName("Edge Asset 1");

        EntityRelation deviceToAssetRelation = new EntityRelation();
        deviceToAssetRelation.setType("test");
        deviceToAssetRelation.setFrom(device.getId());
        deviceToAssetRelation.setTo(asset.getId());
        deviceToAssetRelation.setTypeGroup(RelationTypeGroup.COMMON);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/relation", deviceToAssetRelation);
        Assert.assertTrue(edgeImitator.waitForMessages());

        EntityRelation assetToTenantRelation = new EntityRelation();
        assetToTenantRelation.setType("test");
        assetToTenantRelation.setFrom(asset.getId());
        assetToTenantRelation.setTo(tenantId);
        assetToTenantRelation.setTypeGroup(RelationTypeGroup.COMMON);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/relation", assetToTenantRelation);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RelationRequestMsg.Builder relationRequestMsgBuilder = RelationRequestMsg.newBuilder();
        relationRequestMsgBuilder.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        relationRequestMsgBuilder.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        relationRequestMsgBuilder.setEntityType(device.getId().getEntityType().name());
        testAutoGeneratedCodeByProtobuf(relationRequestMsgBuilder);

        uplinkMsgBuilder.addRelationRequestMsg(relationRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        RelationUpdateMsg relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        EntityRelation entityRelation = JacksonUtil.fromString(relationUpdateMsg.getEntity(), EntityRelation.class, true);
        Assert.assertNotNull(entityRelation);
        Assert.assertEquals(deviceToAssetRelation, entityRelation);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
    }

    /**
     * 功能：构建实体。
     * 参数：
     * - `deviceId1`：设备信息或设备标识。
     * - `deviceId2`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private EntityRelation buildEntityRelationForUplinkMsg(DeviceId deviceId1, DeviceId deviceId2) {
        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setTo(deviceId1);
        relation.setFrom(deviceId2);
        relation.setAdditionalInfo(TextNode.valueOf("{}"));
        return relation;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RelationEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
