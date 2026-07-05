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
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.service.ComponentActor;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventTrigger;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorStopReason;

/**
 * 中文说明：
 * 1. 类目的：`RuleEngineComponentActor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
public abstract class RuleEngineComponentActor<T extends EntityId, P extends ComponentMsgProcessor<T>> extends ComponentActor<T, P> {

    /**
     * 功能：创建 `RuleEngineComponentActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public RuleEngineComponentActor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext, tenantId, id);
    }

    /**
     * 功能：执行 `logLifecycleEvent` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    protected void logLifecycleEvent(ComponentLifecycleEvent event, Exception e) {
        super.logLifecycleEvent(event, e);
        if (e instanceof TbRuleNodeUpdateException || (event == ComponentLifecycleEvent.STARTED && e != null)) {
            return;
        }
        processNotificationRule(event, e);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：
     * - `stopReason`：`stopReason` 参数。
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {
        super.destroy(stopReason, cause);
        if (stopReason == TbActorStopReason.INIT_FAILED && cause != null) {
            processNotificationRule(ComponentLifecycleEvent.STARTED, cause);
        }
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `event`：`event` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    private void processNotificationRule(ComponentLifecycleEvent event, Throwable e) {
        systemContext.getNotificationRuleProcessor().process(RuleEngineComponentLifecycleEventTrigger.builder()
                .tenantId(tenantId)
                .ruleChainId(getRuleChainId())
                .ruleChainName(getRuleChainName())
                .componentId(id)
                .componentName(processor.getComponentName())
                .eventType(event)
                .error(e)
                .build());
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract RuleChainId getRuleChainId();

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getRuleChainName();

}

/*
 * 本类总结：
 * 1. 核心职责：`RuleEngineComponentActor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
