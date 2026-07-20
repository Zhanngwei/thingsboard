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

import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.function.Predicate;

/**
 * 中文说明：
 * 1. `TbEntityTypeActorIdPredicate` 是 ThingsBoard Application 中处理实体消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `Predicate`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@RequiredArgsConstructor
public class TbEntityTypeActorIdPredicate implements Predicate<TbActorId> {

    /**
     * 实体，用于区分不同处理分支。
     */
    private final EntityType entityType;

    /**
     * 功能：执行 `test` 对应的处理。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * 返回：判断结果。
     */
    @Override
    public boolean test(TbActorId actorId) {
        return actorId instanceof TbEntityActorId && testEntityId(((TbEntityActorId) actorId).getEntityId());
    }

    /**
     * 功能：验证实体ID相关场景。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    protected boolean testEntityId(EntityId entityId) {
        return entityId.getEntityType().equals(entityType);
    }
}
