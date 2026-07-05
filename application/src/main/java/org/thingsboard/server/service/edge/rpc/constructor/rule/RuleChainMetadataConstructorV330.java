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
 * 1. 类目的：`RuleChainMetadataConstructorV330` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
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

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainMetadataConstructorV330` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
