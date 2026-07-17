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
package org.thingsboard.server.actors.shared;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.stats.StatsPersistTick;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.RuleNodeException;

/**
 * 中文说明：
 * 1. `ComponentMsgProcessor` 是 ThingsBoard Application 中处理消息的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `EntityId`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class ComponentMsgProcessor<T extends EntityId> extends AbstractContextAwareMsgProcessor {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected final TenantId tenantId;
    protected final T entityId;
    /**
     * 状态，表示当前对象所处状态。
     */
    protected ComponentLifecycleState state;

    /**
     * 功能：创建 `ComponentMsgProcessor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    protected ComponentMsgProcessor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext);
        this.tenantId = tenantId;
        this.entityId = id;
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    protected TenantProfileConfiguration getTenantProfileConfiguration() {
        return systemContext.getTenantProfileCache().get(tenantId).getProfileData().getConfiguration();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public abstract String getComponentName();

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    public abstract void start(TbActorCtx context) throws Exception;

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    public abstract void stop(TbActorCtx context) throws Exception;

    /**
     * 功能：处理分区。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public abstract void onPartitionChangeMsg(PartitionChangeMsg msg) throws Exception;

    /**
     * 功能：处理`on Created`。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    public void onCreated(TbActorCtx context) throws Exception {
        start(context);
    }

    /**
     * 功能：处理`on Update`。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    public void onUpdate(TbActorCtx context) throws Exception {
        restart(context);
    }

    /**
     * 功能：处理`on Activate`。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    public void onActivate(TbActorCtx context) throws Exception {
        restart(context);
    }

    /**
     * 功能：处理`on Suspend`。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    public void onSuspend(TbActorCtx context) throws Exception {
        stop(context);
    }

    /**
     * 功能：处理`on Stop`。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    public void onStop(TbActorCtx context) throws Exception {
        stop(context);
    }

    /**
     * 功能：执行 `restart` 对应的处理。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    private void restart(TbActorCtx context) throws Exception {
        stop(context);
        start(context);
    }

    /**
     * 功能：执行 `scheduleStatsPersistTick` 对应的处理。
     * 参数：
     * - `context`：处理上下文。
     * - `statsPersistFrequency`：`statsPersistFrequency` 参数。
     * 返回：无。
     */
    public void scheduleStatsPersistTick(TbActorCtx context, long statsPersistFrequency) {
        schedulePeriodicMsgWithDelay(context, new StatsPersistTick(), statsPersistFrequency, statsPersistFrequency);
    }

    /**
     * 功能：校验消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * 返回：判断结果。
     */
    protected boolean checkMsgValid(TbMsg tbMsg) {
        var valid = tbMsg.isValid();
        if (!valid) {
            if (log.isTraceEnabled()) {
                log.trace("Skip processing of message: {} because it is no longer valid!", tbMsg);
            }
        }
        return valid;
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `tbMsg`：待处理消息。
     * 返回：无。
     */
    protected void checkComponentStateActive(TbMsg tbMsg) throws RuleNodeException {
        if (state != ComponentLifecycleState.ACTIVE) {
            log.debug("Component is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
            RuleNodeException ruleNodeException = getInactiveException();
            if (tbMsg != null) {
                tbMsg.getCallback().onFailure(ruleNodeException);
            }
            throw ruleNodeException;
        }
    }

    /**
     * 功能：获取`Inactive Exception`。
     * 参数：无。
     * 返回：处理结果。
     */
    abstract protected RuleNodeException getInactiveException();

}
