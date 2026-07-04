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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.profile.state.PersistedAlarmRuleState;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AlarmSchedule;
import org.thingsboard.server.common.data.device.profile.CustomTimeSchedule;
import org.thingsboard.server.common.data.device.profile.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.SpecificTimeSchedule;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.common.adaptor.JsonConverter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.thingsboard.server.common.data.StringUtils.equalsAny;
import static org.thingsboard.server.common.data.StringUtils.splitByCommaWithoutQuotes;

@Data
@Slf4j
/**
 * 中文说明：`AlarmRuleState` 是告警规则状态辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
class AlarmRuleState {

    /**
     * 字段说明：保存 `severity`，表示告警严重级别，供本类方法在规则节点处理流程中使用。
     */
    private final AlarmSeverity severity;
    /**
     * 字段说明：保存 `alarmRule`，表示告警类型、严重级别或详情，供本类方法在规则节点处理流程中使用。
     */
    private final AlarmRule alarmRule;
    /**
     * 字段说明：保存 `spec`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final AlarmConditionSpec spec;
    /**
     * 字段说明：保存 `entityKeys`，表示消息体、元数据、属性或遥测中的键名，供本类方法在规则节点处理流程中使用。
     */
    private final Set<AlarmConditionFilterKey> entityKeys;
    /**
     * 字段说明：保存 `state`，表示运行状态，供本类方法在规则节点处理流程中使用。
     */
    private PersistedAlarmRuleState state;
    /**
     * 字段说明：保存 `updateFlag`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private boolean updateFlag;
    /**
     * 字段说明：保存 `dynamicPredicateValueCtx`，表示规则引擎上下文，供本类方法在规则节点处理流程中使用。
     */
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    /**
     * 方法说明：构造 `AlarmRuleState` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    AlarmRuleState(AlarmSeverity severity, AlarmRule alarmRule, Set<AlarmConditionFilterKey> entityKeys, PersistedAlarmRuleState state, DynamicPredicateValueCtx dynamicPredicateValueCtx) {
        this.severity = severity;
        this.alarmRule = alarmRule;
        this.entityKeys = entityKeys;
        if (state != null) {
            this.state = state;
        } else {
            this.state = new PersistedAlarmRuleState(0L, 0L, 0L);
        }
        this.spec = getSpec(alarmRule);
        this.dynamicPredicateValueCtx = dynamicPredicateValueCtx;
    }

    /**
     * 方法说明：校验配置或数据是否满足节点要求，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean validateTsUpdate(Set<AlarmConditionFilterKey> changedKeys) {
        for (AlarmConditionFilterKey key : changedKeys) {
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 方法说明：校验配置或数据是否满足节点要求，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean validateAttrUpdate(Set<AlarmConditionFilterKey> changedKeys) {
        //If the attribute was updated, but no new telemetry arrived - we ignore this until new telemetry is there.
        for (AlarmConditionFilterKey key : entityKeys) {
            if (key.getType().equals(AlarmConditionKeyType.TIME_SERIES)) {
                return false;
            }
        }
        for (AlarmConditionFilterKey key : changedKeys) {
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public AlarmConditionSpec getSpec(AlarmRule alarmRule) {
        AlarmConditionSpec spec = alarmRule.getCondition().getSpec();
        if (spec == null) {
            spec = new SimpleAlarmConditionSpec();
        }
        return spec;
    }

    /**
     * 方法说明：检查状态、关系、配置或数据合法性，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean checkUpdate() {
        if (updateFlag) {
            updateFlag = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 方法说明：执行 `eval` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public AlarmEvalResult eval(DataSnapshot data) {
        boolean active = isActive(data, data.getTs());
        switch (spec.getType()) {
            case SIMPLE:
                return (active && eval(alarmRule.getCondition(), data)) ? AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
            case DURATION:
                return evalDuration(data, active);
            case REPEATING:
                return evalRepeating(data, active);
            default:
                return AlarmEvalResult.FALSE;
        }
    }

    /**
     * 方法说明：执行 `isActive` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean isActive(DataSnapshot data, long eventTs) {
        if (eventTs == 0L) {
            eventTs = System.currentTimeMillis();
        }
        if (alarmRule.getSchedule() == null) {
            return true;
        }
        switch (alarmRule.getSchedule().getType()) {
            case ANY_TIME:
                return true;
            case SPECIFIC_TIME:
                return isActiveSpecific((SpecificTimeSchedule) getSchedule(data, alarmRule), eventTs);
            case CUSTOM:
                return isActiveCustom((CustomTimeSchedule) getSchedule(data, alarmRule), eventTs);
            default:
                throw new RuntimeException("Unsupported schedule type: " + alarmRule.getSchedule().getType());
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private AlarmSchedule getSchedule(DataSnapshot data, AlarmRule alarmRule) {
        AlarmSchedule schedule = alarmRule.getSchedule();
        EntityKeyValue dynamicValue = getDynamicPredicateValue(data, schedule.getDynamicValue());

        if (dynamicValue != null) {
            try {
                return JsonConverter.parse(dynamicValue.getJsonValue(), alarmRule.getSchedule().getClass());
            } catch (Exception e) {
                log.trace("Failed to parse AlarmSchedule from dynamicValue: {}", dynamicValue.getJsonValue(), e);
            }
        }
        return schedule;
    }

    /**
     * 方法说明：执行 `isActiveSpecific` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean isActiveSpecific(SpecificTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        if (schedule.getDaysOfWeek().size() != 7) {
            int dayOfWeek = zdt.getDayOfWeek().getValue();
            if (!schedule.getDaysOfWeek().contains(dayOfWeek)) {
                return false;
            }
        }
        long endsOn = schedule.getEndsOn();
        if (endsOn == 0) {
            // 24 hours in milliseconds
            endsOn = 86400000;
        }

        return isActive(eventTs, zoneId, zdt, schedule.getStartsOn(), endsOn);
    }

    /**
     * 方法说明：执行 `isActiveCustom` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean isActiveCustom(CustomTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        int dayOfWeek = zdt.toLocalDate().getDayOfWeek().getValue();
        for (CustomTimeScheduleItem item : schedule.getItems()) {
            if (item.getDayOfWeek() == dayOfWeek) {
                if (item.isEnabled()) {
                    long endsOn = item.getEndsOn();
                    if (endsOn == 0) {
                        // 24 hours in milliseconds
                        endsOn = 86400000;
                    }
                    return isActive(eventTs, zoneId, zdt, item.getStartsOn(), endsOn);
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 方法说明：执行 `isActive` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean isActive(long eventTs, ZoneId zoneId, ZonedDateTime zdt, long startsOn, long endsOn) {
        long startOfDay = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli();
        long msFromStartOfDay = eventTs - startOfDay;
        if (startsOn <= endsOn) {
            return startsOn <= msFromStartOfDay && endsOn > msFromStartOfDay;
        } else {
            return startsOn < msFromStartOfDay || (0 < msFromStartOfDay && msFromStartOfDay < endsOn);
        }
    }

    /**
     * 方法说明：清除告警或本地状态，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void clear() {
        if (state.getEventCount() > 0 || state.getLastEventTs() > 0 || state.getDuration() > 0) {
            state.setEventCount(0L);
            state.setLastEventTs(0L);
            state.setDuration(0L);
            updateFlag = true;
        }
    }

    /**
     * 方法说明：执行 `evalRepeating` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private AlarmEvalResult evalRepeating(DataSnapshot data, boolean active) {
        if (active && eval(alarmRule.getCondition(), data)) {
            state.setEventCount(state.getEventCount() + 1);
            updateFlag = true;
            long requiredRepeats = resolveRequiredRepeats(data);
            return state.getEventCount() >= requiredRepeats ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    /**
     * 方法说明：执行 `evalDuration` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private AlarmEvalResult evalDuration(DataSnapshot data, boolean active) {
        if (active && eval(alarmRule.getCondition(), data)) {
            if (state.getLastEventTs() > 0) {
                if (data.getTs() > state.getLastEventTs()) {
                    state.setDuration(state.getDuration() + (data.getTs() - state.getLastEventTs()));
                    state.setLastEventTs(data.getTs());
                    updateFlag = true;
                }
            } else {
                state.setLastEventTs(data.getTs());
                state.setDuration(0L);
                updateFlag = true;
            }
            long requiredDurationInMs = resolveRequiredDurationInMs(data);
            return state.getDuration() > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    /**
     * 方法说明：解析配置模板、消息字段或参数值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private long resolveRequiredRepeats(DataSnapshot data) {
        long repeatingTimes = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType.equals(AlarmConditionSpecType.REPEATING)) {
            RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;

            repeatingTimes = resolveDynamicValue(data, repeating.getPredicate());
        }
        return repeatingTimes;
    }

    /**
     * 方法说明：解析配置模板、消息字段或参数值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private long resolveRequiredDurationInMs(DataSnapshot data) {
        long durationTimeInMs = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType.equals(AlarmConditionSpecType.DURATION)) {
            DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
            TimeUnit timeUnit = duration.getUnit();

            durationTimeInMs = timeUnit.toMillis(resolveDynamicValue(data, duration.getPredicate()));
        }
        return durationTimeInMs;
    }

    /**
     * 方法说明：解析配置模板、消息字段或参数值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private Long resolveDynamicValue(DataSnapshot data, FilterPredicateValue<? extends Number> predicate) {
        DynamicValue<?> dynamicValue = predicate.getDynamicValue();
        Long defaultValue = predicate.getDefaultValue().longValue();
        if (dynamicValue == null || dynamicValue.getSourceAttribute() == null) {
            return defaultValue;
        }

        EntityKeyValue keyValue = getDynamicPredicateValue(data, dynamicValue);
        if (keyValue == null) {
            return defaultValue;
        }

        var longValue = getLongValue(keyValue);
        if (longValue == null) {
            String sourceAttribute = dynamicValue.getSourceAttribute();
            throw new NumericParseException(String.format("Could not convert attribute '%s' with value '%s' to numeric value!", sourceAttribute, getStrValue(keyValue)));
        }
        return longValue;
    }

    /**
     * 方法说明：执行 `eval` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public AlarmEvalResult eval(long ts, DataSnapshot dataSnapshot) {
        switch (spec.getType()) {
            case SIMPLE:
            case REPEATING:
                return AlarmEvalResult.NOT_YET_TRUE;
            case DURATION:
                long requiredDurationInMs = resolveRequiredDurationInMs(dataSnapshot);
                if (requiredDurationInMs > 0 && state.getLastEventTs() > 0 && ts > state.getLastEventTs()) {
                    long duration = state.getDuration() + (ts - state.getLastEventTs());
                    if (isActive(dataSnapshot, ts)) {
                        return duration > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
                    } else {
                        return AlarmEvalResult.FALSE;
                    }
                }
            default:
                return AlarmEvalResult.FALSE;
        }
    }

    /**
     * 方法说明：执行 `eval` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean eval(AlarmCondition condition, DataSnapshot data) {
        boolean eval = true;
        for (var filter : condition.getCondition()) {
            EntityKeyValue value;
            if (filter.getKey().getType().equals(AlarmConditionKeyType.CONSTANT)) {
                try {
                    value = getConstantValue(filter);
                } catch (RuntimeException e) {
                    log.warn("Failed to parse constant value from filter: {}", filter, e);
                    value = null;
                }
            } else {
                value = data.getValue(filter.getKey());
            }
            if (value == null) {
                return false;
            }
            eval = eval && eval(data, value, filter.getPredicate(), filter);
        }
        return eval;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private EntityKeyValue getConstantValue(AlarmConditionFilter filter) {
        EntityKeyValue value = new EntityKeyValue();
        String valueStr = filter.getValue().toString();
        switch (filter.getValueType()) {
            case STRING:
                value.setStrValue(valueStr);
                break;
            case DATE_TIME:
                value.setLngValue(Long.valueOf(valueStr));
                break;
            case NUMERIC:
                value.setDblValue(Double.valueOf(valueStr));
                break;
            case BOOLEAN:
                value.setBoolValue(Boolean.valueOf(valueStr));
                break;
        }
        return value;
    }

    /**
     * 方法说明：执行 `eval` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean eval(DataSnapshot data, EntityKeyValue value, KeyFilterPredicate predicate, AlarmConditionFilter filter) {
        switch (predicate.getType()) {
            case STRING:
                return evalStrPredicate(data, value, (StringFilterPredicate) predicate, filter);
            case NUMERIC:
                return evalNumPredicate(data, value, (NumericFilterPredicate) predicate, filter);
            case BOOLEAN:
                return evalBoolPredicate(data, value, (BooleanFilterPredicate) predicate, filter);
            case COMPLEX:
                return evalComplexPredicate(data, value, (ComplexFilterPredicate) predicate, filter);
            default:
                return false;
        }
    }

    /**
     * 方法说明：执行 `evalComplexPredicate` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean evalComplexPredicate(DataSnapshot data, EntityKeyValue ekv, ComplexFilterPredicate predicate, AlarmConditionFilter filter) {
        switch (predicate.getOperation()) {
            case OR:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (eval(data, ekv, kfp, filter)) {
                        return true;
                    }
                }
                return false;
            case AND:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (!eval(data, ekv, kfp, filter)) {
                        return false;
                    }
                }
                return true;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    /**
     * 方法说明：执行 `evalBoolPredicate` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean evalBoolPredicate(DataSnapshot data, EntityKeyValue ekv, BooleanFilterPredicate predicate, AlarmConditionFilter filter) {
        Boolean val = getBoolValue(ekv);
        if (val == null) {
            return false;
        }
        Boolean predicateValue = getPredicateValue(data, predicate.getValue(), filter, AlarmRuleState::getBoolValue);
        if (predicateValue == null) {
            return false;
        }
        switch (predicate.getOperation()) {
            case EQUAL:
                return val.equals(predicateValue);
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    /**
     * 方法说明：执行 `evalNumPredicate` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean evalNumPredicate(DataSnapshot data, EntityKeyValue ekv, NumericFilterPredicate predicate, AlarmConditionFilter filter) {
        Double val = getDblValue(ekv);
        if (val == null) {
            return false;
        }
        Double predicateValue = getPredicateValue(data, predicate.getValue(), filter, AlarmRuleState::getDblValue);
        if (predicateValue == null) {
            return false;
        }
        switch (predicate.getOperation()) {
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            case EQUAL:
                return val.equals(predicateValue);
            case GREATER:
                return val > predicateValue;
            case GREATER_OR_EQUAL:
                return val >= predicateValue;
            case LESS:
                return val < predicateValue;
            case LESS_OR_EQUAL:
                return val <= predicateValue;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    /**
     * 方法说明：执行 `evalStrPredicate` 对应的辅助逻辑，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean evalStrPredicate(DataSnapshot data, EntityKeyValue ekv, StringFilterPredicate predicate, AlarmConditionFilter filter) {
        String val = getStrValue(ekv);
        if (val == null) {
            return false;
        }
        String predicateValue = getPredicateValue(data, predicate.getValue(), filter, AlarmRuleState::getStrValue);
        if (predicateValue == null) {
            return false;
        }
        if (predicate.isIgnoreCase()) {
            val = val.toLowerCase();
            predicateValue = predicateValue.toLowerCase();
        }
        switch (predicate.getOperation()) {
            case CONTAINS:
                return val.contains(predicateValue);
            case EQUAL:
                return val.equals(predicateValue);
            case STARTS_WITH:
                return val.startsWith(predicateValue);
            case ENDS_WITH:
                return val.endsWith(predicateValue);
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            case NOT_CONTAINS:
                return !val.contains(predicateValue);
            case IN:
                return equalsAny(val, splitByCommaWithoutQuotes(predicateValue));
            case NOT_IN:
                return !equalsAny(val, splitByCommaWithoutQuotes(predicateValue));
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private <T> T getPredicateValue(DataSnapshot data, FilterPredicateValue<T> value, AlarmConditionFilter filter, Function<EntityKeyValue, T> transformFunction) {
        EntityKeyValue ekv = getDynamicPredicateValue(data, value.getDynamicValue());
        if (ekv != null) {
            T result = transformFunction.apply(ekv);
            if (result != null) {
                return result;
            }
        }
        if (filter.getKey().getType() != AlarmConditionKeyType.CONSTANT) {
            return value.getDefaultValue();
        } else {
            return null;
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private <T> EntityKeyValue getDynamicPredicateValue(DataSnapshot data, DynamicValue<T> value) {
        EntityKeyValue ekv = null;
        if (value != null) {
            switch (value.getSourceType()) {
                case CURRENT_DEVICE:
                    ekv = data.getValue(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, value.getSourceAttribute()));
                    if (ekv != null || !value.isInherit()) {
                        break;
                    }
                case CURRENT_CUSTOMER:
                    ekv = dynamicPredicateValueCtx.getCustomerValue(value.getSourceAttribute());
                    if (ekv != null || !value.isInherit()) {
                        break;
                    }
                case CURRENT_TENANT:
                    ekv = dynamicPredicateValueCtx.getTenantValue(value.getSourceAttribute());
            }
        }
        return ekv;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static String getStrValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue().toString() : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue().toString() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? ekv.getBoolValue().toString() : null;
            case STRING:
                return ekv.getStrValue();
            case JSON:
                return ekv.getJsonValue();
            default:
                return null;
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static Double getDblValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue().doubleValue() : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? (ekv.getBoolValue() ? 1.0 : 0.0) : null;
            case STRING:
                try {
                    return Double.parseDouble(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Double.parseDouble(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static Boolean getBoolValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue() > 0 : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue() > 0 : null;
            case BOOLEAN:
                return ekv.getBoolValue();
            case STRING:
                try {
                    return Boolean.parseBoolean(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Boolean.parseBoolean(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmRuleState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static Long getLongValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue();
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue().longValue() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? (ekv.getBoolValue() ? 1 : 0L) : null;
            case STRING:
                try {
                    return Long.parseLong(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Long.parseLong(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }
    /*
     * 本类总结：`AlarmRuleState` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
