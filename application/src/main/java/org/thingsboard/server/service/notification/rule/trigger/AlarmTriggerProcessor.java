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

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig.ClearRule;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

/**
 * 中文说明：
 * 1. `AlarmTriggerProcessor` 是 ThingsBoard Application 中处理告警的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `NotificationRuleTriggerProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Service
public class AlarmTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmTrigger, AlarmNotificationRuleTriggerConfig> {

    /**
     * 功能：执行 `matchesFilter` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean matchesFilter(AlarmTrigger trigger, AlarmNotificationRuleTriggerConfig triggerConfig) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        Alarm alarm = alarmUpdate.getAlarm();
        if (!typeMatches(alarm, triggerConfig)) {
            return false;
        }

        if (alarmUpdate.isCreated()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.CREATED)) {
                return severityMatches(alarm, triggerConfig);
            }
        }  else if (alarmUpdate.isSeverityChanged()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.SEVERITY_CHANGED)) {
                return severityMatches(alarmUpdate.getOld(), triggerConfig) || severityMatches(alarm, triggerConfig);
            }  else {
                // if we haven't yet sent notification about the alarm
                return !severityMatches(alarmUpdate.getOld(), triggerConfig) && severityMatches(alarm, triggerConfig);
            }
        } else if (alarmUpdate.isAcknowledged()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.ACKNOWLEDGED)) {
                return severityMatches(alarm, triggerConfig);
            }
        } else if (alarmUpdate.isCleared()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.CLEARED)) {
                return severityMatches(alarm, triggerConfig);
            }
        }
        return false;
    }

    /**
     * 功能：执行 `matchesClearRule` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean matchesClearRule(AlarmTrigger trigger, AlarmNotificationRuleTriggerConfig triggerConfig) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        Alarm alarm = alarmUpdate.getAlarm();
        if (!typeMatches(alarm, triggerConfig)) {
            return false;
        }
        if (alarmUpdate.isDeleted()) {
            return true;
        }
        ClearRule clearRule = triggerConfig.getClearRule();
        if (clearRule != null) {
            if (isNotEmpty(clearRule.getAlarmStatuses())) {
                return AlarmStatusFilter.from(clearRule.getAlarmStatuses()).matches(alarm);
            }
        }
        return false;
    }

    /**
     * 功能：执行 `severityMatches` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    private boolean severityMatches(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        return emptyOrContains(triggerConfig.getAlarmSeverities(), alarm.getSeverity());
    }

    /**
     * 功能：执行 `typeMatches` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    private boolean typeMatches(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        return emptyOrContains(triggerConfig.getAlarmTypes(), alarm.getType());
    }

    /**
     * 功能：执行 `constructNotificationInfo` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(AlarmTrigger trigger) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        AlarmInfo alarmInfo = alarmUpdate.getAlarm();
        return AlarmNotificationInfo.builder()
                .alarmId(alarmInfo.getUuidId())
                .alarmType(alarmInfo.getType())
                .action(alarmUpdate.isCreated() ? "created" :
                        alarmUpdate.isSeverityChanged() ? "severity changed" :
                        alarmUpdate.isAcknowledged() ? "acknowledged" :
                        alarmUpdate.isCleared() ? "cleared" :
                        alarmUpdate.isDeleted() ? "deleted" : null)
                .alarmOriginator(alarmInfo.getOriginator())
                .alarmOriginatorName(alarmInfo.getOriginatorName())
                .alarmSeverity(alarmInfo.getSeverity())
                .alarmStatus(alarmInfo.getStatus())
                .acknowledged(alarmInfo.isAcknowledged())
                .cleared(alarmInfo.isCleared())
                .alarmCustomerId(alarmInfo.getCustomerId())
                .dashboardId(alarmInfo.getDashboardId())
                .build();
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

}
