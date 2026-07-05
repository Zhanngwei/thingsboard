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
 * 中文说明：`SnapshotUpdate` 是快照更新辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
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
    /*
     * 本类总结：`SnapshotUpdate` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
