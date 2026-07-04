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
 * 测试目标：验证 {@code AlarmStateTest} 覆盖的 设备配置与告警状态组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code AlarmState}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class AlarmStateTest {

    /**
     * 测试方法：覆盖 {@code testSetAlarmConditionMetadata_repeatingCondition} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void testSetAlarmConditionMetadata_repeatingCondition() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code testSetAlarmConditionMetadata_durationCondition} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void testSetAlarmConditionMetadata_durationCondition() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 辅助方法：{@code createMockAlarmRuleState} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private AlarmRuleState createMockAlarmRuleState(AlarmConditionSpec spec) {
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(spec);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);

        return new AlarmRuleState(null, alarmRule, null, null, null);
    }

    /**
     * 辅助方法：{@code createMockAlarmState} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private AlarmState createMockAlarmState() {
        return new AlarmState(null, null, mock(DeviceProfileAlarm.class), null, null);
    }
}
/*
 * 本类总结：{@code AlarmStateTest} 为 {@code AlarmState} 的 设备配置与告警状态组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
