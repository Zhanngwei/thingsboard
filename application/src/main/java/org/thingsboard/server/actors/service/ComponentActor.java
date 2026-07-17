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
package org.thingsboard.server.actors.service;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.actors.stats.StatsPersistMsg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `ComponentActor` 是 ThingsBoard Application 中处理 `Component Actor` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `EntityId`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Slf4j
public abstract class ComponentActor<T extends EntityId, P extends ComponentMsgProcessor<T>> extends ContextAwareActor {

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long lastPersistedErrorTs = 0L;
    protected final TenantId tenantId;
    /**
     * `id`ID，用于定位对应业务对象。
     */
    protected final T id;
    protected P processor;
    /**
     * `messagesProcessed` 字段，保存当前对象的对应属性。
     */
    private long messagesProcessed;
    private long errorsOccurred;

    /**
     * 功能：创建 `ComponentActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public ComponentActor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext);
        this.tenantId = tenantId;
        this.id = id;
    }

    /**
     * 功能：保存或创建处理器。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    abstract protected P createProcessor(TbActorCtx ctx);

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        this.processor = createProcessor(ctx);
        initProcessor(ctx);
    }

    /**
     * 功能：初始化或启动处理器。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    protected void initProcessor(TbActorCtx ctx) throws TbActorException {
        try {
            log.debug("[{}][{}][{}] Starting processor.", tenantId, id, id.getEntityType());
            processor.start(ctx);
            logLifecycleEvent(ComponentLifecycleEvent.STARTED);
            if (systemContext.isStatisticsEnabled()) {
                scheduleStatsPersistTick();
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to start {} processor.", tenantId, id, id.getEntityType(), e);
            logAndPersist("OnStart", e, true);
            logLifecycleEvent(ComponentLifecycleEvent.STARTED, e);
            throw new TbActorException("Failed to init actor", e);
        }
    }

    /**
     * 功能：执行 `scheduleStatsPersistTick` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void scheduleStatsPersistTick() {
        try {
            processor.scheduleStatsPersistTick(ctx, systemContext.getStatisticsPersistFrequency());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to schedule statistics store message. No statistics is going to be stored: {}", tenantId, id, e.getMessage());
            logAndPersist("onScheduleStatsPersistMsg", e);
        }
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
        try {
            log.debug("[{}][{}][{}] Stopping processor.", tenantId, id, id.getEntityType());
            if (processor != null) {
                processor.stop(ctx);
            }
            logLifecycleEvent(ComponentLifecycleEvent.STOPPED);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to stop {} processor: {}", tenantId, id, id.getEntityType(), e.getMessage());
            logAndPersist("OnStop", e, true);
            logLifecycleEvent(ComponentLifecycleEvent.STOPPED, e);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    protected void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        log.debug("[{}][{}][{}] onComponentLifecycleMsg: [{}]", tenantId, id, id.getEntityType(), msg.getEvent());
        try {
            switch (msg.getEvent()) {
                case CREATED:
                    processor.onCreated(ctx);
                    break;
                case UPDATED:
                    processor.onUpdate(ctx);
                    break;
                case ACTIVATED:
                    processor.onActivate(ctx);
                    break;
                case SUSPENDED:
                    processor.onSuspend(ctx);
                    break;
                case DELETED:
                    processor.onStop(ctx);
                    ctx.stop(ctx.getSelf());
                    break;
                default:
                    break;
            }
            logLifecycleEvent(msg.getEvent());
        } catch (Exception e) {
            logAndPersist("onLifecycleMsg", e, true);
            logLifecycleEvent(msg.getEvent(), e);
            if (e instanceof TbRuleNodeUpdateException) {
                throw (TbRuleNodeUpdateException) e;
            }
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    protected void onClusterEventMsg(PartitionChangeMsg msg) {
        try {
            processor.onPartitionChangeMsg(msg);
        } catch (Exception e) {
            logAndPersist("onClusterEventMsg", e);
        }
    }

    /**
     * 功能：处理`on Stats Persist Tick`。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    protected void onStatsPersistTick(EntityId entityId) {
        try {
            systemContext.getStatsActor().tell(new StatsPersistMsg(messagesProcessed, errorsOccurred, tenantId, entityId));
            resetStatsCounters();
        } catch (Exception e) {
            logAndPersist("onStatsPersistTick", e);
        }
    }

    /**
     * 功能：执行 `resetStatsCounters` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void resetStatsCounters() {
        messagesProcessed = 0;
        errorsOccurred = 0;
    }

    /**
     * 功能：执行 `increaseMessagesProcessedCount` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void increaseMessagesProcessedCount() {
        messagesProcessed++;
    }

    /**
     * 功能：执行 `logAndPersist` 对应的处理。
     * 参数：
     * - `method`：`method` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    protected void logAndPersist(String method, Exception e) {
        logAndPersist(method, e, false);
    }

    /**
     * 功能：执行 `logAndPersist` 对应的处理。
     * 参数：
     * - `method`：`method` 参数。
     * - `e`：`e` 参数。
     * - `critical`：`critical` 参数。
     * 返回：无。
     */
    private void logAndPersist(String method, Exception e, boolean critical) {
        errorsOccurred++;
        String componentName = processor != null ? processor.getComponentName() : "Unknown";
        if (critical) {
            log.debug("[{}][{}][{}] Failed to process method: {}", id, tenantId, componentName, method);
            log.debug("Critical Error: ", e);
        } else {
            log.trace("[{}][{}][{}] Failed to process method: {}", id, tenantId, componentName, method);
            log.trace("Debug Error: ", e);
        }
        long ts = System.currentTimeMillis();
        if (ts - lastPersistedErrorTs > getErrorPersistFrequency()) {
            systemContext.persistError(tenantId, id, method, e);
            lastPersistedErrorTs = ts;
        }
    }

    /**
     * 功能：执行 `logLifecycleEvent` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    private void logLifecycleEvent(ComponentLifecycleEvent event) {
        logLifecycleEvent(event, null);
    }

    /**
     * 功能：执行 `logLifecycleEvent` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    protected void logLifecycleEvent(ComponentLifecycleEvent event, Exception e) {
        systemContext.persistLifecycleEvent(tenantId, id, event, e);
    }

    /**
     * 功能：获取错误信息。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getErrorPersistFrequency();

}
