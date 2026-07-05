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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `TbGetTenantAttributeNodeTest` 测试类，用于验证 `TbGetTenantAttributeNode` 相关行为。
 */
@ExtendWith(MockitoExtension.class)
public class TbGetTenantAttributeNodeTest {

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
    private static final TestDbCallbackExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctxMock;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    private AttributesService attributesServiceMock;
    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    @Mock
    private TimeseriesService timeseriesServiceMock;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbGetTenantAttributeNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbGetEntityDataNodeConfiguration config;
    /**
     * 节点实例，保存当前对象的配置选项。
     */
    private TbNodeConfiguration nodeConfiguration;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private TbMsg msg;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setUp() {
        node = new TbGetTenantAttributeNode();
        config = new TbGetEntityDataNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
    }

    /**
     * 功能：验证 `givenConfigWithNullFetchTo_whenInit_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenConfigWithNullFetchTo_whenInit_thenException() {
        // GIVEN
        config.setFetchTo(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 功能：验证 `givenConfigWithNullDataToFetch_whenInit_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenConfigWithNullDataToFetch_whenInit_thenException() {
        // GIVEN
        config.setDataToFetch(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("DataToFetch property has invalid value: null. Only ATTRIBUTES and LATEST_TELEMETRY values supported!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 功能：验证 `givenConfigWithUnsupportedDataToFetch_whenInit_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenConfigWithUnsupportedDataToFetch_whenInit_thenException() {
        // GIVEN
        config.setDataToFetch(DataToFetch.FIELDS);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("DataToFetch property has invalid value: FIELDS. Only ATTRIBUTES and LATEST_TELEMETRY values supported!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 功能：验证 `givenDefaultConfig_whenInit_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDefaultConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDataMapping()).isEqualTo(Map.of("alarmThreshold", "threshold"));
        assertThat(config.getDataToFetch()).isEqualTo(DataToFetch.ATTRIBUTES);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.METADATA);
    }

    /**
     * 功能：验证 `givenCustomConfig_whenInit_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenCustomConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN
        config.setDataMapping(Map.of(
                "sourceAttr1", "targetKey1",
                "sourceAttr2", "targetKey2",
                "sourceAttr3", "targetKey3"));
        config.setDataToFetch(DataToFetch.LATEST_TELEMETRY);
        config.setFetchTo(TbMsgSource.DATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDataMapping()).isEqualTo(Map.of(
                "sourceAttr1", "targetKey1",
                "sourceAttr2", "targetKey2",
                "sourceAttr3", "targetKey3"));
        assertThat(config.getDataToFetch()).isEqualTo(DataToFetch.LATEST_TELEMETRY);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.DATA);
    }

    /**
     * 功能：验证 `givenEmptyAttributesMapping_whenInit_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenEmptyAttributesMapping_whenInit_thenException() {
        // GIVEN
        var expectedExceptionMessage = "At least one mapping entry should be specified!";

        config.setDataMapping(Collections.emptyMap());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 功能：验证 `givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException() {
        // GIVEN
        node.fetchTo = TbMsgSource.DATA;
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 功能：验证 `givenFetchAttributesToData_whenOnMsg_thenShouldFetchAttributesToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchAttributesToData_whenOnMsg_thenShouldFetchAttributesToData() {
        // GIVEN
        var deviceId = new DeviceId(UUID.randomUUID());

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.ATTRIBUTES, deviceId);

        List<AttributeKvEntry> attributesList = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey1", "sourceValue1"), 1L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey2", "sourceValue2"), 2L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey3", "sourceValue3"), 3L)
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(eq(TENANT_ID), eq(TENANT_ID), eq(DataConstants.SERVER_SCOPE), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(attributesList));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42," +
                "\"humidity\":77," +
                "\"messageBodyPattern1\":\"targetKey2\"," +
                "\"messageBodyPattern2\":\"sourceKey3\"," +
                "\"targetKey1\":\"sourceValue1\"," +
                "\"targetKey2\":\"sourceValue2\"," +
                "\"targetKey3\":\"sourceValue3\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 功能：验证 `givenFetchAttributesToMetaData_whenOnMsg_thenShouldFetchAttributesToMetaData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchAttributesToMetaData_whenOnMsg_thenShouldFetchAttributesToMetaData() {
        // GIVEN
        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.ATTRIBUTES, TENANT_ID);

        List<AttributeKvEntry> attributesList = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey1", "sourceValue1"), 1L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey2", "sourceValue2"), 2L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey3", "sourceValue3"), 3L)
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(eq(TENANT_ID), eq(TENANT_ID), eq(DataConstants.SERVER_SCOPE), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(attributesList));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "metaDataPattern1", "sourceKey2",
                "metaDataPattern2", "targetKey3",
                "targetKey1", "sourceValue1",
                "targetKey2", "sourceValue2",
                "targetKey3", "sourceValue3"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    /**
     * 功能：验证 `givenFetchTelemetryToData_whenOnMsg_thenShouldFetchTelemetryToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchTelemetryToData_whenOnMsg_thenShouldFetchTelemetryToData() {
        // GIVEN
        var customerId = new CustomerId(UUID.randomUUID());

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.LATEST_TELEMETRY, customerId);

        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey1", "sourceValue1")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey2", "sourceValue2")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey3", "sourceValue3"))
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(timeseriesServiceMock.findLatest(eq(TENANT_ID), eq(TENANT_ID), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(timeseries));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42," +
                "\"humidity\":77," +
                "\"messageBodyPattern1\":\"targetKey2\"," +
                "\"messageBodyPattern2\":\"sourceKey3\"," +
                "\"targetKey1\":\"sourceValue1\"," +
                "\"targetKey2\":\"sourceValue2\"," +
                "\"targetKey3\":\"sourceValue3\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 功能：验证 `givenFetchTelemetryToMetaData_whenOnMsg_thenShouldFetchTelemetryToMetaData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchTelemetryToMetaData_whenOnMsg_thenShouldFetchTelemetryToMetaData() {
        // GIVEN
        var ruleChainId = new RuleChainId(UUID.randomUUID());

        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.LATEST_TELEMETRY, ruleChainId);

        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey1", "sourceValue1")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey2", "sourceValue2")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey3", "sourceValue3"))
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(timeseriesServiceMock.findLatest(eq(TENANT_ID), eq(TENANT_ID), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(timeseries));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "metaDataPattern1", "sourceKey2",
                "metaDataPattern2", "targetKey3",
                "targetKey1", "sourceValue1",
                "targetKey2", "sourceValue2",
                "targetKey3", "sourceValue3"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    /**
     * 功能：验证 `givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetEntityDataNodeConfiguration().defaultConfiguration();
        var node = new TbGetTenantAttributeNode();
        String oldConfig = "{\"attrMapping\":{\"alarmThreshold\":\"threshold\"},\"telemetry\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 功能：执行 `prepareMsgAndConfig` 对应的处理。
     * 参数：
     * - `fetchTo`：`fetchTo` 参数。
     * - `dataToFetch`：待处理数据。
     * - `originator`：`originator` 参数。
     * 返回：无。
     */
    private void prepareMsgAndConfig(TbMsgSource fetchTo, DataToFetch dataToFetch, EntityId originator) {
        config.setDataMapping(Map.of(
                "sourceKey1", "targetKey1",
                "${metaDataPattern1}", "$[messageBodyPattern1]",
                "$[messageBodyPattern2]", "${metaDataPattern2}"));
        config.setDataToFetch(dataToFetch);
        config.setFetchTo(fetchTo);

        node.config = config;
        node.fetchTo = fetchTo;

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("metaDataPattern1", "sourceKey2");
        msgMetaData.putValue("metaDataPattern2", "targetKey3");

        var msgData = "{\"temp\":42,\"humidity\":77,\"messageBodyPattern1\":\"targetKey2\",\"messageBodyPattern2\":\"sourceKey3\"}";

        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, msgMetaData, msgData);
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
 * 本类总结：{@code TbGetTenantAttributeNodeTest} 为 {@code TbGetTenantAttributeNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
