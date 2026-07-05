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
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbRuleEngineActorMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by ashvayka on 19.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`RuleNodeToRuleChainTellNextMsg` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@EqualsAndHashCode(callSuper = true)
@ToString
class RuleNodeToRuleChainTellNextMsg extends TbRuleEngineActorMsg implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 4577026446412871820L;
    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Getter
    private final RuleChainId ruleChainId;
    /**
     * `originator` 字段，保存当前对象的对应属性。
     */
    @Getter
    private final RuleNodeId originator;
    /**
     * 关系集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final Set<String> relationTypes;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Getter
    private final String failureMessage;

    /**
     * 功能：创建 `RuleNodeToRuleChainTellNextMsg` 实例，并初始化必要字段。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `originator`：`originator` 参数。
     * - `relationTypes`：类型。
     * - `tbMsg`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public RuleNodeToRuleChainTellNextMsg(RuleChainId ruleChainId, RuleNodeId originator, Set<String> relationTypes, TbMsg tbMsg, String failureMessage) {
        super(tbMsg);
        this.ruleChainId = ruleChainId;
        this.originator = originator;
        this.relationTypes = relationTypes;
        this.failureMessage = failureMessage;
    }

    /**
     * 功能：处理Actor 实例。
     * 参数：
     * - `reason`：`reason` 参数。
     * 返回：无。
     */
    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message = reason == TbActorStopReason.STOPPED ? String.format("Rule chain [%s] stopped", ruleChainId.getId()) : String.format("Failed to initialize rule chain [%s]!", ruleChainId.getId());
        msg.getCallback().onFailure(new RuleEngineException(message));
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_TO_RULE_CHAIN_TELL_NEXT_MSG;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RuleNodeToRuleChainTellNextMsg` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
