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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.notification.info.AlarmCommentNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmCommentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmCommentTrigger;
import org.thingsboard.server.dao.entity.EntityService;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

/**
 * 中文说明：
 * 1. `AlarmCommentTriggerProcessor` 是 ThingsBoard Application 中处理告警的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `NotificationRuleTriggerProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Service
@RequiredArgsConstructor
public class AlarmCommentTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmCommentTrigger, AlarmCommentNotificationRuleTriggerConfig> {

    /**
     * 实体，提供当前类调用的业务操作。
     */
    private final EntityService entityService;

    /**
     * 功能：执行 `matchesFilter` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean matchesFilter(AlarmCommentTrigger trigger, AlarmCommentNotificationRuleTriggerConfig triggerConfig) {
        if (trigger.getActionType() == ActionType.UPDATED_COMMENT && !triggerConfig.isNotifyOnCommentUpdate()) {
            return false;
        }
        if (triggerConfig.isOnlyUserComments()) {
            if (trigger.getComment().getType() == AlarmCommentType.SYSTEM) {
                return false;
            }
        }
        Alarm alarm = trigger.getAlarm();
        return emptyOrContains(triggerConfig.getAlarmTypes(), alarm.getType()) &&
                emptyOrContains(triggerConfig.getAlarmSeverities(), alarm.getSeverity()) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarm));
    }

    /**
     * 功能：执行 `constructNotificationInfo` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(AlarmCommentTrigger trigger) {
        Alarm alarm = trigger.getAlarm();
        String originatorName;
        if (alarm instanceof AlarmInfo) {
            originatorName = ((AlarmInfo) alarm).getOriginatorName();
        } else {
            originatorName = entityService.fetchEntityName(trigger.getTenantId(), alarm.getOriginator()).orElse("");
        }
        return AlarmCommentNotificationInfo.builder()
                .comment(trigger.getComment().getComment().get("text").asText())
                .action(trigger.getActionType() == ActionType.ADDED_COMMENT ? "added" : "updated")
                .userEmail(trigger.getUser().getEmail())
                .userFirstName(trigger.getUser().getFirstName())
                .userLastName(trigger.getUser().getLastName())
                .alarmId(alarm.getUuidId())
                .alarmType(alarm.getType())
                .alarmOriginator(alarm.getOriginator())
                .alarmOriginatorName(originatorName)
                .alarmSeverity(alarm.getSeverity())
                .alarmStatus(alarm.getStatus())
                .alarmCustomerId(alarm.getCustomerId())
                .dashboardId(alarm.getDashboardId())
                .build();
    }

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
