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
 * 1. 类目的：`BaseRuleChainMetadataConstructor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
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

/*
 * 本类总结：
 * 1. 核心职责：`BaseRuleChainMetadataConstructor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
