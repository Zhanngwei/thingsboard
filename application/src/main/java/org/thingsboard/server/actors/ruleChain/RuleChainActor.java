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

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;

/**
 * 中文说明：
 * 1. 类目的：`RuleChainActor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
public class RuleChainActor extends RuleEngineComponentActor<RuleChainId, RuleChainActorMessageProcessor> {

    /**
     * 规则链对象，用于描述当前业务场景。
     */
    private final RuleChain ruleChain;

    /**
     * 功能：创建 `RuleChainActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：新创建的对象实例。
     */
    private RuleChainActor(ActorSystemContext systemContext, TenantId tenantId, RuleChain ruleChain) {
        super(systemContext, tenantId, ruleChain.getId());
        this.ruleChain = ruleChain;
    }

    /**
     * 功能：保存或创建处理器。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    @Override
    protected RuleChainActorMessageProcessor createProcessor(TbActorCtx ctx) {
        return new RuleChainActorMessageProcessor(tenantId, ruleChain, systemContext,
                ctx.getParentRef(), ctx);
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
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                processor.onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case RULE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                processor.onTellNext((RuleNodeToRuleChainTellNextMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                processor.onRuleChainToRuleChainMsg((RuleChainToRuleChainMsg) msg);
                break;
            case RULE_CHAIN_INPUT_MSG:
                processor.onRuleChainInputMsg((RuleChainInputMsg) msg);
                break;
            case RULE_CHAIN_OUTPUT_MSG:
                processor.onRuleChainOutputMsg((RuleChainOutputMsg) msg);
                break;
            case PARTITION_CHANGE_MSG:
                processor.onPartitionChangeMsg((PartitionChangeMsg) msg);
                break;
            case STATS_PERSIST_TICK_MSG:
                onStatsPersistTick(id);
                break;
            default:
                return false;
        }
        return true;
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
         * 版本号常量，用于统一引用固定值。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final RuleChain ruleChain;

        /**
         * 功能：创建 `RuleChainActor` 实例，并初始化必要字段。
         * 参数：
         * - `context`：处理上下文。
         * - `tenantId`：租户IDID。
         * - `ruleChain`：`ruleChain` 参数。
         * 返回：新创建的对象实例。
         */
        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChain ruleChain) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChain = ruleChain;
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(ruleChain.getId());
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActor createActor() {
            return new RuleChainActor(context, tenantId, ruleChain);
        }
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected RuleChainId getRuleChainId() {
        return ruleChain.getId();
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getRuleChainName() {
        return ruleChain.getName();
    }

    /**
     * 功能：获取错误信息。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getRuleChainErrorPersistFrequency();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainActor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
