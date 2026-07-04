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
package org.thingsboard.server.service.edge.rpc.processor.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import static org.thingsboard.server.service.edge.DefaultEdgeNotificationService.EDGE_IS_ROOT_BODY_KEY;

@Slf4j
@Component
@TbCoreComponent
/**
 * 中文说明：
 * 1. 类目的：`RuleChainEdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class RuleChainEdgeProcessor extends BaseEdgeProcessor {

    /**
     * 方法说明：
     * 1. 职责：执行 `convertRuleChainEventToDownlink` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DownlinkMsg convertRuleChainEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (ruleChain != null) {
                    boolean isRoot = false;
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (edgeEvent.getBody() != null && edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY) != null) {
                        try {
                            isRoot = Boolean.parseBoolean(edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY).asText());
                        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                        } catch (Exception ignored) {}
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!isRoot) {
                        Edge edge = edgeService.findEdgeById(edgeEvent.getTenantId(), edgeEvent.getEdgeId());
                        isRoot = edge.getRootRuleChainId().equals(ruleChainId);
                    }
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    RuleChainUpdateMsg ruleChainUpdateMsg = ((RuleChainMsgConstructor)
                            // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
                            ruleChainMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                            .constructRuleChainUpdatedMsg(msgType, ruleChain, isRoot);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
                        .addRuleChainUpdateMsg(((RuleChainMsgConstructor) ruleChainMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                                .constructRuleChainDeleteMsg(ruleChainId))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertRuleChainMetadataEventToDownlink` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DownlinkMsg convertRuleChainMetadataEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        DownlinkMsg downlinkMsg = null;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ruleChain != null) {
            RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = ((RuleChainMsgConstructor)
                    // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
                    ruleChainMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                    .constructRuleChainMetadataUpdatedMsg(edgeEvent.getTenantId(), msgType, ruleChainMetaData, edgeVersion);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (ruleChainMetadataUpdateMsg != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainEdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
