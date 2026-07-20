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
 * 1. `AssetProfileEdgeTest` 是 ThingsBoard Application 中验证 `AssetProfileEdge` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractEdgeTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
