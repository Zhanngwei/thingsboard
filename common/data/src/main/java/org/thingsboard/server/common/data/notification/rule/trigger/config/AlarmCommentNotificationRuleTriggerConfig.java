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
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.util.Set;

/**
 * 中文说明：
 * 1. `AlarmCommentNotificationRuleTriggerConfig` 是 ThingsBoard Common Data 中描述通知行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `NotificationRuleTriggerConfig`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmCommentNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    /**
     * 告警集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<String> alarmTypes;
    private Set<AlarmSeverity> alarmSeverities;
    /**
     * 告警集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<AlarmSearchStatus> alarmStatuses;
    private boolean onlyUserComments;
    /**
     * 是否满足`notifyOnCommentUpdate`条件。
     */
    private boolean notifyOnCommentUpdate;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_COMMENT;
    }

}
