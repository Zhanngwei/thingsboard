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

import org.junit.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * `AlarmStateTest` 测试类，用于验证 `AlarmState` 相关行为。
 */
public class AlarmStateTest {

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSetAlarmConditionMetadata_repeatingCondition() {
        AlarmRuleState ruleState = createMockAlarmRuleState(new RepeatingAlarmConditionSpec());
        int eventCount = 3;
        ruleState.getState().setEventCount(eventCount);

        AlarmState alarmState = createMockAlarmState();
        TbMsgMetaData metaData = new TbMsgMetaData();

        alarmState.setAlarmConditionMetadata(ruleState, metaData);

        assertEquals(AlarmConditionSpecType.REPEATING, ruleState.getSpec().getType());
        assertNotNull(metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
        assertNull(metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
        assertEquals(String.valueOf(eventCount), metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
    }

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSetAlarmConditionMetadata_durationCondition() {
        DurationAlarmConditionSpec spec = new DurationAlarmConditionSpec();
        spec.setUnit(TimeUnit.SECONDS);
        AlarmRuleState ruleState = createMockAlarmRuleState(spec);
        int duration = 12;
        ruleState.getState().setDuration(duration);

        AlarmState alarmState = createMockAlarmState();
        TbMsgMetaData metaData = new TbMsgMetaData();

        alarmState.setAlarmConditionMetadata(ruleState, metaData);

        assertEquals(AlarmConditionSpecType.DURATION, ruleState.getSpec().getType());
        assertNotNull(metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
        assertNull(metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
        assertEquals(String.valueOf(duration), metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `spec`：`spec` 参数。
     * 返回：处理结果。
     */
    private AlarmRuleState createMockAlarmRuleState(AlarmConditionSpec spec) {
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(spec);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);

        return new AlarmRuleState(null, alarmRule, null, null, null);
    }

    /**
     * 功能：保存或创建告警。
     * 参数：无。
     * 返回：处理结果。
     */
    private AlarmState createMockAlarmState() {
        return new AlarmState(null, null, mock(DeviceProfileAlarm.class), null, null);
    }
}
/*
 * 本类总结：{@code AlarmStateTest} 为 {@code AlarmState} 的 设备配置与告警状态组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
