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
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. `RuleChainEdgeTest` 是 ThingsBoard Application 中验证 `RuleChainEdge` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractEdgeTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class RuleChainEdgeTest extends AbstractEdgeTest {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final int CONFIGURATION_VERSION = 5;

    /**
     * 功能：验证`Rule Chains`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRuleChains() throws Exception {
        // create rule chain
        edgeImitator.expectMessageAmount(2);
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        createRuleChainMetadata(savedRuleChain);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        RuleChain ruleChainMsg = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertTrue(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()));
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());
        Assert.assertEquals(savedRuleChain.getName(), ruleChainMsg.getName());

        testRuleChainMetadataRequestMsg(savedRuleChain.getId());

        // unassign rule chain from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), savedRuleChain.getUuidId().getMostSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), savedRuleChain.getUuidId().getLeastSignificantBits());

        // delete rule chain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendRuleChainMetadataRequestToCloud() throws Exception {
        RuleChainId edgeRootRuleChainId = edge.getRootRuleChainId();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RuleChainMetadataRequestMsg.Builder ruleChainMetadataRequestMsgBuilder = RuleChainMetadataRequestMsg.newBuilder();
        ruleChainMetadataRequestMsgBuilder.setRuleChainIdMSB(edgeRootRuleChainId.getId().getMostSignificantBits());
        ruleChainMetadataRequestMsgBuilder.setRuleChainIdLSB(edgeRootRuleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(ruleChainMetadataRequestMsgBuilder);
        uplinkMsgBuilder.addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
        RuleChainMetaData ruleChainMetadataMsg = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        Assert.assertNotNull(ruleChainMetadataMsg);
        Assert.assertEquals(edgeRootRuleChainId, ruleChainMetadataMsg.getRuleChainId());

        testAutoGeneratedCodeByProtobuf(ruleChainMetadataUpdateMsg);
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    private void testRuleChainMetadataRequestMsg(RuleChainId ruleChainId) throws Exception {
        RuleChainMetadataRequestMsg.Builder ruleChainMetadataRequestMsgBuilder = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(ruleChainMetadataRequestMsgBuilder);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
        RuleChainMetaData ruleChainMetadataMsg = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        Assert.assertNotNull(ruleChainMetadataMsg);
        Assert.assertEquals(ruleChainId, ruleChainMetadataMsg.getRuleChainId());

        for (RuleNode ruleNode : ruleChainMetadataMsg.getNodes()) {
            Assert.assertEquals(CONFIGURATION_VERSION, ruleNode.getConfigurationVersion());
        }
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：处理结果。
     */
    private RuleChainMetaData createRuleChainMetadata(RuleChain ruleChain) {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(CONFIGURATION_VERSION);
        TbGetAttributesNodeConfiguration configuration = new TbGetAttributesNodeConfiguration();
        configuration.setFetchTo(TbMsgSource.METADATA);
        configuration.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(CONFIGURATION_VERSION);
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode3.setConfigurationVersion(CONFIGURATION_VERSION);
        ruleNode3.setConfiguration(JacksonUtil.valueToTree(configuration));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0, 1, "success");
        ruleChainMetaData.addConnectionInfo(0, 2, "fail");
        ruleChainMetaData.addConnectionInfo(1, 2, "success");

        return doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSetRootRuleChain() throws Exception {
        // create rule chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge New Root Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        edgeImitator.expectMessageAmount(2);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        RuleChainMetaData metaData = createRuleChainMetadata(savedRuleChain);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // set new rule chain as root
        RuleChainId currentRootRuleChainId = edge.getRootRuleChainId();
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/" + savedRuleChain.getUuidId() + "/root", Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        RuleChain ruleChainMsg = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertTrue(ruleChainMsg.isRoot());
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());

        // update metadata for root rule chain
        edgeImitator.expectMessageAmount(1);
        metaData.getNodes().forEach(n -> n.setDebugMode(true));
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        ruleChainMsg = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertTrue(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()));
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());
        Assert.assertEquals(savedRuleChain.getName(), ruleChainMsg.getName());
        Assert.assertTrue(ruleChainMsg.isRoot());

        // revert root rule chain
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/" + currentRootRuleChainId.getId() + "/root", Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // unassign rule chain from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // delete rule chain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));
    }
}
