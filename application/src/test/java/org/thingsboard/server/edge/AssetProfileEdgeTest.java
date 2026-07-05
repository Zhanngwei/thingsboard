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

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`AssetProfileEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@DaoSqlTest
public class AssetProfileEdgeTest extends AbstractEdgeTest {

    /**
     * 功能：验证资产相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssetProfiles() throws Exception {
        RuleChainId buildingsRuleChainId = createEdgeRuleChainAndAssignToEdge("Buildings Rule Chain");

        // create asset profile
        AssetProfile assetProfile = this.createAssetProfile("Building");
        assetProfile.setDefaultEdgeRuleChainId(buildingsRuleChainId);
        edgeImitator.expectMessageAmount(1);
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        AssetProfileUpdateMsg assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        AssetProfile assetProfileMsg = JacksonUtil.fromString(assetProfileUpdateMsg.getEntity(), AssetProfile.class, true);
        Assert.assertNotNull(assetProfileMsg);
        Assert.assertEquals(assetProfile, assetProfileMsg);
        Assert.assertEquals("Building", assetProfileMsg.getName());
        Assert.assertEquals(buildingsRuleChainId, assetProfileMsg.getDefaultEdgeRuleChainId());
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());

        // update asset profile
        assetProfile.setImage("IMAGE");
        edgeImitator.expectMessageAmount(1);
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        assetProfileMsg = JacksonUtil.fromString(assetProfileUpdateMsg.getEntity(), AssetProfile.class, true);
        Assert.assertNotNull(assetProfileMsg);
        Assert.assertEquals("IMAGE", assetProfileMsg.getImage());
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/assetProfile/" + assetProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(assetProfile.getUuidId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(assetProfile.getUuidId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        unAssignFromEdgeAndDeleteRuleChain(buildingsRuleChainId);
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendAssetProfileToCloud() throws Exception {
        RuleChainId edgeRuleChainId = createEdgeRuleChainAndAssignToEdge("Asset Profile Rule Chain");
        DashboardId dashboardId = createDashboardAndAssignToEdge("Asset Profile Dashboard");

        AssetProfile assetProfileOnEdge = buildAssetProfileForUplinkMsg("Asset Profile On Edge");
        assetProfileOnEdge.setDefaultRuleChainId(edgeRuleChainId);
        assetProfileOnEdge.setDefaultDashboardId(dashboardId);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AssetProfileUpdateMsg.Builder assetProfileUpdateMsgBuilder = AssetProfileUpdateMsg.newBuilder();
        assetProfileUpdateMsgBuilder.setIdMSB(assetProfileOnEdge.getUuidId().getMostSignificantBits());
        assetProfileUpdateMsgBuilder.setIdLSB(assetProfileOnEdge.getUuidId().getLeastSignificantBits());
        assetProfileUpdateMsgBuilder.setEntity(JacksonUtil.toString(assetProfileOnEdge));
        assetProfileUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(assetProfileUpdateMsgBuilder);
        uplinkMsgBuilder.addAssetProfileUpdateMsg(assetProfileUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        AssetProfile assetProfile = doGet("/api/assetProfile/" + assetProfileOnEdge.getUuidId(), AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertEquals("Asset Profile On Edge", assetProfile.getName());
        Assert.assertEquals(dashboardId, assetProfile.getDefaultDashboardId());
        Assert.assertNull(assetProfile.getDefaultRuleChainId());
        Assert.assertEquals(edgeRuleChainId, assetProfile.getDefaultEdgeRuleChainId());

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/assetProfile/" + assetProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        AssetProfileUpdateMsg assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(assetProfile.getUuidId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(assetProfile.getUuidId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        // cleanup
        unAssignFromEdgeAndDeleteDashboard(dashboardId);
        unAssignFromEdgeAndDeleteRuleChain(edgeRuleChainId);
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendAssetProfileToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        String assetProfileOnCloudName = StringUtils.randomAlphanumeric(15);

        edgeImitator.expectMessageAmount(1);
        AssetProfile assetProfileOnCloud = this.createAssetProfile(assetProfileOnCloudName);
        assetProfileOnCloud = doPost("/api/assetProfile", assetProfileOnCloud, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AssetProfile assetProfileOnEdge = buildAssetProfileForUplinkMsg(assetProfileOnCloudName);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AssetProfileUpdateMsg.Builder assetProfileUpdateMsgBuilder = AssetProfileUpdateMsg.newBuilder();
        assetProfileUpdateMsgBuilder.setEntity(JacksonUtil.toString(assetProfileOnEdge));
        assetProfileUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addAssetProfileUpdateMsg(assetProfileUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<AssetProfileUpdateMsg> assetProfileUpdateMsgOpt = edgeImitator.findMessageByType(AssetProfileUpdateMsg.class);
        Assert.assertTrue(assetProfileUpdateMsgOpt.isPresent());
        AssetProfileUpdateMsg latestAssetProfileUpdateMsg = assetProfileUpdateMsgOpt.get();
        AssetProfile assetProfileMsg = JacksonUtil.fromString(latestAssetProfileUpdateMsg.getEntity(), AssetProfile.class, true);
        Assert.assertNotNull(assetProfileMsg);
        Assert.assertNotEquals(assetProfileOnCloudName, assetProfileMsg.getName());

        Assert.assertNotEquals(assetProfileOnCloud.getUuidId(), assetProfileOnEdge.getUuidId());

        AssetProfile assetProfile = doGet("/api/assetProfile/" + assetProfileMsg.getUuidId(), AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertNotEquals(assetProfileOnCloudName, assetProfile.getName());
    }

    /**
     * 功能：构建资产配置。
     * 参数：
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    private AssetProfile buildAssetProfileForUplinkMsg(String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setId(new AssetProfileId(UUID.randomUUID()));
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(name);
        assetProfile.setDefault(false);
        return assetProfile;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AssetProfileEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
