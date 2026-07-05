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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

/**
 * 中文说明：
 * 1. 类目的：`RuleNodeActor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@Slf4j
public class RuleNodeActor extends RuleEngineComponentActor<RuleNodeId, RuleNodeActorMessageProcessor> {

    /**
     * 规则链，用于标识或展示当前对象。
     */
    private final String ruleChainName;
    private final RuleChainId ruleChainId;
    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    private final RuleNodeId ruleNodeId;

    /**
     * 功能：创建 `RuleNodeActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `ruleChainName`：名称。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    private RuleNodeActor(ActorSystemContext systemContext, TenantId tenantId, RuleChainId ruleChainId, String ruleChainName, RuleNodeId ruleNodeId) {
        super(systemContext, tenantId, ruleNodeId);
        this.ruleChainName = ruleChainName;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
    }

    /**
     * 功能：保存或创建处理器。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    @Override
    protected RuleNodeActorMessageProcessor createProcessor(TbActorCtx ctx) {
        return new RuleNodeActorMessageProcessor(tenantId, this.ruleChainName, ruleNodeId, systemContext, ctx.getParentRef(), ctx);
    }

    /**
     * 功能：执行 `doProcess` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    protected boolean doProcess(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case COMPONENT_LIFE_CYCLE_MSG:
            case RULE_NODE_UPDATED_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_MSG:
                onRuleChainToRuleNodeMsg((RuleChainToRuleNodeMsg) msg);
                break;
            case RULE_TO_SELF_MSG:
                onRuleNodeToSelfMsg((RuleNodeToSelfMsg) msg);
                break;
            case STATS_PERSIST_TICK_MSG:
                onStatsPersistTick(id);
                break;
            case PARTITION_CHANGE_MSG:
                onClusterEventMsg((PartitionChangeMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 功能：处理规则节点。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onRuleNodeToSelfMsg(RuleNodeToSelfMsg msg) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Going to process rule msg: {}", ruleChainId, id, processor.getComponentName(), msg.getMsg());
        }
        try {
            processor.onRuleToSelfMsg(msg);
            increaseMessagesProcessedCount();
        } catch (Exception e) {
            logAndPersist("onRuleMsg", e);
        }
    }

    /**
     * 功能：处理规则链。
     * 参数：
     * - `envelope`：`envelope` 参数。
     * 返回：无。
     */
    private void onRuleChainToRuleNodeMsg(RuleChainToRuleNodeMsg envelope) {
        TbMsg msg = envelope.getMsg();
        if (!msg.isValid()) {
            if (log.isTraceEnabled()) {
                log.trace("Skip processing of message: {} because it is no longer valid!", msg);
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Going to process rule engine msg: {}", ruleChainId, id, processor.getComponentName(), msg);
        }
        try {
            processor.onRuleChainToRuleNodeMsg(envelope);
            increaseMessagesProcessedCount();
        } catch (Exception e) {
            logAndPersist("onRuleMsg", e);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`ActorCreator` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
     * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Actor / Command。
     */
    public static class ActorCreator extends ContextBasedCreator {

        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final RuleChainId ruleChainId;
        /**
         * 规则链，用于标识或展示当前对象。
         */
        private final String ruleChainName;
        private final RuleNodeId ruleNodeId;

        /**
         * 功能：创建 `RuleNodeActor` 实例，并初始化必要字段。
         * 参数：
         * - `context`：处理上下文。
         * - `tenantId`：租户IDID。
         * - `ruleChainId`：规则链ID。
         * - `ruleChainName`：名称。
         * - 其余参数：补充处理条件。
         * 返回：新创建的对象实例。
         */
        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChainId ruleChainId, String ruleChainName, RuleNodeId ruleNodeId) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChainId = ruleChainId;
            this.ruleChainName = ruleChainName;
            this.ruleNodeId = ruleNodeId;

        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(ruleNodeId);
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActor createActor() {
            return new RuleNodeActor(context, tenantId, ruleChainId, ruleChainName, ruleNodeId);
        }
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected RuleChainId getRuleChainId() {
        return ruleChainId;
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getRuleChainName() {
        return ruleChainName;
    }

    /**
     * 功能：获取错误信息。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getRuleNodeErrorPersistFrequency();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RuleNodeActor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
