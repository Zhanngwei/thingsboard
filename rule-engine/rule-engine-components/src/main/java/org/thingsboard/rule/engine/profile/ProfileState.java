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

import lombok.AccessLevel;
import lombok.Getter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AlarmSchedule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * 中文说明：`ProfileState` 是配置状态辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
class ProfileState {

    /**
     * 字段说明：保存 `deviceProfile`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private DeviceProfile deviceProfile;
    @Getter(AccessLevel.PACKAGE)
    /**
     * 字段说明：保存 `alarmSettings`，表示告警类型、严重级别或详情，供本类方法在规则节点处理流程中使用。
     */
    private final List<DeviceProfileAlarm> alarmSettings = new CopyOnWriteArrayList<>();
    @Getter(AccessLevel.PACKAGE)
    /**
     * 字段说明：保存 `entityKeys`，表示消息体、元数据、属性或遥测中的键名，供本类方法在规则节点处理流程中使用。
     */
    private final Set<AlarmConditionFilterKey> entityKeys = ConcurrentHashMap.newKeySet();

    private final Map<String, Map<AlarmSeverity, Set<AlarmConditionFilterKey>>> alarmCreateKeys = new HashMap<>();
    private final Map<String, Set<AlarmConditionFilterKey>> alarmClearKeys = new HashMap<>();

    /**
     * 方法说明：构造 `ProfileState` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    ProfileState(DeviceProfile deviceProfile) {
        updateDeviceProfile(deviceProfile);
    }

    /**
     * 方法说明：执行 `updateDeviceProfile` 对应的辅助逻辑，供 `ProfileState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    void updateDeviceProfile(DeviceProfile deviceProfile) {
        this.deviceProfile = deviceProfile;
        alarmSettings.clear();
        alarmCreateKeys.clear();
        alarmClearKeys.clear();
        entityKeys.clear();
        if (deviceProfile.getProfileData().getAlarms() != null) {
            alarmSettings.addAll(deviceProfile.getProfileData().getAlarms());
            for (DeviceProfileAlarm alarm : deviceProfile.getProfileData().getAlarms()) {
                Map<AlarmSeverity, Set<AlarmConditionFilterKey>> createAlarmKeys = alarmCreateKeys.computeIfAbsent(alarm.getId(), id -> new HashMap<>());
                alarm.getCreateRules().forEach(((severity, alarmRule) -> {
                    var ruleKeys = createAlarmKeys.computeIfAbsent(severity, id -> new HashSet<>());
                    for (var keyFilter : alarmRule.getCondition().getCondition()) {
                        entityKeys.add(keyFilter.getKey());
                        ruleKeys.add(keyFilter.getKey());
                        addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, ruleKeys);
                    }
                    addEntityKeysFromAlarmConditionSpec(alarmRule);
                    AlarmSchedule schedule = alarmRule.getSchedule();
                    if (schedule != null) {
                        addScheduleDynamicValues(schedule);
                    }
                }));
                if (alarm.getClearRule() != null) {
                    var clearAlarmKeys = alarmClearKeys.computeIfAbsent(alarm.getId(), id -> new HashSet<>());
                    for (var keyFilter : alarm.getClearRule().getCondition().getCondition()) {
                        entityKeys.add(keyFilter.getKey());
                        clearAlarmKeys.add(keyFilter.getKey());
                        addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, clearAlarmKeys);
                    }
                    addEntityKeysFromAlarmConditionSpec(alarm.getClearRule());
                }
            }
        }
    }

    /**
     * 方法说明：向消息、元数据、集合或缓存追加数据，供 `ProfileState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void addScheduleDynamicValues(AlarmSchedule schedule) {
        DynamicValue<String> dynamicValue = schedule.getDynamicValue();
        if (dynamicValue != null) {
            entityKeys.add(
                    new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                            dynamicValue.getSourceAttribute())
            );
        }
    }

    /**
     * 方法说明：向消息、元数据、集合或缓存追加数据，供 `ProfileState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void addEntityKeysFromAlarmConditionSpec(AlarmRule alarmRule) {
        AlarmConditionSpec spec = alarmRule.getCondition().getSpec();
        if (spec == null) {
            return;
        }
        AlarmConditionSpecType specType = spec.getType();
        switch (specType) {
            case DURATION:
                DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
                if(duration.getPredicate().getDynamicValue() != null
                        && duration.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    duration.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
            case REPEATING:
                RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;
                if(repeating.getPredicate().getDynamicValue() != null
                        && repeating.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    repeating.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
        }

    }

    /**
     * 方法说明：向消息、元数据、集合或缓存追加数据，供 `ProfileState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void addDynamicValuesRecursively(KeyFilterPredicate predicate, Set<AlarmConditionFilterKey> entityKeys, Set<AlarmConditionFilterKey> ruleKeys) {
        switch (predicate.getType()) {
            case STRING:
            case NUMERIC:
            case BOOLEAN:
                DynamicValue value = ((SimpleKeyFilterPredicate) predicate).getValue().getDynamicValue();
                if (value != null && (value.getSourceType() == DynamicValueSourceType.CURRENT_TENANT ||
                        value.getSourceType() == DynamicValueSourceType.CURRENT_CUSTOMER ||
                        value.getSourceType() == DynamicValueSourceType.CURRENT_DEVICE)) {
                    AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, value.getSourceAttribute());
                    entityKeys.add(entityKey);
                    ruleKeys.add(entityKey);
                }
                break;
            case COMPLEX:
                for (KeyFilterPredicate child : ((ComplexFilterPredicate) predicate).getPredicates()) {
                    addDynamicValuesRecursively(child, entityKeys, ruleKeys);
                }
                break;
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `ProfileState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    DeviceProfileId getProfileId() {
        return deviceProfile.getId();
    }

    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `ProfileState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    Set<AlarmConditionFilterKey> getCreateAlarmKeys(String id, AlarmSeverity severity) {
        Map<AlarmSeverity, Set<AlarmConditionFilterKey>> sKeys = alarmCreateKeys.get(id);
        if (sKeys == null) {
            return Collections.emptySet();
        } else {
            Set<AlarmConditionFilterKey> keys = sKeys.get(severity);
            if (keys == null) {
                return Collections.emptySet();
            } else {
                return keys;
            }
        }
    }

    /**
     * 方法说明：清除告警或本地状态，供 `ProfileState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    Set<AlarmConditionFilterKey> getClearAlarmKeys(String id) {
        Set<AlarmConditionFilterKey> keys = alarmClearKeys.get(id);
        if (keys == null) {
            return Collections.emptySet();
        } else {
            return keys;
        }
    }
    /*
     * 本类总结：`ProfileState` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
