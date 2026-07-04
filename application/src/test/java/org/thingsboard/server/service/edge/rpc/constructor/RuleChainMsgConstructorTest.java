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

@Slf4j
@RunWith(MockitoJUnitRunner.class)
/**
 * 中文说明：
 * 1. 类目的：`RuleChainMsgConstructorTest` 是ThingsBoard Application 测试模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class RuleChainMsgConstructorTest {

    /**
     * 字段说明：
     * 1. 保存 `RPC_CONNECTION_TYPE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String RPC_CONNECTION_TYPE = "RPC";

    /**
     * 字段说明：
     * 1. 保存 `ruleChainMsgConstructorV1` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RuleChainMsgConstructorV1 ruleChainMsgConstructorV1;

    /**
     * 字段说明：
     * 1. 保存 `tenantId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TenantId tenantId;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `setup` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setup() {
        ruleChainMsgConstructorV1 = new RuleChainMsgConstructorV1();
        tenantId = new TenantId(UUID.randomUUID());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConstructRuleChainMetadataUpdatedMsg_V_3_4_0` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConstructRuleChainMetadataUpdatedMsg_V_3_3_3` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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
     * 方法说明：
     * 1. 职责：执行 `assetV_3_3_3_and_V_3_4_0` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConstructRuleChainMetadataUpdatedMsg_V_3_3_0` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConstructRuleChainMetadataUpdatedMsg_V_3_3_0_inDifferentOrder` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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
     * 方法说明：
     * 1. 职责：执行 `assertCheckpointRuleNodeConfiguration` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `compareNodeConnectionInfoAndProto` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void compareNodeConnectionInfoAndProto(NodeConnectionInfo expected, org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto actual) {
        Assert.assertEquals(expected.getFromIndex(), actual.getFromIndex());
        Assert.assertEquals(expected.getToIndex(), actual.getToIndex());
        Assert.assertEquals(expected.getType(), actual.getType());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createRuleChainMetaData` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `createConnections` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `createNodeConnectionInfo` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private NodeConnectionInfo createNodeConnectionInfo(int fromIndex, int toIndex, String type) {
        NodeConnectionInfo result = new NodeConnectionInfo();
        result.setFromIndex(fromIndex);
        result.setToIndex(toIndex);
        result.setType(type);
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createRuleNodes` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `createRuleNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `createConnectionsInDifferentOrder` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `createRuleNodesInDifferentOrder` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `getOutputNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getOutputNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbRuleChainOutputNode",
                "Output node",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":178,\"layoutY\":592}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCheckpointNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getCheckpointNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbCheckpointNode",
                "Checkpoint node",
                JacksonUtil.toJsonNode("{\"queueName\":\"HighPriority\"}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":178,\"layoutY\":647}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getSaveTimeSeriesNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getSaveTimeSeriesNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode",
                "Save Timeseries",
                JacksonUtil.toJsonNode("{\"defaultTTL\":0}"),
                JacksonUtil.toJsonNode("{\"layoutX\":823,\"layoutY\":157}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getMessageTypeSwitchNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getMessageTypeSwitchNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode",
                "Message Type Switch",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"layoutX\":347,\"layoutY\":149}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getLogOtherNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getLogOtherNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.action.TbLogNode",
                "Log Other",
                JacksonUtil.toJsonNode("{\"jsScript\":\"return '\\\\nIncoming message:\\\\n' + JSON.stringify(msg) + '\\\\nIncoming metadata:\\\\n' + JSON.stringify(metadata);\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":378}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getPushToCloudNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getPushToCloudNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.edge.TbMsgPushToCloudNode",
                "Push to cloud",
                JacksonUtil.toJsonNode("{\"scope\":\"SERVER_SCOPE\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":1129,\"layoutY\":52}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getAcknowledgeNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getAcknowledgeNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbAckNode",
                "Acknowledge node",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":177,\"layoutY\":703}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceProfileNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getDeviceProfileNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.profile.TbDeviceProfileNode",
                "Device Profile Node",
                JacksonUtil.toJsonNode("{\"persistAlarmRulesState\":false,\"fetchAlarmRulesStateOnStart\":false}"),
                JacksonUtil.toJsonNode("{\"description\":\"Process incoming messages from devices with the alarm rules defined in the device profile. Dispatch all incoming messages with \\\"Success\\\" relation type.\",\"layoutX\":187,\"layoutY\":468}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getSaveClientAttributesNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getSaveClientAttributesNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode",
                "Save Client Attributes",
                JacksonUtil.toJsonNode("{\"scope\":\"CLIENT_SCOPE\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":52}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getLogRpcFromDeviceNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getLogRpcFromDeviceNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.action.TbLogNode",
                "Log RPC from Device",
                JacksonUtil.toJsonNode("{\"jsScript\":\"return '\\\\nIncoming message:\\\\n' + JSON.stringify(msg) + '\\\\nIncoming metadata:\\\\n' + JSON.stringify(metadata);\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":825,\"layoutY\":266}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getRpcCallRequestNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getRpcCallRequestNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode",
                "RPC Call Request",
                JacksonUtil.toJsonNode("{\"timeoutInSeconds\":60}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":466}"));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getPushToAnalyticsNode` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RuleNode getPushToAnalyticsNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbRuleChainInputNode",
                "Push to Analytics",
                JacksonUtil.toJsonNode("{\"ruleChainId\":\"af588000-6c7c-11ec-bafd-c9a47a5c8d99\"}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":477,\"layoutY\":560}"));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainMsgConstructorTest` 在 ThingsBoard Application 测试模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
