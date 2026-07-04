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
package org.thingsboard.rule.engine.filter;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbCheckAlarmStatusNodeTest} 覆盖的 过滤/分流节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbCheckAlarmStatusNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
class TbCheckAlarmStatusNodeTest {

    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /** 测试常量字段：{@code DEVICE_ID} 保存 {@code DeviceId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    /** 测试常量字段：{@code ALARM_ID} 保存 {@code AlarmId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final AlarmId ALARM_ID = new AlarmId(UUID.randomUUID());
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code TestDbCallbackExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TestDbCallbackExecutor DB_EXECUTOR = new TestDbCallbackExecutor();

    /** 可变 fixture 字段：{@code node} 保存 {@code TbCheckAlarmStatusNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbCheckAlarmStatusNode node;

    /** 可变 fixture 字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbContext ctx;
    /** 可变 fixture 字段：{@code alarmService} 保存 {@code RuleEngineAlarmService} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RuleEngineAlarmService alarmService;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() throws TbNodeException {
        var config = new TbCheckAlarmStatusNodeConfig().defaultConfiguration();

        ctx = mock(TbContext.class);
        alarmService = mock(RuleEngineAlarmService.class);

        when(ctx.getTenantId()).thenReturn(TENANT_ID);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        node = new TbCheckAlarmStatusNode();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    /**
     * 生命周期方法：{@code tearDown} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    /**
     * 测试方法：覆盖 {@code givenActiveAlarm_whenOnMsg_then_True} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenActiveAlarm_whenOnMsg_then_True() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var alarm = new Alarm();
        alarm.setId(ALARM_ID);
        alarm.setOriginator(DEVICE_ID);
        alarm.setType("General Alarm");

        String msgData = JacksonUtil.toString(alarm);
        TbMsg msg = getTbMsg(msgData);

        when(alarmService.findAlarmByIdAsync(TENANT_ID, ALARM_ID)).thenReturn(Futures.immediateFuture(alarm));

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenClearedAlarm_whenOnMsg_then_False} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenClearedAlarm_whenOnMsg_then_False() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var alarm = new Alarm();
        alarm.setId(ALARM_ID);
        alarm.setOriginator(DEVICE_ID);
        alarm.setType("General Alarm");
        alarm.setCleared(true);

        String msgData = JacksonUtil.toString(alarm);
        TbMsg msg = getTbMsg(msgData);

        when(alarmService.findAlarmByIdAsync(TENANT_ID, ALARM_ID)).thenReturn(Futures.immediateFuture(alarm));

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenDeletedAlarm_whenOnMsg_then_Failure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDeletedAlarm_whenOnMsg_then_Failure() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var alarm = new Alarm();
        alarm.setId(ALARM_ID);
        alarm.setOriginator(DEVICE_ID);
        alarm.setType("General Alarm");
        alarm.setCleared(true);

        String msgData = JacksonUtil.toString(alarm);
        TbMsg msg = getTbMsg(msgData);

        when(alarmService.findAlarmByIdAsync(TENANT_ID, ALARM_ID)).thenReturn(Futures.immediateFuture(null));

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), throwableCaptor.capture());
        verify(ctx, never()).tellSuccess(any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
        Throwable value = throwableCaptor.getValue();
        assertThat(value).isInstanceOf(TbNodeException.class).hasMessage("No such alarm found.");
    }

    /**
     * 测试方法：覆盖 {@code givenUnparseableAlarm_whenOnMsg_then_Failure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenUnparseableAlarm_whenOnMsg_then_Failure() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        String msgData = "{\"Number\":1113718,\"id\":8.1}";
        TbMsg msg = getTbMsg(msgData);
        willReturn("Default Rule Chain").given(ctx).getRuleChainName();

        assertThatThrownBy(() -> node.onMsg(ctx, msg))
                .as("onMsg")
                .isInstanceOf(TbNodeException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessage("java.lang.IllegalArgumentException: The given string value cannot be transformed to Json object: {\"Number\":1113718,\"id\":8.1}");
    }

    /**
     * 辅助方法：{@code getTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getTbMsg(String msgData) {
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, msgData);
    }

}
/*
 * 本类总结：{@code TbCheckAlarmStatusNodeTest} 为 {@code TbCheckAlarmStatusNode} 的 过滤/分流节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
