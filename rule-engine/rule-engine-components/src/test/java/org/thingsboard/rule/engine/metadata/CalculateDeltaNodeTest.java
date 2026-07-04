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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code CalculateDeltaNodeTest} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code CalculateDeltaNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class CalculateDeltaNodeTest {

    /** 测试常量字段：{@code DUMMY_DEVICE_ORIGINATOR} 保存 {@code DeviceId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code timeseriesServiceMock} 保存 {@code TimeseriesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TimeseriesService timeseriesServiceMock;
    /** 可变 fixture 字段：{@code node} 保存 {@code CalculateDeltaNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private CalculateDeltaNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code CalculateDeltaNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private CalculateDeltaNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbNodeConfiguration nodeConfiguration;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new CalculateDeltaNode();
        config = new CalculateDeltaNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);

        node.init(ctxMock, nodeConfiguration);
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenDefaultConfiguration_thenVerify} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDefaultConfig_whenDefaultConfiguration_thenVerify() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        assertEquals(config.getInputValueKey(), "pulseCounter");
        assertEquals(config.getOutputValueKey(), "delta");
        assertTrue(config.isUseCache());
        assertFalse(config.isAddPeriodBetweenMsgs());
        assertEquals(config.getPeriodValueKey(), "periodInMs");
        assertTrue(config.isTellFailureIfDeltaIsNegative());
    }

    /**
     * 测试方法：覆盖 {@code givenInvalidMsgType_whenOnMsg_thenShouldTellNextOther} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenInvalidMsgType_whenOnMsg_thenShouldTellNextOther() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var msgData = "{\"pulseCounter\": 42}";
        var msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock, times(1)).tellNext(eq(msg), eq(TbNodeConnectionType.OTHER));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }

    /**
     * 测试方法：覆盖 {@code givenInvalidMsgDataType_whenOnMsg_thenShouldTellNextOther} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenInvalidMsgDataType_whenOnMsg_thenShouldTellNextOther() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock, times(1)).tellNext(eq(msg), eq(TbNodeConnectionType.OTHER));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }


    /**
     * 测试方法：覆盖 {@code givenInputKeyIsNotPresent_whenOnMsg_thenShouldTellNextOther} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenInputKeyIsNotPresent_whenOnMsg_thenShouldTellNextOther() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock, times(1)).tellNext(eq(msg), eq(TbNodeConnectionType.OTHER));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }

    /**
     * 测试方法：覆盖 {@code givenDoubleValue_whenOnMsgAndCachingOff_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDoubleValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setRound(1);
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 40.5)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":1.5}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    /**
     * 测试方法：覆盖 {@code givenLongStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenLongStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("temperature", 40L)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    /**
     * 测试方法：覆盖 {@code givenValidStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenValidStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("temperature", "40.0")));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    /**
     * 测试方法：覆盖 {@code givenTwoMessagesAndPeriodOnAndCachingOn_whenOnMsg_thenVerify} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenTwoMessagesAndPeriodOnAndCachingOn_whenOnMsg_thenVerify() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // STAGE 1
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setPeriodValueKey("ts_delta");
        config.setAddPeriodBetweenMsgs(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatest(new BasicTsKvEntry(1L, new DoubleDataEntry("temperature", 40.0)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var firstMsgMetaData = new TbMsgMetaData();
        firstMsgMetaData.putValue("ts", String.valueOf(3L));
        var firstMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, firstMsgMetaData, msgData);

        // WHEN
        node.onMsg(ctxMock, firstMsg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2,\"ts_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());

        // STAGE 2
        // GIVEN
        reset(ctxMock);
        reset(timeseriesServiceMock);

        var secondMsgMetaData = new TbMsgMetaData();
        secondMsgMetaData.putValue("ts", String.valueOf(6L));
        var secondMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, secondMsgMetaData, msgData);

        // WHEN
        node.onMsg(ctxMock, secondMsg);

        // THEN
        actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(timeseriesServiceMock, never()).findLatest(any(), any(), anyList());
        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":0,\"ts_delta\":3}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    /**
     * 测试方法：覆盖 {@code givenLastValueIsNull_whenOnMsgAndCachingOff_thenDeltaShouldBeZero} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenLastValueIsNull_whenOnMsgAndCachingOff_thenDeltaShouldBeZero() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", null)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":0}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    /**
     * 测试方法：覆盖 {@code givenNegativeDeltaAndTellFailureIfNegativeDeltaTrue_whenOnMsg_thenShouldTellFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenNegativeDeltaAndTellFailureIfNegativeDeltaTrue_whenOnMsg_thenShouldTellFailure() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setTellFailureIfDeltaIsNegative(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatest(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("pulseCounter", 200L)));

        var msgData = "{\"pulseCounter\":\"123\"}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(ctxMock, times(1)).tellFailure(actualMsgCaptor.capture(), actualExceptionCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        var expectedExceptionMsg = "Delta value is negative!";
        var actualException = actualExceptionCaptor.getValue();

        assertEquals(msg, actualMsgCaptor.getValue());
        assertInstanceOf(IllegalArgumentException.class, actualException);
        assertEquals(expectedExceptionMsg, actualException.getMessage());
    }

    /**
     * 测试方法：覆盖 {@code givenNegativeDeltaAndTellFailureIfNegativeDeltaFalse_whenOnMsg_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenNegativeDeltaAndTellFailureIfNegativeDeltaFalse_whenOnMsg_thenShouldTellSuccess() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setTellFailureIfDeltaIsNegative(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatest(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("pulseCounter", 200L)));

        var msgData = "{\"pulseCounter\":\"123\"}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        String expectedMsgData = "{\"pulseCounter\":\"123\",\"delta\":-77}";
        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    /**
     * 测试方法：覆盖 {@code givenInvalidStringValue_whenOnMsg_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenInvalidStringValue_whenOnMsg_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        mockFindLatest(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("pulseCounter", "high")));

        var msgData = "{\"pulseCounter\":\"123\"}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN-THEN
        Assertions.assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculation failed. Unable to parse value [high] of telemetry [pulseCounter] to Double");
    }

    /**
     * 测试方法：覆盖 {@code givenBooleanValue_whenOnMsg_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenBooleanValue_whenOnMsg_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        mockFindLatest(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry("pulseCounter", false)));

        var msgData = "{\"pulseCounter\":true}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN-THEN
        Assertions.assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculation failed. Boolean values are not supported!");
    }

    /**
     * 测试方法：覆盖 {@code givenJsonValue_whenOnMsg_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenJsonValue_whenOnMsg_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        mockFindLatest(new BasicTsKvEntry(System.currentTimeMillis(), new JsonDataEntry("pulseCounter", "{\"isActive\":false}")));

        var msgData = "{\"pulseCounter\":{\"isActive\":true}}";
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, msgData);

        // WHEN-THEN
        Assertions.assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculation failed. JSON values are not supported!");
    }

    /**
     * 辅助方法：{@code mockFindLatest} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void mockFindLatest(TsKvEntry tsKvEntry) {
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatestSync(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of(tsKvEntry.getKey())))
        )).thenReturn(List.of(tsKvEntry));
    }

    /**
     * 辅助方法：{@code mockFindLatestAsync} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void mockFindLatestAsync(TsKvEntry tsKvEntry) {
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of(tsKvEntry.getKey())))
        )).thenReturn(Futures.immediateFuture(List.of(tsKvEntry)));
    }

    /**
     * 测试目标：验证 {@code ListMatcher} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
     * 所属生产节点/组件：{@code ListMatcher}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
     * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
     * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
     * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
     */
    @RequiredArgsConstructor
    private static class ListMatcher<T> implements ArgumentMatcher<List<T>> {

        /** 固定 fixture 字段：{@code expectedList} 保存 {@code List<T>} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
        private final List<T> expectedList;

        /** 实现方法：{@code matches} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
        @Override
        public boolean matches(List<T> actualList) {
            if (actualList == expectedList) {
                return true;
            }
            if (actualList.size() != expectedList.size()) {
                return false;
            }
            return actualList.containsAll(expectedList);
        }

    }

}
/*
 * 本类总结：{@code CalculateDeltaNodeTest} 为 {@code CalculateDeltaNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
