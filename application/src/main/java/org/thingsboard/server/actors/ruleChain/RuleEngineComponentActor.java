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
 * 1. `RuleEngineComponentActor` 是 ThingsBoard Application 中处理 `Rule Engine Component` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `EntityId`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
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
