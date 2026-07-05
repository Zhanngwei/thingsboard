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
 * 1. 类目的：`AlarmTriggerProcessor` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
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

/*
 * 本类总结：
 * 1. 核心职责：`AlarmTriggerProcessor` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
