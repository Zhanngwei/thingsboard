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

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`RuleChainEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
public class RuleChainEdgeTest extends AbstractEdgeTest {

    /**
     * 字段说明：
     * 1. 保存 `CONFIGURATION_VERSION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int CONFIGURATION_VERSION = 5;

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testRuleChains` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSendRuleChainMetadataRequestToCloud` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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
     * 方法说明：
     * 1. 职责：执行 `testRuleChainMetadataRequestMsg` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (RuleNode ruleNode : ruleChainMetadataMsg.getNodes()) {
            Assert.assertEquals(CONFIGURATION_VERSION, ruleNode.getConfigurationVersion());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createRuleChainMetadata` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSetRootRuleChain` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
