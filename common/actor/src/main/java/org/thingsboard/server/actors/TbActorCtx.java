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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 中文说明：
 * 1. `TbActorCtx` 是 ThingsBoard Actor 中定义 `Tb Actor Ctx` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `TbActorRef`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbActorCtx extends TbActorRef {

    /**
     * 功能：获取`Self`。
     * 参数：无。
     * 返回：处理结果。
     */
    TbActorId getSelf();

    /**
     * 功能：获取`Parent Ref`。
     * 参数：无。
     * 返回：处理结果。
     */
    TbActorRef getParentRef();

    /**
     * 功能：执行 `tell` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void tell(TbActorId target, TbActorMsg msg);

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `target`：`target` 参数。
     * 返回：无。
     */
    void stop(TbActorId target);

    /**
     * 功能：获取Actor 实例。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * - `dispatcher`：`dispatcher` 参数。
     * - `creator`：`creator` 参数。
     * - `createCondition`：`createCondition` 参数。
     * 返回：处理结果。
     */
    TbActorRef getOrCreateChildActor(TbActorId actorId, Supplier<String> dispatcher, Supplier<TbActorCreator> creator, Supplier<Boolean> createCondition);

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void broadcastToChildren(TbActorMsg msg);

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `highPriority`：`highPriority` 参数。
     * 返回：无。
     */
    void broadcastToChildren(TbActorMsg msg, boolean highPriority);

    /**
     * 功能：执行 `broadcastToChildrenByType` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    void broadcastToChildrenByType(TbActorMsg msg, EntityType entityType);

    /**
     * 功能：执行 `broadcastToChildren` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `childFilter`：`childFilter` 参数。
     * 返回：无。
     */
    void broadcastToChildren(TbActorMsg msg, Predicate<TbActorId> childFilter);

    /**
     * 功能：执行 `filterChildren` 对应的处理。
     * 参数：
     * - `childFilter`：`childFilter` 参数。
     * 返回：匹配的数据集合。
     */
    List<TbActorId> filterChildren(Predicate<TbActorId> childFilter);
}
