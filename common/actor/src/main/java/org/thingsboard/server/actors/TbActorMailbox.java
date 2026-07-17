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
package org.thingsboard.server.actors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorError;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 中文说明：
 * 1. `TbActorMailbox` 是 ThingsBoard Actor 中处理 `Tb Actor Mailbox` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `TbActorCtx`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public final class TbActorMailbox implements TbActorCtx {
    /**
     * `HIGH_PRIORITY`常量，用于统一引用固定值。
     */
    private static final boolean HIGH_PRIORITY = true;
    private static final boolean NORMAL_PRIORITY = false;

    /**
     * `FREE`常量，用于统一引用固定值。
     */
    private static final boolean FREE = false;
    private static final boolean BUSY = true;

    /**
     * `NOT_READY`常量，用于统一引用固定值。
     */
    private static final boolean NOT_READY = false;
    private static final boolean READY = true;

    /**
     * Actor 系统，表示当前对象的对应属性。
     */
    private final TbActorSystem system;
    private final TbActorSystemSettings settings;
    /**
     * `selfId`ID，用于定位对应业务对象。
     */
    private final TbActorId selfId;
    private final TbActorRef parentRef;
    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    private final TbActor actor;
    private final Dispatcher dispatcher;
    private final ConcurrentLinkedQueue<TbActorMsg> highPriorityMsgs = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TbActorMsg> normalPriorityMsgs = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean busy = new AtomicBoolean(FREE);
    private final AtomicBoolean ready = new AtomicBoolean(NOT_READY);
    private final AtomicBoolean destroyInProgress = new AtomicBoolean();
    /**
     * `stopReason` 字段，保存当前对象的对应属性。
     */
    private volatile TbActorStopReason stopReason;

    /**
     * 功能：初始化或启动Actor 实例。
     * 参数：无。
     * 返回：无。
     */
    public void initActor() {
        dispatcher.getExecutor().execute(() -> tryInit(1));
    }

    /**
     * 功能：执行 `tryInit` 对应的处理。
     * 参数：
     * - `attempt`：`attempt` 参数。
     * 返回：无。
     */
    private void tryInit(int attempt) {
        try {
            log.debug("[{}] Trying to init actor, attempt: {}", selfId, attempt);
            if (!destroyInProgress.get()) {
                actor.init(this);
                if (!destroyInProgress.get()) {
                    ready.set(READY);
                    tryProcessQueue(false);
                }
            }
        } catch (Throwable t) {
            InitFailureStrategy strategy;
            int attemptIdx = attempt + 1;
            if (isUnrecoverable(t)) {
                strategy = InitFailureStrategy.stop();
            } else {
                log.debug("[{}] Failed to init actor, attempt: {}", selfId, attempt, t);
                strategy = actor.onInitFailure(attempt, t);
            }
            if (strategy.isStop() || (settings.getMaxActorInitAttempts() > 0 && attemptIdx > settings.getMaxActorInitAttempts())) {
                log.info("[{}] Failed to init actor, attempt {}, going to stop attempts.", selfId, attempt, t);
                stopReason = TbActorStopReason.INIT_FAILED;
                destroy(t.getCause());
            } else if (strategy.getRetryDelay() > 0) {
                log.info("[{}] Failed to init actor, attempt {}, going to retry in attempts in {}ms", selfId, attempt, strategy.getRetryDelay());
                log.debug("[{}] Error", selfId, t);
                system.getScheduler().schedule(() -> dispatcher.getExecutor().execute(() -> tryInit(attemptIdx)), strategy.getRetryDelay(), TimeUnit.MILLISECONDS);
            } else {
                log.info("[{}] Failed to init actor, attempt {}, going to retry immediately", selfId, attempt);
                log.debug("[{}] Error", selfId, t);
                dispatcher.getExecutor().execute(() -> tryInit(attemptIdx));
            }
        }
    }

    /**
     * 功能：判断`Unrecoverable`。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：判断结果。
     */
    private static boolean isUnrecoverable(Throwable t) {
        if (t instanceof TbActorException && t.getCause() != null) {
            t = t.getCause();
        }
        return t instanceof TbActorError && ((TbActorError) t).isUnrecoverable();
    }

    /**
     * 功能：执行 `enqueue` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `highPriority`：`highPriority` 参数。
     * 返回：无。
     */
    private void enqueue(TbActorMsg msg, boolean highPriority) {
        if (!destroyInProgress.get()) {
            if (highPriority) {
                highPriorityMsgs.add(msg);
            } else {
                normalPriorityMsgs.add(msg);
            }
            tryProcessQueue(true);
        } else {
            if (highPriority && msg.getMsgType().equals(MsgType.RULE_NODE_UPDATED_MSG)) {
                synchronized (this) {
                    if (stopReason == TbActorStopReason.INIT_FAILED) {
                        destroyInProgress.set(false);
                        stopReason = null;
                        initActor();
                    } else {
                        msg.onTbActorStopped(stopReason);
                    }
                }
            } else {
                msg.onTbActorStopped(stopReason);
            }
        }
    }

    /**
     * 功能：执行 `tryProcessQueue` 对应的处理。
     * 参数：
     * - `newMsg`：待处理消息。
     * 返回：无。
     */
    private void tryProcessQueue(boolean newMsg) {
        if (ready.get() == READY) {
            if (newMsg || !highPriorityMsgs.isEmpty() || !normalPriorityMsgs.isEmpty()) {
                if (busy.compareAndSet(FREE, BUSY)) {
                    dispatcher.getExecutor().execute(this::processMailbox);
                } else {
                    log.trace("[{}] MessageBox is busy, new msg: {}", selfId, newMsg);
                }
            } else {
                log.trace("[{}] MessageBox is empty, new msg: {}", selfId, newMsg);
            }
        } else {
            log.trace("[{}] MessageBox is not ready, new msg: {}", selfId, newMsg);
        }
    }

    /**
     * 功能：处理`Mailbox`。
     * 参数：无。
     * 返回：无。
     */
    private void processMailbox() {
        boolean noMoreElements = false;
        for (int i = 0; i < settings.getActorThroughput(); i++) {
            TbActorMsg msg = highPriorityMsgs.poll();
            if (msg == null) {
                msg = normalPriorityMsgs.poll();
            }
            if (msg != null) {
                try {
                    log.debug("[{}] Going to process message: {}", selfId, msg);
                    actor.process(msg);
                } catch (TbRuleNodeUpdateException updateException) {
                    stopReason = TbActorStopReason.INIT_FAILED;
                    destroy(updateException.getCause());
                } catch (Throwable t) {
                    log.debug("[{}] Failed to process message: {}", selfId, msg, t);
                    ProcessFailureStrategy strategy = actor.onProcessFailure(msg, t);
                    if (strategy.isStop()) {
                        system.stop(selfId);
                    }
                }
            } else {
                noMoreElements = true;
                break;
            }
        }
        if (noMoreElements) {
            busy.set(FREE);
            dispatcher.getExecutor().execute(() -> tryProcessQueue(false));
        } else {
            dispatcher.getExecutor().execute(this::processMailbox);
        }
    }

    /**
     * 功能：获取`Self`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbActorId getSelf() {
        return selfId;
    }

    /**
     * 功能：执行 `tell` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * - `actorMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void tell(TbActorId target, TbActorMsg actorMsg) {
        system.tell(target, actorMsg);
    }

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void broadcastToChildren(TbActorMsg msg) {
        broadcastToChildren(msg, false);
    }

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `highPriority`：`highPriority` 参数。
     * 返回：无。
     */
    @Override
    public void broadcastToChildren(TbActorMsg msg, boolean highPriority) {
        system.broadcastToChildren(selfId, msg, highPriority);
    }

    /**
     * 功能：执行 `broadcastToChildrenByType` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    @Override
    public void broadcastToChildrenByType(TbActorMsg msg, EntityType entityType) {
        broadcastToChildren(msg, actorId -> entityType.equals(actorId.getEntityType()));
    }

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `childFilter`：`childFilter` 参数。
     * 返回：无。
     */
    @Override
    public void broadcastToChildren(TbActorMsg msg, Predicate<TbActorId> childFilter) {
        system.broadcastToChildren(selfId, childFilter, msg);
    }

    /**
     * 功能：执行 `filterChildren` 对应的处理。
     * 参数：
     * - `childFilter`：`childFilter` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TbActorId> filterChildren(Predicate<TbActorId> childFilter) {
        return system.filterChildren(selfId, childFilter);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * 返回：无。
     */
    @Override
    public void stop(TbActorId target) {
        system.stop(target);
    }

    /**
     * 功能：获取Actor 实例。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * - `dispatcher`：`dispatcher` 参数。
     * - `creator`：`creator` 参数。
     * - `createCondition`：`createCondition` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbActorRef getOrCreateChildActor(TbActorId actorId, Supplier<String> dispatcher, Supplier<TbActorCreator> creator, Supplier<Boolean> createCondition) {
        TbActorRef actorRef = system.getActor(actorId);
        if (actorRef == null && createCondition.get()) {
            return system.createChildActor(dispatcher.get(), creator.get(), selfId);
        } else {
            return actorRef;
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    public void destroy(Throwable cause) {
        if (stopReason == null) {
            stopReason = TbActorStopReason.STOPPED;
        }
        destroyInProgress.set(true);
        dispatcher.getExecutor().execute(() -> {
            try {
                ready.set(NOT_READY);
                actor.destroy(stopReason, cause);
                highPriorityMsgs.forEach(msg -> msg.onTbActorStopped(stopReason));
                normalPriorityMsgs.forEach(msg -> msg.onTbActorStopped(stopReason));
            } catch (Throwable t) {
                log.warn("[{}] Failed to destroy actor: {}", selfId, t);
            }
        });
    }

    /**
     * 功能：获取Actor 实例。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbActorId getActorId() {
        return selfId;
    }

    /**
     * 功能：执行 `tell` 对应的处理。
     * 参数：
     * - `actorMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void tell(TbActorMsg actorMsg) {
        enqueue(actorMsg, NORMAL_PRIORITY);
    }

    /**
     * 功能：执行 `tellWithHighPriority` 对应的处理。
     * 参数：
     * - `actorMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void tellWithHighPriority(TbActorMsg actorMsg) {
        enqueue(actorMsg, HIGH_PRIORITY);
    }

}
