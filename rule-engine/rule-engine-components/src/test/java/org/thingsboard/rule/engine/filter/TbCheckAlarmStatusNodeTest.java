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
 * `TbCheckAlarmStatusNodeTest` 测试类，用于验证 `TbCheckAlarmStatusNode` 相关行为。
 */
class TbCheckAlarmStatusNodeTest {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /**
     * 设备ID常量，用于统一引用固定值。
     */
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    /**
     * 告警ID常量，用于统一引用固定值。
     */
    private static final AlarmId ALARM_ID = new AlarmId(UUID.randomUUID());
    /**
     * 执行器常量，用于统一引用固定值。
     */
    private static final TestDbCallbackExecutor DB_EXECUTOR = new TestDbCallbackExecutor();

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbCheckAlarmStatusNode node;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    private RuleEngineAlarmService alarmService;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
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
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    /**
     * 功能：验证 `givenActiveAlarm_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenActiveAlarm_whenOnMsg_then_True() throws TbNodeException {
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
     * 功能：验证 `givenClearedAlarm_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenClearedAlarm_whenOnMsg_then_False() throws TbNodeException {
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
     * 功能：验证 `givenDeletedAlarm_whenOnMsg_then_Failure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDeletedAlarm_whenOnMsg_then_Failure() throws TbNodeException {
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
     * 功能：验证 `givenUnparseableAlarm_whenOnMsg_then_Failure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenUnparseableAlarm_whenOnMsg_then_Failure() {
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
     * 功能：获取消息。
     * 参数：
     * - `msgData`：待处理消息。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(String msgData) {
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, msgData);
    }

}
