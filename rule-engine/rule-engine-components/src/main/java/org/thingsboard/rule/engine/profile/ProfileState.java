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
 * 中文说明：
 * 1. `ProfileState` 是 ThingsBoard Rule Engine Components 中承载 `Profile State` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
class ProfileState {

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    private DeviceProfile deviceProfile;
    /**
     * 告警列表，用于保存一组待处理对象。
     */
    @Getter(AccessLevel.PACKAGE)
    private final List<DeviceProfileAlarm> alarmSettings = new CopyOnWriteArrayList<>();
    /**
     * 实体集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter(AccessLevel.PACKAGE)
    private final Set<AlarmConditionFilterKey> entityKeys = ConcurrentHashMap.newKeySet();

    private final Map<String, Map<AlarmSeverity, Set<AlarmConditionFilterKey>>> alarmCreateKeys = new HashMap<>();
    private final Map<String, Set<AlarmConditionFilterKey>> alarmClearKeys = new HashMap<>();

    /**
     * 功能：创建 `ProfileState` 实例，并初始化必要字段。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    ProfileState(DeviceProfile deviceProfile) {
        updateDeviceProfile(deviceProfile);
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
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
     * 功能：保存或创建`Schedule Dynamic Values`。
     * 参数：
     * - `schedule`：`schedule` 参数。
     * 返回：无。
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
     * 功能：保存或创建实体。
     * 参数：
     * - `alarmRule`：`alarmRule` 参数。
     * 返回：无。
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
     * 功能：保存或创建`Dynamic Values Recursively`。
     * 参数：
     * - `predicate`：`predicate` 参数。
     * - `entityKeys`：实体对象。
     * - `ruleKeys`：键。
     * 返回：无。
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
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    DeviceProfileId getProfileId() {
        return deviceProfile.getId();
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `id`：`id`ID。
     * - `severity`：`severity` 参数。
     * 返回：匹配的数据集合。
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
     * 功能：获取告警。
     * 参数：
     * - `id`：`id`ID。
     * 返回：匹配的数据集合。
     */
    Set<AlarmConditionFilterKey> getClearAlarmKeys(String id) {
        Set<AlarmConditionFilterKey> keys = alarmClearKeys.get(id);
        if (keys == null) {
            return Collections.emptySet();
        } else {
            return keys;
        }
    }
}
