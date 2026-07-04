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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbAlarmNodeTest} 覆盖的 动作节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbAlarmNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbAlarmNodeTest {

    /** 可变 fixture 字段：{@code node} 保存 {@code TbAbstractAlarmNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbAbstractAlarmNode node;

    /** Mock 依赖字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctx;
    /** Mock 依赖字段：{@code alarmService} 保存 {@code RuleEngineAlarmService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineAlarmService alarmService;

    /** Mock 依赖字段：{@code detailsJs} 保存 {@code ScriptEngine} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private ScriptEngine detailsJs;

    /** 参数捕获字段：{@code successCaptor} 保存 {@code ArgumentCaptor<Runnable>} 测试数据或依赖，来源：由 Mockito 注解创建，用于捕获被测逻辑传给 Mock 的参数，生命周期随单个测试实例。 */
    @Captor
    private ArgumentCaptor<Runnable> successCaptor;
    /** 参数捕获字段：{@code failureCaptor} 保存 {@code ArgumentCaptor<Consumer<Throwable>>} 测试数据或依赖，来源：由 Mockito 注解创建，用于捕获被测逻辑传给 Mock 的参数，生命周期随单个测试实例。 */
    @Captor
    private ArgumentCaptor<Consumer<Throwable>> failureCaptor;

    /** 固定 fixture 字段：{@code ruleChainId} 保存 {@code RuleChainId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    /** 固定 fixture 字段：{@code ruleNodeId} 保存 {@code RuleNodeId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    /** 可变 fixture 字段：{@code dbExecutor} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private ListeningExecutor dbExecutor;

    /** 固定 fixture 字段：{@code originator} 保存 {@code EntityId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final EntityId originator = new DeviceId(Uuids.timeBased());
    /** 固定 fixture 字段：{@code alarmOriginator} 保存 {@code EntityId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final EntityId alarmOriginator = new AlarmId(Uuids.timeBased());
    /** 固定 fixture 字段：{@code tenantId} 保存 {@code TenantId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    /** 固定 fixture 字段：{@code metaData} 保存 {@code TbMsgMetaData} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final TbMsgMetaData metaData = new TbMsgMetaData();
    /** 固定 fixture 字段：{@code rawJson} 保存 {@code String} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

    /**
     * 生命周期方法：{@code before} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Before
    public void before() {
        dbExecutor = new TestDbCallbackExecutor();
    }

    /**
     * 测试方法：覆盖 {@code newAlarmCanBeCreated} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void newAlarmCanBeCreated() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 测试方法：覆盖 {@code buildDetailsThrowsException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void buildDetailsThrowsException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFailedFuture(new NotImplementedException("message")));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);

        node.onMsg(ctx, msg);

        verifyError(msg, "message", NotImplementedException.class);

        verify(ctx).createScriptEngine(ScriptLanguage.JS, "DETAILS");
        verify(ctx).getAlarmService();
        verify(ctx, times(2)).getDbCallbackExecutor();
        verify(ctx).logJsEvalRequest();
        verify(ctx).getTenantId();
        verify(alarmService).findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType");

        verifyNoMoreInteractions(ctx, alarmService);
    }

    /**
     * 测试方法：覆盖 {@code ifAlarmClearedCreateNew} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void ifAlarmClearedCreateNew() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm clearedAlarm = Alarm.builder().cleared(true).acknowledged(true).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(clearedAlarm);

        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());


        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 测试方法：覆盖 {@code alarmCanBeUpdated} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void alarmCanBeUpdated() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(activeAlarm);

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .endTs(activeAlarm.getEndTs())
                .build();
        when(alarmService.updateAlarm(any(AlarmUpdateRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .modified(true)
                        .old(new Alarm(activeAlarm))
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());
        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Updated"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_EXISTING_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertTrue(activeAlarm.getEndTs() >= oldEndDate);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 测试方法：覆盖 {@code alarmCanBeCleared} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void alarmCanBeCleared() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .cleared(true)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(activeAlarm);
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 测试方法：覆盖 {@code alarmCanBeClearedWithAlarmOriginator} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void alarmCanBeClearedWithAlarmOriginator() throws ScriptException, IOException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, alarmOriginator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        AlarmId id = new AlarmId(alarmOriginator.getId());
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();
        activeAlarm.setId(id);

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .cleared(true)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();
        expectedAlarm.setId(id);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findAlarmById(tenantId, id)).thenReturn(activeAlarm);
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(alarmOriginator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 测试方法：覆盖 {@code testCreateAlarmWithDynamicSeverityFromMessageBody} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testCreateAlarmWithDynamicSeverityFromMessageBody() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setSeverity("$[alarmSeverity]");
        config.setAlarmType("SomeType");
        config.setScriptLang(ScriptLanguage.JS);
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        String rawJson = "{\"alarmSeverity\": \"WARNING\", \"passed\": 5}";
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 测试方法：覆盖 {@code testCreateAlarmWithDynamicSeverityFromMetadata} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testCreateAlarmWithDynamicSeverityFromMetadata() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setScriptLang(ScriptLanguage.JS);
        config.setSeverity("${alarmSeverity}");
        config.setAlarmType("SomeType");
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        metaData.putValue("alarmSeverity", "WARNING");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 测试方法：覆盖 {@code testCreateAlarmsWithPropagationToTenantWithDynamicTypes} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testCreateAlarmsWithPropagationToTenantWithDynamicTypes() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        for (int i = 0; i < 10; i++) {
            var config = new TbCreateAlarmNodeConfiguration();
            config.setPropagateToTenant(true);
            config.setSeverity(AlarmSeverity.CRITICAL.name());
            config.setAlarmType("SomeType" + i);
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            config.setDynamicSeverity(true);
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);

            metaData.putValue("key", "value");
            TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
            long ts = msg.getTs();
            Alarm expectedAlarm = Alarm.builder()
                    .startTs(ts)
                    .endTs(ts)
                    .tenantId(tenantId)
                    .originator(originator)
                    .severity(AlarmSeverity.CRITICAL)
                    .propagateToTenant(true)
                    .type("SomeType" + i)
                    .details(null)
                    .build();

            when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
            when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType" + i)).thenReturn(null);
            when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                    AlarmApiCallResult.builder()
                            .successful(true)
                            .created(true)
                            .alarm(new AlarmInfo(expectedAlarm))
                            .build());
            node.onMsg(ctx, msg);

            verify(ctx, atMost(10)).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
            successCaptor.getValue().run();
            verify(ctx, atMost(10)).tellNext(any(), eq("Created"));

            ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
            ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
            ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
            ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
            verify(ctx, atMost(10)).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

            assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
            assertEquals(originator, originatorCaptor.getValue());
            assertEquals("value", metadataCaptor.getValue().getValue("key"));
            assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
            assertNotSame(metaData, metadataCaptor.getValue());

            Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
            assertEquals(expectedAlarm, actualAlarm);
        }
    }

    /**
     * 辅助方法：{@code initWithCreateAlarmScript} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void initWithCreateAlarmScript() {
        try {
            TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
            config.setPropagate(true);
            config.setSeverity(AlarmSeverity.CRITICAL.name());
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 辅助方法：{@code initWithClearAlarmScript} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void initWithClearAlarmScript() {
        try {
            TbClearAlarmNodeConfiguration config = new TbClearAlarmNodeConfiguration();
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbClearAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 辅助方法：{@code verifyError} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }

}
/*
 * 本类总结：{@code TbAlarmNodeTest} 为 {@code TbAlarmNode} 的 动作节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
