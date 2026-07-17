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
package org.thingsboard.server.service.edge.rpc.constructor.rule;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto;
import org.thingsboard.server.gen.edge.v1.RuleChainConnectionInfoProto;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleNodeProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

/**
 * 中文说明：
 * 1. `BaseRuleChainMetadataConstructor` 是 ThingsBoard Application 中创建或提供规则链对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `RuleChainMetadataConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Slf4j
@AllArgsConstructor
public abstract class BaseRuleChainMetadataConstructor implements RuleChainMetadataConstructor {

    /**
     * 功能：执行 `constructRuleChainMetadataUpdatedMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `msgType`：待处理消息。
     * - `ruleChainMetaData`：待处理数据。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChainMetadataUpdateMsg constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                                           UpdateMsgType msgType,
                                                                           RuleChainMetaData ruleChainMetaData,
                                                                           EdgeVersion edgeVersion) {
        RuleChainMetadataUpdateMsg.Builder builder = RuleChainMetadataUpdateMsg.newBuilder();
        constructRuleChainMetadataUpdatedMsg(tenantId, builder, ruleChainMetaData);
        builder.setMsgType(msgType);
        return builder.build();
    }

    /**
     * 功能：执行 `constructRuleChainMetadataUpdatedMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `builder`：`builder` 参数。
     * - `ruleChainMetaData`：待处理数据。
     * 返回：无。
     */
    protected abstract void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                                 RuleChainMetadataUpdateMsg.Builder builder,
                                                                 RuleChainMetaData ruleChainMetaData);

    /**
     * 功能：执行 `constructConnections` 对应的处理。
     * 参数：
     * - `connections`：数据列表。
     * 返回：匹配的数据集合。
     */
    protected List<NodeConnectionInfoProto> constructConnections(List<NodeConnectionInfo> connections) {
        List<NodeConnectionInfoProto> result = new ArrayList<>();
        if (connections != null && !connections.isEmpty()) {
            for (NodeConnectionInfo connection : connections) {
                result.add(constructConnection(connection));
            }
        }
        return result;
    }

    /**
     * 功能：执行 `constructConnection` 对应的处理。
     * 参数：
     * - `connection`：`connection` 参数。
     * 返回：处理结果。
     */
    private NodeConnectionInfoProto constructConnection(NodeConnectionInfo connection) {
        return NodeConnectionInfoProto.newBuilder()
                .setFromIndex(connection.getFromIndex())
                .setToIndex(connection.getToIndex())
                .setType(connection.getType())
                .build();
    }

    /**
     * 功能：执行 `constructNodes` 对应的处理。
     * 参数：
     * - `nodes`：数据列表。
     * 返回：匹配的数据集合。
     */
    protected List<RuleNodeProto> constructNodes(List<RuleNode> nodes) {
        List<RuleNodeProto> result = new ArrayList<>();
        if (nodes != null && !nodes.isEmpty()) {
            for (RuleNode node : nodes) {
                result.add(constructNode(node));
            }
        }
        return result;
    }

    /**
     * 功能：执行 `constructNode` 对应的处理。
     * 参数：
     * - `node`：`node` 参数。
     * 返回：处理结果。
     */
    private RuleNodeProto constructNode(RuleNode node) {
        return RuleNodeProto.newBuilder()
                .setIdMSB(node.getId().getId().getMostSignificantBits())
                .setIdLSB(node.getId().getId().getLeastSignificantBits())
                .setType(node.getType())
                .setName(node.getName())
                .setDebugMode(node.isDebugMode())
                .setConfiguration(JacksonUtil.toString(node.getConfiguration()))
                .setAdditionalInfo(JacksonUtil.toString(node.getAdditionalInfo()))
                .setSingletonMode(node.isSingletonMode())
                .setConfigurationVersion(node.getConfigurationVersion())
                .build();
    }

    /**
     * 功能：执行 `constructRuleChainConnections` 对应的处理。
     * 参数：
     * - `ruleChainConnections`：数据列表。
     * - `removedNodeIndexes`：`removedNodeIndexes` 参数。
     * 返回：匹配的数据集合。
     */
    protected List<RuleChainConnectionInfoProto> constructRuleChainConnections(List<RuleChainConnectionInfo> ruleChainConnections,
                                                                               NavigableSet<Integer> removedNodeIndexes) {
        List<RuleChainConnectionInfoProto> result = new ArrayList<>();
        if (ruleChainConnections != null && !ruleChainConnections.isEmpty()) {
            for (RuleChainConnectionInfo ruleChainConnectionInfo : ruleChainConnections) {
                if (!removedNodeIndexes.isEmpty()) { // 3_3_0 only
                    int fromIndex = ruleChainConnectionInfo.getFromIndex();
                    // decrease index because of removed nodes
                    for (Integer removedIndex : removedNodeIndexes) {
                        if (fromIndex > removedIndex) {
                            fromIndex = fromIndex - 1;
                        }
                    }
                    ruleChainConnectionInfo.setFromIndex(fromIndex);
                    ObjectNode additionalInfo = (ObjectNode) ruleChainConnectionInfo.getAdditionalInfo();
                    if (additionalInfo.get("ruleChainNodeId") == null) {
                        additionalInfo.put("ruleChainNodeId", "rule-chain-node-UNDEFINED");
                    }
                }
                result.add(constructRuleChainConnection(ruleChainConnectionInfo));
            }
        }
        return result;
    }

    /**
     * 功能：执行 `constructRuleChainConnection` 对应的处理。
     * 参数：
     * - `ruleChainConnectionInfo`：`ruleChainConnectionInfo` 参数。
     * 返回：处理结果。
     */
    private RuleChainConnectionInfoProto constructRuleChainConnection(RuleChainConnectionInfo ruleChainConnectionInfo) {
        return RuleChainConnectionInfoProto.newBuilder()
                .setFromIndex(ruleChainConnectionInfo.getFromIndex())
                .setTargetRuleChainIdMSB(ruleChainConnectionInfo.getTargetRuleChainId().getId().getMostSignificantBits())
                .setTargetRuleChainIdLSB(ruleChainConnectionInfo.getTargetRuleChainId().getId().getLeastSignificantBits())
                .setType(ruleChainConnectionInfo.getType())
                .setAdditionalInfo(JacksonUtil.toString(ruleChainConnectionInfo.getAdditionalInfo()))
                .build();
    }
}
