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
 * 1. `RuleChainActor` 是 ThingsBoard Application 中处理规则链消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `RuleEngineComponentActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
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
     * 1. `ActorCreator` 是 ThingsBoard Application 中创建或提供 `Actor Creator` 对象的构造组件。
     * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
     * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
     * 4. 直接依赖的类型边界包括 `ContextBasedCreator`。
     * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
     * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
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
