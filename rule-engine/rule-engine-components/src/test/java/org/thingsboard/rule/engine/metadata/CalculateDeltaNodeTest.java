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
 * `CalculateDeltaNodeTest` 测试类，用于验证 `CalculateDeltaNode` 相关行为。
 */
@ExtendWith(MockitoExtension.class)
public class CalculateDeltaNodeTest {

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /**
     * 执行器常量，用于统一引用固定值。
     */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctxMock;
    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    @Mock
    private TimeseriesService timeseriesServiceMock;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private CalculateDeltaNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private CalculateDeltaNodeConfiguration config;
    /**
     * 节点实例，保存当前对象的配置选项。
     */
    private TbNodeConfiguration nodeConfiguration;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
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
     * 功能：验证 `givenDefaultConfig_whenDefaultConfiguration_thenVerify` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDefaultConfig_whenDefaultConfiguration_thenVerify() {
        assertEquals(config.getInputValueKey(), "pulseCounter");
        assertEquals(config.getOutputValueKey(), "delta");
        assertTrue(config.isUseCache());
        assertFalse(config.isAddPeriodBetweenMsgs());
        assertEquals(config.getPeriodValueKey(), "periodInMs");
        assertTrue(config.isTellFailureIfDeltaIsNegative());
    }

    /**
     * 功能：验证 `givenInvalidMsgType_whenOnMsg_thenShouldTellNextOther` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInvalidMsgType_whenOnMsg_thenShouldTellNextOther() {
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
     * 功能：验证 `givenInvalidMsgDataType_whenOnMsg_thenShouldTellNextOther` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInvalidMsgDataType_whenOnMsg_thenShouldTellNextOther() {
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
     * 功能：验证 `givenInputKeyIsNotPresent_whenOnMsg_thenShouldTellNextOther` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInputKeyIsNotPresent_whenOnMsg_thenShouldTellNextOther() {
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
     * 功能：验证 `givenDoubleValue_whenOnMsgAndCachingOff_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDoubleValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
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
     * 功能：验证 `givenLongStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenLongStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
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
     * 功能：验证 `givenValidStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenValidStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
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
     * 功能：验证 `givenTwoMessagesAndPeriodOnAndCachingOn_whenOnMsg_thenVerify` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenTwoMessagesAndPeriodOnAndCachingOn_whenOnMsg_thenVerify() throws TbNodeException {
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
     * 功能：验证 `givenLastValueIsNull_whenOnMsgAndCachingOff_thenDeltaShouldBeZero` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenLastValueIsNull_whenOnMsgAndCachingOff_thenDeltaShouldBeZero() throws TbNodeException {
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
     * 功能：验证 `givenNegativeDeltaAndTellFailureIfNegativeDeltaTrue_whenOnMsg_thenShouldTellFailure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNegativeDeltaAndTellFailureIfNegativeDeltaTrue_whenOnMsg_thenShouldTellFailure() throws TbNodeException {
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
     * 功能：验证 `givenNegativeDeltaAndTellFailureIfNegativeDeltaFalse_whenOnMsg_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNegativeDeltaAndTellFailureIfNegativeDeltaFalse_whenOnMsg_thenShouldTellSuccess() throws TbNodeException {
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
     * 功能：验证 `givenInvalidStringValue_whenOnMsg_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInvalidStringValue_whenOnMsg_thenException() {
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
     * 功能：验证 `givenBooleanValue_whenOnMsg_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenBooleanValue_whenOnMsg_thenException() {
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
     * 功能：验证 `givenJsonValue_whenOnMsg_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenJsonValue_whenOnMsg_thenException() {
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
     * 功能：执行 `mockFindLatest` 对应的处理。
     * 参数：
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：无。
     */
    private void mockFindLatest(TsKvEntry tsKvEntry) {
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatestSync(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of(tsKvEntry.getKey())))
        )).thenReturn(List.of(tsKvEntry));
    }

    /**
     * 功能：执行 `mockFindLatestAsync` 对应的处理。
     * 参数：
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：无。
     */
    private void mockFindLatestAsync(TsKvEntry tsKvEntry) {
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of(tsKvEntry.getKey())))
        )).thenReturn(Futures.immediateFuture(List.of(tsKvEntry)));
    }

    /**
     * `ListMatcher` 类，封装当前模块中的一组相关职责。
     */
    @RequiredArgsConstructor
    private static class ListMatcher<T> implements ArgumentMatcher<List<T>> {

        /**
         * `expectedList`列表，用于保存一组待处理对象。
         */
        private final List<T> expectedList;

        /**
         * 功能：执行 `matches` 对应的处理。
         * 参数：
         * - `actualList`：数据列表。
         * 返回：判断结果。
         */
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
