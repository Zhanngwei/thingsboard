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
 * 1. 类目的：`ComponentActor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
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

/*
 * 本类总结：
 * 1. 核心职责：`ComponentActor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
