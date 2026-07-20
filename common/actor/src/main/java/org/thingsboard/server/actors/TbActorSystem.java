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

import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

/**
 * 中文说明：
 * 1. `TbActorSystem` 是 ThingsBoard Actor 中定义 `Tb Actor System` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbActorSystem {

    /**
     * 功能：获取调度器。
     * 参数：无。
     * 返回：处理结果。
     */
    ScheduledExecutorService getScheduler();

    /**
     * 功能：保存或创建`Dispatcher`。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * - `executor`：`executor` 参数。
     * 返回：无。
     */
    void createDispatcher(String dispatcherId, ExecutorService executor);

    /**
     * 功能：停止或关闭`Dispatcher`。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * 返回：无。
     */
    void destroyDispatcher(String dispatcherId);

    /**
     * 功能：获取Actor 实例。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * 返回：处理结果。
     */
    TbActorRef getActor(TbActorId actorId);

    /**
     * 功能：保存或创建Actor 实例。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * - `creator`：`creator` 参数。
     * 返回：处理结果。
     */
    TbActorRef createRootActor(String dispatcherId, TbActorCreator creator);

    /**
     * 功能：保存或创建Actor 实例。
     * 参数：
     * - `dispatcherId`：`dispatcherId`ID。
     * - `creator`：`creator` 参数。
     * - `parent`：`parent` 参数。
     * 返回：处理结果。
     */
    TbActorRef createChildActor(String dispatcherId, TbActorCreator creator, TbActorId parent);

    /**
     * 功能：执行 `tell` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * - `actorMsg`：待处理消息。
     * 返回：无。
     */
    void tell(TbActorId target, TbActorMsg actorMsg);

    /**
     * 功能：执行 `tellWithHighPriority` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * - `actorMsg`：待处理消息。
     * 返回：无。
     */
    void tellWithHighPriority(TbActorId target, TbActorMsg actorMsg);

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `actorRef`：`actorRef` 参数。
     * 返回：无。
     */
    void stop(TbActorRef actorRef);

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * 返回：无。
     */
    void stop(TbActorId actorId);

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void stop();

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void broadcastToChildren(TbActorId parent, TbActorMsg msg);

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `msg`：待处理消息。
     * - `highPriority`：`highPriority` 参数。
     * 返回：无。
     */
    void broadcastToChildren(TbActorId parent, TbActorMsg msg, boolean highPriority);

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `childFilter`：`childFilter` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void broadcastToChildren(TbActorId parent, Predicate<TbActorId> childFilter, TbActorMsg msg);

    /**
     * 功能：执行 `filterChildren` 对应的处理。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `childFilter`：`childFilter` 参数。
     * 返回：匹配的数据集合。
     */
    List<TbActorId> filterChildren(TbActorId parent, Predicate<TbActorId> childFilter);
}
