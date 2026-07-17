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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainConnectionInfoProto;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleNodeProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `RuleChainMsgConstructorTest` 是 ThingsBoard Application 中验证 `RuleChainMsgConstructor` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class RuleChainMsgConstructorTest {

    /**
     * RPC常量，用于统一引用固定值。
     */
    private static final String RPC_CONNECTION_TYPE = "RPC";

    /**
     * 规则链，承载当前步骤需要处理的内容。
     */
    private RuleChainMsgConstructorV1 ruleChainMsgConstructorV1;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setup() {
        ruleChainMsgConstructorV1 = new RuleChainMsgConstructorV1();
        tenantId = new TenantId(UUID.randomUUID());
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_4_0() {
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData = createRuleChainMetaData(
                ruleChainId, 3, createRuleNodes(ruleChainId), createConnections());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                        ruleChainMetaData,
                        EdgeVersion.V_3_4_0);

        assetV_3_3_3_and_V_3_4_0(ruleChainMetadataUpdateMsg);

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_3_3() {
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData = createRuleChainMetaData(
                ruleChainId, 3, createRuleNodes(ruleChainId), createConnections());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                        ruleChainMetaData,
                        EdgeVersion.V_3_3_3);

        assetV_3_3_3_and_V_3_4_0(ruleChainMetadataUpdateMsg);

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    /**
     * 功能：执行 `assetV_3_3_3_and_V_3_4_0` 对应的处理。
     * 参数：
     * - `ruleChainMetadataUpdateMsg`：待处理消息。
     * 返回：无。
     */
    private void assetV_3_3_3_and_V_3_4_0(RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        Assert.assertEquals("First rule node index incorrect!", 3, ruleChainMetadataUpdateMsg.getFirstNodeIndex());
        Assert.assertEquals("Nodes count incorrect!", 12, ruleChainMetadataUpdateMsg.getNodesCount());
        Assert.assertEquals("Connections count incorrect!", 13, ruleChainMetadataUpdateMsg.getConnectionsCount());
        Assert.assertEquals("Rule chain connections count incorrect!", 0, ruleChainMetadataUpdateMsg.getRuleChainConnectionsCount());

        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 6, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(0));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 10, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(1));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(2));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 11, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(3));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 11, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(4));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 11, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(5));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 7, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(6));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 4, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(7));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 5, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(8));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 8, TbNodeConnectionType.OTHER), ruleChainMetadataUpdateMsg.getConnections(9));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 9, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(10));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(7, 11, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(11));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(10, 9, RPC_CONNECTION_TYPE), ruleChainMetadataUpdateMsg.getConnections(12));
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_3_0() {
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData = createRuleChainMetaData(ruleChainId, 3, createRuleNodes(ruleChainId), createConnections());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                        ruleChainMetaData,
                        EdgeVersion.V_3_3_0);

        Assert.assertEquals("First rule node index incorrect!", 2, ruleChainMetadataUpdateMsg.getFirstNodeIndex());
        Assert.assertEquals("Nodes count incorrect!", 10, ruleChainMetadataUpdateMsg.getNodesCount());
        Assert.assertEquals("Connections count incorrect!", 10, ruleChainMetadataUpdateMsg.getConnectionsCount());
        Assert.assertEquals("Rule chain connections count incorrect!", 1, ruleChainMetadataUpdateMsg.getRuleChainConnectionsCount());

        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(2, 5, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(0));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 9, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(1));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 9, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(2));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 9, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(3));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 6, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(4));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 3, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(5));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 4, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(6));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 7, TbNodeConnectionType.OTHER), ruleChainMetadataUpdateMsg.getConnections(7));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 8, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(8));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 9, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(9));

        RuleChainConnectionInfoProto ruleChainConnection = ruleChainMetadataUpdateMsg.getRuleChainConnections(0);
        Assert.assertEquals("From index incorrect!", 2, ruleChainConnection.getFromIndex());
        Assert.assertEquals("Type index incorrect!", TbNodeConnectionType.SUCCESS, ruleChainConnection.getType());
        Assert.assertEquals("Additional info incorrect!",
                "{\"description\":\"\",\"layoutX\":477,\"layoutY\":560,\"ruleChainNodeId\":\"rule-chain-node-UNDEFINED\"}",
                ruleChainConnection.getAdditionalInfo());
        Assert.assertTrue("Target rule chain id MSB incorrect!", ruleChainConnection.getTargetRuleChainIdMSB() != 0);
        Assert.assertTrue("Target rule chain id LSB incorrect!", ruleChainConnection.getTargetRuleChainIdLSB() != 0);

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_3_0_inDifferentOrder() {
        // same rule chain metadata, but different order of rule nodes
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData1 = createRuleChainMetaData(ruleChainId, 8, createRuleNodesInDifferentOrder(ruleChainId), createConnectionsInDifferentOrder());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, 
                        ruleChainMetaData1, 
                        EdgeVersion.V_3_3_0);

        Assert.assertEquals("First rule node index incorrect!", 7, ruleChainMetadataUpdateMsg.getFirstNodeIndex());
        Assert.assertEquals("Nodes count incorrect!", 10, ruleChainMetadataUpdateMsg.getNodesCount());
        Assert.assertEquals("Connections count incorrect!", 10, ruleChainMetadataUpdateMsg.getConnectionsCount());
        Assert.assertEquals("Rule chain connections count incorrect!", 1, ruleChainMetadataUpdateMsg.getRuleChainConnectionsCount());

        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(0));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 0, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(1));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 3, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(2));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 6, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(3));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 5, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(4));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 2, TbNodeConnectionType.OTHER), ruleChainMetadataUpdateMsg.getConnections(5));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 1, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(6));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(7));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(8));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(7, 4, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(9));

        RuleChainConnectionInfoProto ruleChainConnection = ruleChainMetadataUpdateMsg.getRuleChainConnections(0);
        Assert.assertEquals("From index incorrect!", 7, ruleChainConnection.getFromIndex());
        Assert.assertEquals("Type index incorrect!", TbNodeConnectionType.SUCCESS, ruleChainConnection.getType());
        Assert.assertEquals("Additional info incorrect!",
                "{\"description\":\"\",\"layoutX\":477,\"layoutY\":560,\"ruleChainNodeId\":\"rule-chain-node-UNDEFINED\"}",
                ruleChainConnection.getAdditionalInfo());
        Assert.assertTrue("Target rule chain id MSB incorrect!", ruleChainConnection.getTargetRuleChainIdMSB() != 0);
        Assert.assertTrue("Target rule chain id LSB incorrect!", ruleChainConnection.getTargetRuleChainIdLSB() != 0);

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    /**
     * 功能：执行 `assertCheckpointRuleNodeConfiguration` 对应的处理。
     * 参数：
     * - `nodesList`：数据列表。
     * - `expectedConfiguration`：配置对象。
     * 返回：无。
     */
    private void assertCheckpointRuleNodeConfiguration(List<RuleNodeProto> nodesList,
                                                       String expectedConfiguration) {
        Optional<RuleNodeProto> checkpointRuleNodeOpt = nodesList.stream()
                .filter(rn -> "org.thingsboard.rule.engine.flow.TbCheckpointNode".equals(rn.getType()))
                .findFirst();
        Assert.assertTrue(checkpointRuleNodeOpt.isPresent());
        RuleNodeProto checkpointRuleNode = checkpointRuleNodeOpt.get();
        Assert.assertEquals(expectedConfiguration, checkpointRuleNode.getConfiguration());
    }

    /**
     * 功能：执行 `compareNodeConnectionInfoAndProto` 对应的处理。
     * 参数：
     * - `expected`：`expected` 参数。
     * - `actual`：`actual` 参数。
     * 返回：无。
     */
    private void compareNodeConnectionInfoAndProto(NodeConnectionInfo expected, org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto actual) {
        Assert.assertEquals(expected.getFromIndex(), actual.getFromIndex());
        Assert.assertEquals(expected.getToIndex(), actual.getToIndex());
        Assert.assertEquals(expected.getType(), actual.getType());
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `firstNodeIndex`：`firstNodeIndex` 参数。
     * - `nodes`：数据列表。
     * - `connections`：数据列表。
     * 返回：处理结果。
     */
    private RuleChainMetaData createRuleChainMetaData(RuleChainId ruleChainId, Integer firstNodeIndex, List<RuleNode> nodes, List<NodeConnectionInfo> connections) {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);
        ruleChainMetaData.setFirstNodeIndex(firstNodeIndex);
        ruleChainMetaData.setNodes(nodes);
        ruleChainMetaData.setConnections(connections);
        ruleChainMetaData.setRuleChainConnections(null);
        return ruleChainMetaData;
    }

    /**
     * 功能：保存或创建`Connections`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private List<NodeConnectionInfo> createConnections() {
        List<NodeConnectionInfo> result = new ArrayList<>();
        result.add(createNodeConnectionInfo(3, 6, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(3, 10, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(3, 0, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(4, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(5, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(6, 11, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 7, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 4, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 5, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 8, TbNodeConnectionType.OTHER));
        result.add(createNodeConnectionInfo(6, 9, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(7, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(10, 9, RPC_CONNECTION_TYPE));
        return result;
    }

    /**
     * 功能：保存或创建节点实例。
     * 参数：
     * - `fromIndex`：`fromIndex` 参数。
     * - `toIndex`：`toIndex` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    private NodeConnectionInfo createNodeConnectionInfo(int fromIndex, int toIndex, String type) {
        NodeConnectionInfo result = new NodeConnectionInfo();
        result.setFromIndex(fromIndex);
        result.setToIndex(toIndex);
        result.setType(type);
        return result;
    }

    /**
     * 功能：保存或创建`Rule Nodes`。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    private List<RuleNode> createRuleNodes(RuleChainId ruleChainId) {
        List<RuleNode> result = new ArrayList<>();
        result.add(getOutputNode(ruleChainId));
        result.add(getAcknowledgeNode(ruleChainId));
        result.add(getCheckpointNode(ruleChainId));
        result.add(getDeviceProfileNode(ruleChainId));
        result.add(getSaveTimeSeriesNode(ruleChainId));
        result.add(getSaveClientAttributesNode(ruleChainId));
        result.add(getMessageTypeSwitchNode(ruleChainId));
        result.add(getLogRpcFromDeviceNode(ruleChainId));
        result.add(getLogOtherNode(ruleChainId));
        result.add(getRpcCallRequestNode(ruleChainId));
        result.add(getPushToAnalyticsNode(ruleChainId));
        result.add(getPushToCloudNode(ruleChainId));
        return result;
    }

    /**
     * 功能：保存或创建规则节点。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `type`：类型。
     * - `name`：名称。
     * - `configuration`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private RuleNode createRuleNode(RuleChainId ruleChainId, String type, String name, JsonNode configuration, JsonNode additionalInfo) {
        RuleNode e = new RuleNode();
        e.setRuleChainId(ruleChainId);
        e.setType(type);
        e.setName(name);
        e.setDebugMode(false);
        e.setConfiguration(configuration);
        e.setAdditionalInfo(additionalInfo);
        e.setId(new RuleNodeId(UUID.randomUUID()));
        return e;
    }

    /**
     * 功能：保存或创建`Connections In Different Order`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private List<NodeConnectionInfo> createConnectionsInDifferentOrder() {
        List<NodeConnectionInfo> result = new ArrayList<>();
        result.add(createNodeConnectionInfo(0, 2, RPC_CONNECTION_TYPE));
        result.add(createNodeConnectionInfo(4, 1, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(5, 1, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 4, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 7, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 6, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 3, TbNodeConnectionType.OTHER));
        result.add(createNodeConnectionInfo(5, 2, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 1, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(7, 1, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(8, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(8, 5, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(8, 0, TbNodeConnectionType.SUCCESS));
        return result;
    }

    /**
     * 功能：保存或创建`Rule Nodes In Different Order`。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    private List<RuleNode> createRuleNodesInDifferentOrder(RuleChainId ruleChainId) {
        List<RuleNode> result = new ArrayList<>();
        result.add(getPushToAnalyticsNode(ruleChainId));
        result.add(getPushToCloudNode(ruleChainId));
        result.add(getRpcCallRequestNode(ruleChainId));
        result.add(getLogOtherNode(ruleChainId));
        result.add(getLogRpcFromDeviceNode(ruleChainId));
        result.add(getMessageTypeSwitchNode(ruleChainId));
        result.add(getSaveClientAttributesNode(ruleChainId));
        result.add(getSaveTimeSeriesNode(ruleChainId));
        result.add(getDeviceProfileNode(ruleChainId));
        result.add(getCheckpointNode(ruleChainId));
        result.add(getAcknowledgeNode(ruleChainId));
        result.add(getOutputNode(ruleChainId));
        return result;
    }


    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getOutputNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbRuleChainOutputNode",
                "Output node",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":178,\"layoutY\":592}"));
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getCheckpointNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbCheckpointNode",
                "Checkpoint node",
                JacksonUtil.toJsonNode("{\"queueName\":\"HighPriority\"}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":178,\"layoutY\":647}"));
    }

    /**
     * 功能：获取时序数据。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getSaveTimeSeriesNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode",
                "Save Timeseries",
                JacksonUtil.toJsonNode("{\"defaultTTL\":0}"),
                JacksonUtil.toJsonNode("{\"layoutX\":823,\"layoutY\":157}"));
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getMessageTypeSwitchNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode",
                "Message Type Switch",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"layoutX\":347,\"layoutY\":149}"));
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getLogOtherNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.action.TbLogNode",
                "Log Other",
                JacksonUtil.toJsonNode("{\"jsScript\":\"return '\\\\nIncoming message:\\\\n' + JSON.stringify(msg) + '\\\\nIncoming metadata:\\\\n' + JSON.stringify(metadata);\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":378}"));
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getPushToCloudNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.edge.TbMsgPushToCloudNode",
                "Push to cloud",
                JacksonUtil.toJsonNode("{\"scope\":\"SERVER_SCOPE\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":1129,\"layoutY\":52}"));
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getAcknowledgeNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbAckNode",
                "Acknowledge node",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":177,\"layoutY\":703}"));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getDeviceProfileNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.profile.TbDeviceProfileNode",
                "Device Profile Node",
                JacksonUtil.toJsonNode("{\"persistAlarmRulesState\":false,\"fetchAlarmRulesStateOnStart\":false}"),
                JacksonUtil.toJsonNode("{\"description\":\"Process incoming messages from devices with the alarm rules defined in the device profile. Dispatch all incoming messages with \\\"Success\\\" relation type.\",\"layoutX\":187,\"layoutY\":468}"));
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getSaveClientAttributesNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode",
                "Save Client Attributes",
                JacksonUtil.toJsonNode("{\"scope\":\"CLIENT_SCOPE\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":52}"));
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getLogRpcFromDeviceNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.action.TbLogNode",
                "Log RPC from Device",
                JacksonUtil.toJsonNode("{\"jsScript\":\"return '\\\\nIncoming message:\\\\n' + JSON.stringify(msg) + '\\\\nIncoming metadata:\\\\n' + JSON.stringify(metadata);\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":825,\"layoutY\":266}"));
    }

    /**
     * 功能：获取RPC。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getRpcCallRequestNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode",
                "RPC Call Request",
                JacksonUtil.toJsonNode("{\"timeoutInSeconds\":60}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":466}"));
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    private RuleNode getPushToAnalyticsNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbRuleChainInputNode",
                "Push to Analytics",
                JacksonUtil.toJsonNode("{\"ruleChainId\":\"af588000-6c7c-11ec-bafd-c9a47a5c8d99\"}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":477,\"layoutY\":560}"));
    }
}
