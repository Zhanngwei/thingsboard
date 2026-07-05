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

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbActorSystem` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Data
public class DefaultTbActorSystem implements TbActorSystem {

    private final ConcurrentMap<String, Dispatcher> dispatchers = new ConcurrentHashMap<>();
    private final ConcurrentMap<TbActorId, TbActorMailbox> actors = new ConcurrentHashMap<>();
    private final ConcurrentMap<TbActorId, ReentrantLock> actorCreationLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<TbActorId, Set<TbActorId>> parentChildMap = new ConcurrentHashMap<>();

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final TbActorSystemSettings settings;
    /**
     * 调度器，用于安排延迟任务或周期任务。
     */
    @Getter
    private final ScheduledExecutorService scheduler;

    /**
     * 功能：创建 `DefaultTbActorSystem` 实例，并初始化必要字段。
     * 参数：
     * - `settings`：配置对象。
     * 返回：新创建的对象实例。
     */
    public DefaultTbActorSystem(TbActorSystemSettings settings) {
        this.settings = settings;
        this.scheduler = Executors.newScheduledThreadPool(settings.getSchedulerPoolSize(), ThingsBoardThreadFactory.forName("actor-system-scheduler"));
    }

    /**
     * 功能：保存或创建`Dispatcher`。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * - `executor`：`executor` 参数。
     * 返回：无。
     */
    @Override
    public void createDispatcher(String dispatcherId, ExecutorService executor) {
        Dispatcher current = dispatchers.putIfAbsent(dispatcherId, new Dispatcher(dispatcherId, executor));
        if (current != null) {
            throw new RuntimeException("Dispatcher with id [" + dispatcherId + "] is already registered!");
        }
    }

    /**
     * 功能：停止或关闭`Dispatcher`。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * 返回：无。
     */
    @Override
    public void destroyDispatcher(String dispatcherId) {
        Dispatcher dispatcher = dispatchers.remove(dispatcherId);
        if (dispatcher != null) {
            dispatcher.getExecutor().shutdownNow();
        } else {
            throw new RuntimeException("Dispatcher with id [" + dispatcherId + "] is not registered!");
        }
    }

    /**
     * 功能：获取Actor 实例。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * 返回：处理结果。
     */
    @Override
    public TbActorRef getActor(TbActorId actorId) {
        return actors.get(actorId);
    }

    /**
     * 功能：保存或创建Actor 实例。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * - `creator`：`creator` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbActorRef createRootActor(String dispatcherId, TbActorCreator creator) {
        return createActor(dispatcherId, creator, null);
    }

    /**
     * 功能：保存或创建Actor 实例。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * - `creator`：`creator` 参数。
     * - `parent`：`parent` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbActorRef createChildActor(String dispatcherId, TbActorCreator creator, TbActorId parent) {
        return createActor(dispatcherId, creator, parent);
    }

    /**
     * 功能：保存或创建Actor 实例。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * - `creator`：`creator` 参数。
     * - `parent`：`parent` 参数。
     * 返回：处理结果。
     */
    private TbActorRef createActor(String dispatcherId, TbActorCreator creator, TbActorId parent) {
        Dispatcher dispatcher = dispatchers.get(dispatcherId);
        if (dispatcher == null) {
            log.warn("Dispatcher with id [{}] is not registered!", dispatcherId);
            throw new RuntimeException("Dispatcher with id [" + dispatcherId + "] is not registered!");
        }

        TbActorId actorId = creator.createActorId();
        TbActorMailbox actorMailbox = actors.get(actorId);
        if (actorMailbox != null) {
            log.debug("Actor with id [{}] is already registered!", actorId);
        } else {
            Lock actorCreationLock = actorCreationLocks.computeIfAbsent(actorId, id -> new ReentrantLock());
            actorCreationLock.lock();
            try {
                actorMailbox = actors.get(actorId);
                if (actorMailbox == null) {
                    log.debug("Creating actor with id [{}]!", actorId);
                    TbActor actor = creator.createActor();
                    TbActorRef parentRef = null;
                    if (parent != null) {
                        parentRef = getActor(parent);
                        if (parentRef == null) {
                            throw new TbActorNotRegisteredException(parent, "Parent Actor with id [" + parent + "] is not registered!");
                        }
                    }
                    TbActorMailbox mailbox = new TbActorMailbox(this, settings, actorId, parentRef, actor, dispatcher);
                    actors.put(actorId, mailbox);
                    mailbox.initActor();
                    actorMailbox = mailbox;
                    if (parent != null) {
                        parentChildMap.computeIfAbsent(parent, id -> ConcurrentHashMap.newKeySet()).add(actorId);
                    }
                } else {
                    log.debug("Actor with id [{}] is already registered!", actorId);
                }
            } finally {
                actorCreationLock.unlock();
                actorCreationLocks.remove(actorId);
            }
        }
        return actorMailbox;
    }

    /**
     * 功能：执行 `tellWithHighPriority` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * - `actorMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void tellWithHighPriority(TbActorId target, TbActorMsg actorMsg) {
        tell(target, actorMsg, true);
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
        tell(target, actorMsg, false);
    }

    /**
     * 功能：执行 `tell` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * - `actorMsg`：待处理消息。
     * - `highPriority`：`highPriority` 参数。
     * 返回：无。
     */
    private void tell(TbActorId target, TbActorMsg actorMsg, boolean highPriority) {
        TbActorMailbox mailbox = actors.get(target);
        if (mailbox == null) {
            throw new TbActorNotRegisteredException(target, "Actor with id [" + target + "] is not registered!");
        }
        if (highPriority) {
            mailbox.tellWithHighPriority(actorMsg);
        } else {
            mailbox.tell(actorMsg);
        }
    }


    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void broadcastToChildren(TbActorId parent, TbActorMsg msg) {
        broadcastToChildren(parent, msg, false);
    }

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `msg`：待处理消息。
     * - `highPriority`：`highPriority` 参数。
     * 返回：无。
     */
    @Override
    public void broadcastToChildren(TbActorId parent, TbActorMsg msg, boolean highPriority) {
        broadcastToChildren(parent, id -> true, msg, highPriority);
    }

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `childFilter`：`childFilter` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void broadcastToChildren(TbActorId parent, Predicate<TbActorId> childFilter, TbActorMsg msg) {
        broadcastToChildren(parent, childFilter, msg, false);
    }

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `childFilter`：`childFilter` 参数。
     * - `msg`：待处理消息。
     * - `highPriority`：`highPriority` 参数。
     * 返回：无。
     */
    private void broadcastToChildren(TbActorId parent, Predicate<TbActorId> childFilter, TbActorMsg msg, boolean highPriority) {
        Set<TbActorId> children = parentChildMap.get(parent);
        if (children != null) {
            children.stream().filter(childFilter).forEach(id -> {
                try {
                    tell(id, msg, highPriority);
                } catch (TbActorNotRegisteredException e) {
                    log.warn("Actor is missing for {}", id);
                }
            });
        }
    }

    /**
     * 功能：执行 `filterChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `childFilter`：`childFilter` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TbActorId> filterChildren(TbActorId parent, Predicate<TbActorId> childFilter) {
        Set<TbActorId> children = parentChildMap.get(parent);
        if (children != null) {
            return children.stream().filter(childFilter).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `actorRef`：`actorRef` 参数。
     * 返回：无。
     */
    @Override
    public void stop(TbActorRef actorRef) {
        stop(actorRef.getActorId());
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * 返回：无。
     */
    @Override
    public void stop(TbActorId actorId) {
        Set<TbActorId> children = parentChildMap.remove(actorId);
        if (children != null) {
            for (TbActorId child : children) {
                stop(child);
            }
        }
        parentChildMap.values().forEach(parentChildren -> parentChildren.remove(actorId));

        TbActorMailbox mailbox = actors.remove(actorId);
        if (mailbox != null) {
            mailbox.destroy(null);
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        dispatchers.values().forEach(dispatcher -> {
            dispatcher.getExecutor().shutdown();
            try {
                dispatcher.getExecutor().awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("[{}] Failed to stop dispatcher", dispatcher.getDispatcherId(), e);
            }
        });
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        actors.clear();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbActorSystem` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
