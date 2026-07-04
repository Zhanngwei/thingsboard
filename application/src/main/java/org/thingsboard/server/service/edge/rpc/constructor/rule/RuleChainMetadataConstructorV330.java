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

@Slf4j
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
public class RuleChainMetadataConstructorV330 extends BaseRuleChainMetadataConstructor {

    private static final String RULE_CHAIN_INPUT_NODE = TbRuleChainInputNode.class.getName();
    private static final String TB_RULE_CHAIN_OUTPUT_NODE = TbRuleChainOutputNode.class.getName();

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `constructRuleChainMetadataUpdatedMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                        RuleChainMetadataUpdateMsg.Builder builder,
                                                        RuleChainMetaData ruleChainMetaData) {
        builder.setRuleChainIdMSB(ruleChainMetaData.getRuleChainId().getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainMetaData.getRuleChainId().getId().getLeastSignificantBits());
        List<RuleNode> supportedNodes = filterNodes(ruleChainMetaData.getNodes());

        NavigableSet<Integer> removedNodeIndexes = getRemovedNodeIndexes(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections());
        List<NodeConnectionInfo> connections = filterConnections(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections(), removedNodeIndexes);

        List<RuleChainConnectionInfo> ruleChainConnections = new ArrayList<>();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ruleChainMetaData.getRuleChainConnections() != null) {
            ruleChainConnections.addAll(ruleChainMetaData.getRuleChainConnections());
        }
        ruleChainConnections.addAll(addRuleChainConnections(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections()));
        builder.addAllNodes(constructNodes(supportedNodes))
                .addAllConnections(constructConnections(connections))
                .addAllRuleChainConnections(constructRuleChainConnections(ruleChainConnections, removedNodeIndexes));
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            Integer firstNodeIndex = ruleChainMetaData.getFirstNodeIndex();
            // decrease index because of removed nodes
            for (Integer removedIndex : removedNodeIndexes) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
     * 方法说明：
     * 1. 职责：执行 `getRemovedNodeIndexes` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private NavigableSet<Integer> getRemovedNodeIndexes(List<RuleNode> nodes, List<NodeConnectionInfo> connections) {
        TreeSet<Integer> removedIndexes = new TreeSet<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (NodeConnectionInfo connection : connections) {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (int i = 0; i < nodes.size(); i++) {
                RuleNode node = nodes.get(i);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)
                        || node.getType().equalsIgnoreCase(TB_RULE_CHAIN_OUTPUT_NODE)) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (connection.getFromIndex() == i || connection.getToIndex() == i) {
                        removedIndexes.add(i);
                    }
                }
            }
        }
        return removedIndexes.descendingSet();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `filterConnections` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<NodeConnectionInfo> filterConnections(List<RuleNode> nodes,
                                                       List<NodeConnectionInfo> connections,
                                                       NavigableSet<Integer> removedNodeIndexes) {
        List<NodeConnectionInfo> result = new ArrayList<>();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (connections != null) {
            result = connections.stream().filter(conn -> {
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (int i = 0; i < nodes.size(); i++) {
                    RuleNode node = nodes.get(i);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)
                            || node.getType().equalsIgnoreCase(TB_RULE_CHAIN_OUTPUT_NODE)) {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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
     * 方法说明：
     * 1. 职责：执行 `filterNodes` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `addRuleChainConnections` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
