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

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

/**
 * Created by ashvayka on 19.03.18.
 */

/**
 * 中文说明：
 * 1. `RuleNodeRelation` 是 ThingsBoard Application 中处理实体关系消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 它直接协作于 Actor 上下文、消息类型和对应的消息处理器。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Data
final class RuleNodeRelation {

    /**
     * `in` 字段，保存当前对象的对应属性。
     */
    private final EntityId in;
    private final EntityId out;
    /**
     * 类型，用于区分不同处理分支。
     */
    private final String type;

}
