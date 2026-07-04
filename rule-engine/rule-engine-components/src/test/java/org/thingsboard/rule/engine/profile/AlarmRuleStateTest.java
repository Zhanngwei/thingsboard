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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.StringFilterPredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 测试目标：验证 {@code AlarmRuleStateTest} 覆盖的 设备配置与告警状态组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code AlarmRuleState}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：内存 fixture、参数化数据源，以及测试体按需创建的 Mockito mock/spy；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class AlarmRuleStateTest {

    /** 参数源方法：{@code testEvalCondition} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> testEvalCondition() {
        return Stream.of(
                Arguments.of(StringFilterPredicate.StringOperation.IN, "test,value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.IN, "test,value", "teeeeest", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_IN, "test,value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_IN, "test,value", "teeeeest", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.CONTAINS, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.CONTAINS, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_CONTAINS, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_CONTAINS, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.EQUAL, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.EQUAL, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_EQUAL, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_EQUAL, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.ENDS_WITH, "test value", "some test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.ENDS_WITH, "test value", "some test value2", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.STARTS_WITH, "test value", "test value attribute", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.STARTS_WITH, "test value", "test", AlarmEvalResult.FALSE)
        );
    }

    /**
     * 测试方法：覆盖 {@code testEvalCondition} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @ParameterizedTest
    @MethodSource
    public void testEvalCondition(StringFilterPredicate.StringOperation operation, String predicateValue, String attributeValue, AlarmEvalResult evalResult) {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
            AlarmConditionFilterKey alarmConditionFilterKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "stringKey");

            StringFilterPredicate predicate = new StringFilterPredicate();
            predicate.setOperation(operation);
            predicate.setValue(new FilterPredicateValue<>(predicateValue));

            AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
            alarmConditionFilter.setKey(alarmConditionFilterKey);
            alarmConditionFilter.setPredicate(predicate);
            alarmConditionFilter.setValueType(EntityKeyValueType.STRING);

            List<AlarmConditionFilter> condition = new ArrayList<>();
            condition.add(alarmConditionFilter);

            AlarmCondition alarmCondition = new AlarmCondition();
            alarmCondition.setSpec(new SimpleAlarmConditionSpec());
            alarmCondition.setCondition(condition);

            AlarmRule alarmRule = new AlarmRule();
            alarmRule.setCondition(alarmCondition);

            AlarmRuleState alarmRuleState = new AlarmRuleState(null, alarmRule, null, null, null);

            Set<AlarmConditionFilterKey> entityKeys = new HashSet<>(List.of(alarmConditionFilterKey));
            DataSnapshot result = new DataSnapshot(entityKeys);
            result.putValue(alarmConditionFilterKey, System.currentTimeMillis(), EntityKeyValue.fromString(attributeValue));
            Assertions.assertEquals(evalResult, alarmRuleState.eval(result));
    }
}
/*
 * 本类总结：{@code AlarmRuleStateTest} 为 {@code AlarmRuleState} 的 设备配置与告警状态组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
