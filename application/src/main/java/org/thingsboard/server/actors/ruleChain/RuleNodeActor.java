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
 * 1. `RuleNodeActor` 是 ThingsBoard Application 中处理规则节点消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `RuleEngineComponentActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
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
     * 1. `ActorCreator` 是 ThingsBoard Application 中创建或提供 `Actor Creator` 对象的构造组件。
     * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
     * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
     * 4. 直接依赖的类型边界包括 `ContextBasedCreator`。
     * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
     * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
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
