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
package org.thingsboard.rule.engine.profile;

import lombok.Getter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;

import java.util.Set;

/**
 * 中文说明：
 * 1. `SnapshotUpdate` 是 ThingsBoard Rule Engine Components 中围绕 `Snapshot Update` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
class SnapshotUpdate {

    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private final AlarmConditionKeyType type;
    /**
     * `keys`集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final Set<AlarmConditionFilterKey> keys;

    /**
     * 功能：创建 `SnapshotUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `type`：类型。
     * - `keys`：键。
     * 返回：新创建的对象实例。
     */
    SnapshotUpdate(AlarmConditionKeyType type, Set<AlarmConditionFilterKey> keys) {
        this.type = type;
        this.keys = keys;
    }

    /**
     * 功能：判断`Update`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean hasUpdate(){
        return !keys.isEmpty();
    }
}
