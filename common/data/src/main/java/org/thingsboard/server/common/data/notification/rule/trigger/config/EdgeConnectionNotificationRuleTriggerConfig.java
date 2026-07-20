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
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `EdgeConnectionNotificationRuleTriggerConfig` 是 ThingsBoard Common Data 中描述通知行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `NotificationRuleTriggerConfig`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeConnectionNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    /**
     * `edges`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<UUID> edges; // if empty - all edges
    private Set<EdgeConnectivityEvent> notifyOn;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.EDGE_CONNECTION;
    }

    /**
     * 中文说明：
     * 1. `EdgeConnectivityEvent` 是 ThingsBoard Common Data 中定义事件固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
     */
    public enum EdgeConnectivityEvent {
        CONNECTED, DISCONNECTED
    }

}
