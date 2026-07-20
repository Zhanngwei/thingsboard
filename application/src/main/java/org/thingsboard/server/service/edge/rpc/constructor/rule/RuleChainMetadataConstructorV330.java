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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNode;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNodeConfiguration;
import org.thingsboard.rule.engine.flow.TbRuleChainOutputNode;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `RuleChainMetadataConstructorV330` 是 ThingsBoard Application 中创建或提供规则链对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseRuleChainMetadataConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Slf4j
public class RuleChainMetadataConstructorV330 extends BaseRuleChainMetadataConstructor {

    private static final String RULE_CHAIN_INPUT_NODE = TbRuleChainInputNode.class.getName();
    private static final String TB_RULE_CHAIN_OUTPUT_NODE = TbRuleChainOutputNode.class.getName();

    /**
     * 功能：执行 `constructRuleChainMetadataUpdatedMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `builder`：`builder` 参数。
     * - `ruleChainMetaData`：待处理数据。
     * 返回：无。
     */
    @Override
    protected void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                        RuleChainMetadataUpdateMsg.Builder builder,
                                                        RuleChainMetaData ruleChainMetaData) {
        builder.setRuleChainIdMSB(ruleChainMetaData.getRuleChainId().getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainMetaData.getRuleChainId().getId().getLeastSignificantBits());
        List<RuleNode> supportedNodes = filterNodes(ruleChainMetaData.getNodes());

        NavigableSet<Integer> removedNodeIndexes = getRemovedNodeIndexes(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections());
        List<NodeConnectionInfo> connections = filterConnections(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections(), removedNodeIndexes);

        List<RuleChainConnectionInfo> ruleChainConnections = new ArrayList<>();
        if (ruleChainMetaData.getRuleChainConnections() != null) {
            ruleChainConnections.addAll(ruleChainMetaData.getRuleChainConnections());
        }
        ruleChainConnections.addAll(addRuleChainConnections(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections()));
        builder.addAllNodes(constructNodes(supportedNodes))
                .addAllConnections(constructConnections(connections))
                .addAllRuleChainConnections(constructRuleChainConnections(ruleChainConnections, removedNodeIndexes));
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            Integer firstNodeIndex = ruleChainMetaData.getFirstNodeIndex();
            // decrease index because of removed nodes
            for (Integer removedIndex : removedNodeIndexes) {
                if (firstNodeIndex > removedIndex) {
                    firstNodeIndex = firstNodeIndex - 1;
                }
            }
            builder.setFirstNodeIndex(firstNodeIndex);
        } else {
            builder.setFirstNodeIndex(-1);
        }
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `nodes`：数据列表。
     * - `connections`：数据列表。
     * 返回：匹配的数据集合。
     */
    private NavigableSet<Integer> getRemovedNodeIndexes(List<RuleNode> nodes, List<NodeConnectionInfo> connections) {
        TreeSet<Integer> removedIndexes = new TreeSet<>();
        for (NodeConnectionInfo connection : connections) {
            for (int i = 0; i < nodes.size(); i++) {
                RuleNode node = nodes.get(i);
                if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)
                        || node.getType().equalsIgnoreCase(TB_RULE_CHAIN_OUTPUT_NODE)) {
                    if (connection.getFromIndex() == i || connection.getToIndex() == i) {
                        removedIndexes.add(i);
                    }
                }
            }
        }
        return removedIndexes.descendingSet();
    }

    /**
     * 功能：执行 `filterConnections` 对应的处理。
     * 参数：
     * - `nodes`：数据列表。
     * - `connections`：数据列表。
     * - `removedNodeIndexes`：`removedNodeIndexes` 参数。
     * 返回：匹配的数据集合。
     */
    private List<NodeConnectionInfo> filterConnections(List<RuleNode> nodes,
                                                       List<NodeConnectionInfo> connections,
                                                       NavigableSet<Integer> removedNodeIndexes) {
        List<NodeConnectionInfo> result = new ArrayList<>();
        if (connections != null) {
            result = connections.stream().filter(conn -> {
                for (int i = 0; i < nodes.size(); i++) {
                    RuleNode node = nodes.get(i);
                    if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)
                            || node.getType().equalsIgnoreCase(TB_RULE_CHAIN_OUTPUT_NODE)) {
                        if (conn.getFromIndex() == i || conn.getToIndex() == i) {
                            return false;
                        }
                    }
                }
                return true;
            }).map(conn -> {
                NodeConnectionInfo newConn = new NodeConnectionInfo();
                newConn.setFromIndex(conn.getFromIndex());
                newConn.setToIndex(conn.getToIndex());
                newConn.setType(conn.getType());
                return newConn;
            }).collect(Collectors.toList());
        }

        // decrease index because of removed nodes
        for (Integer removedIndex : removedNodeIndexes) {
            for (NodeConnectionInfo newConn : result) {
                if (newConn.getToIndex() > removedIndex) {
                    newConn.setToIndex(newConn.getToIndex() - 1);
                }
                if (newConn.getFromIndex() > removedIndex) {
                    newConn.setFromIndex(newConn.getFromIndex() - 1);
                }
            }
        }

        return result;
    }

    /**
     * 功能：执行 `filterNodes` 对应的处理。
     * 参数：
     * - `nodes`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<RuleNode> filterNodes(List<RuleNode> nodes) {
        List<RuleNode> result = new ArrayList<>();
        for (RuleNode node : nodes) {
            if (RULE_CHAIN_INPUT_NODE.equals(node.getType())
                    || TB_RULE_CHAIN_OUTPUT_NODE.equals(node.getType())) {
                log.trace("Skipping not supported rule node {}", node);
            } else {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `nodes`：数据列表。
     * - `connections`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<RuleChainConnectionInfo> addRuleChainConnections(List<RuleNode> nodes, List<NodeConnectionInfo> connections) {
        List<RuleChainConnectionInfo> result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            RuleNode node = nodes.get(i);
            if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)) {
                for (NodeConnectionInfo connection : connections) {
                    if (connection.getToIndex() == i) {
                        RuleChainConnectionInfo e = new RuleChainConnectionInfo();
                        e.setFromIndex(connection.getFromIndex());
                        TbRuleChainInputNodeConfiguration configuration = JacksonUtil.treeToValue(node.getConfiguration(), TbRuleChainInputNodeConfiguration.class);
                        e.setTargetRuleChainId(new RuleChainId(UUID.fromString(configuration.getRuleChainId())));
                        e.setAdditionalInfo(node.getAdditionalInfo());
                        e.setType(connection.getType());
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }
}
