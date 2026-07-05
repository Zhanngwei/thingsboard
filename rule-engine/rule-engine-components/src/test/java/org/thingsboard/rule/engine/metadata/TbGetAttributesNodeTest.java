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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `TbGetAttributesNodeTest` 测试类，用于验证 `TbGetAttributesNode` 相关行为。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbGetAttributesNodeTest {

    /**
     * `ORIGINATOR`常量，用于统一引用固定值。
     */
    private static final EntityId ORIGINATOR = new DeviceId(Uuids.timeBased());
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final TenantId TENANT_ID = TenantId.fromUUID(Uuids.timeBased());
    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    private AbstractListeningExecutor dbExecutor;

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
     * 客户端列表，用于保存一组待处理对象。
     */
    private List<String> clientAttributes;
    /**
     * 服务端列表，用于保存一组待处理对象。
     */
    private List<String> serverAttributes;
    /**
     * `sharedAttributes`列表，用于保存一组待处理对象。
     */
    private List<String> sharedAttributes;
    /**
     * 时间戳列表，用于保存一组待处理对象。
     */
    private List<String> tsKeys;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long ts;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbGetAttributesNode node;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() throws TbNodeException {
        dbExecutor = new AbstractListeningExecutor() {
            /**
             * 功能：获取`Thread Poll Size`。
             * 参数：无。
             * 返回：数值结果。
             */
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

        clientAttributes = getAttributeNames("client");
        serverAttributes = getAttributeNames("server");
        sharedAttributes = getAttributeNames("shared");
        tsKeys = List.of("temperature", "humidity", "unknown");
        ts = System.currentTimeMillis();

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.CLIENT_SCOPE, clientAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(clientAttributes, ts)));

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.SERVER_SCOPE, serverAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(serverAttributes, ts)));

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.SHARED_SCOPE, sharedAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(sharedAttributes, ts)));

        when(timeseriesServiceMock.findLatest(TENANT_ID, ORIGINATOR, tsKeys))
                .thenReturn(Futures.immediateFuture(getListTsKvEntry(tsKeys, ts)));
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
        dbExecutor.destroy();
    }

    /**
     * 功能：验证 `givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.METADATA, false, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.METADATA, false, tsKeys);
    }

    /**
     * 功能：验证 `givenFetchLatestTimeseriesToMetadata_whenOnMsg_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchLatestTimeseriesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.METADATA, true, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.METADATA, true, tsKeys);
    }

    /**
     * 功能：验证 `givenFetchAttributesToData_whenOnMsg_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchAttributesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, false, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.DATA, false, tsKeys);
    }

    /**
     * 功能：验证 `givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellSuccess` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.DATA, true, tsKeys);
    }

    /**
     * 功能：验证 `givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellFailure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.METADATA, false, true);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(actualMsg, TbMsgSource.METADATA, false, tsKeys);
    }

    /**
     * 功能：验证 `givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellFailure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, true);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(actualMsg, TbMsgSource.DATA, true, tsKeys);
    }

    /**
     * 功能：验证 `givenFetchLatestTimeseriesToDataAndDataIsNotJsonObject_whenOnMsg_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenFetchLatestTimeseriesToDataAndDataIsNotJsonObject_whenOnMsg_thenException() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, true);
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, ORIGINATOR, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        verify(ctxMock, never()).tellSuccess(any());
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
    }

    /**
     * 功能：验证 `givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 功能：验证 `givenOldConfigWithNoFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfigWithNoFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 功能：验证 `givenOldConfigWithNullFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfigWithNullFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"fetchToData\":null," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 功能：校验消息。
     * 参数：
     * - `checkSuccess`：`checkSuccess` 参数。
     * 返回：判断结果。
     */
    private TbMsg checkMsg(boolean checkSuccess) {
        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        if (checkSuccess) {
            verify(ctxMock, timeout(5000)).tellSuccess(msgCaptor.capture());
        } else {
            var exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
            verify(ctxMock, never()).tellSuccess(any());
            verify(ctxMock, timeout(5000)).tellFailure(msgCaptor.capture(), exceptionCaptor.capture());
            var exception = exceptionCaptor.getValue();
            assertNotNull(exception);
            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().startsWith("The following attribute/telemetry keys is not present in the DB:"));
        }

        var resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getMetaData());
        assertNotNull(resultMsg.getData());
        return resultMsg;
    }

    /**
     * 功能：校验`Attributes`。
     * 参数：
     * - `actualMsg`：待处理消息。
     * - `fetchTo`：`fetchTo` 参数。
     * - `prefix`：`prefix` 参数。
     * - `attributes`：数据列表。
     * 返回：无。
     */
    private void checkAttributes(TbMsg actualMsg, TbMsgSource fetchTo, String prefix, List<String> attributes) {
        var msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        attributes.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .forEach(attribute -> {
                    String result = null;
                    if (TbMsgSource.DATA.equals(fetchTo)) {
                        result = msgData.get(prefix + attribute).asText();
                    } else if (TbMsgSource.METADATA.equals(fetchTo)) {
                        result = actualMsg.getMetaData().getValue(prefix + attribute);
                    }
                    assertNotNull(result);
                    assertEquals(attribute + "_value", result);
                });
    }

    /**
     * 功能：校验时间戳。
     * 参数：
     * - `actualMsg`：待处理消息。
     * - `fetchTo`：`fetchTo` 参数。
     * - `getLatestValueWithTs`：时间戳。
     * - `tsKeys`：键。
     * 返回：无。
     */
    private void checkTs(TbMsg actualMsg, TbMsgSource fetchTo, boolean getLatestValueWithTs, List<String> tsKeys) {
        var msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        long value = 1L;
        for (var key : tsKeys) {
            if (key.equals("unknown")) {
                continue;
            }
            String actualValue = null;
            String expectedValue;
            if (getLatestValueWithTs) {
                expectedValue = "{\"ts\":" + ts + ",\"value\":{\"data\":" + value + "}}";
            } else {
                expectedValue = "{\"data\":" + value + "}";
            }
            if (TbMsgSource.DATA.equals(fetchTo)) {
                actualValue = JacksonUtil.toString(msgData.get(key));
            } else if (TbMsgSource.METADATA.equals(fetchTo)) {
                actualValue = actualMsg.getMetaData().getValue(key);
            }
            assertNotNull(actualValue);
            assertEquals(expectedValue, actualValue);
            value++;
        }
    }

    /**
     * 功能：初始化或启动节点实例。
     * 参数：
     * - `fetchTo`：`fetchTo` 参数。
     * - `getLatestValueWithTs`：时间戳。
     * - `isTellFailureIfAbsent`：`isTellFailureIfAbsent` 参数。
     * 返回：处理结果。
     */
    private TbGetAttributesNode initNode(TbMsgSource fetchTo, boolean getLatestValueWithTs, boolean isTellFailureIfAbsent) throws TbNodeException {
        var config = new TbGetAttributesNodeConfiguration();
        config.setClientAttributeNames(List.of("client_attr_1", "client_attr_2", "${client_attr_metadata}", "unknown"));
        config.setServerAttributeNames(List.of("server_attr_1", "server_attr_2", "${server_attr_metadata}", "unknown"));
        config.setSharedAttributeNames(List.of("shared_attr_1", "shared_attr_2", "$[shared_attr_data]", "unknown"));
        config.setLatestTsKeyNames(List.of("temperature", "humidity", "unknown"));
        config.setFetchTo(fetchTo);
        config.setGetLatestValueWithTs(getLatestValueWithTs);
        config.setTellFailureIfAbsent(isTellFailureIfAbsent);

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var node = new TbGetAttributesNode();
        node.init(ctxMock, nodeConfiguration);
        return node;
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(EntityId entityId) {
        var msgData = JacksonUtil.newObjectNode();
        msgData.put("shared_attr_data", "shared_attr_3");

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("client_attr_metadata", "client_attr_3");
        msgMetaData.putValue("server_attr_metadata", "server_attr_3");

        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, entityId, msgMetaData, JacksonUtil.toString(msgData));
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `prefix`：`prefix` 参数。
     * 返回：匹配的数据集合。
     */
    private List<String> getAttributeNames(String prefix) {
        return List.of(prefix + "_attr_1", prefix + "_attr_2", prefix + "_attr_3", "unknown");
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `attributesList`：数据列表。
     * - `ts`：时间戳。
     * 返回：匹配的数据集合。
     */
    private List<AttributeKvEntry> getListAttributeKvEntry(List<String> attributesList, long ts) {
        return attributesList.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .map(attribute -> toAttributeKvEntry(ts, attribute))
                .collect(Collectors.toList());
    }

    /**
     * 功能：执行 `toAttributeKvEntry` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * - `attribute`：`attribute` 参数。
     * 返回：处理结果。
     */
    private BaseAttributeKvEntry toAttributeKvEntry(long ts, String attribute) {
        return new BaseAttributeKvEntry(ts, new StringDataEntry(attribute, attribute + "_value"));
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `keysList`：键。
     * - `ts`：时间戳。
     * 返回：匹配的数据集合。
     */
    private List<TsKvEntry> getListTsKvEntry(List<String> keysList, long ts) {
        long value = 1L;
        var kvEntriesList = new ArrayList<TsKvEntry>();
        for (var key : keysList) {
            if (key.equals("unknown")) {
                continue;
            }
            String dataValue = "{\"data\":" + value + "}";
            kvEntriesList.add(new BasicTsKvEntry(ts, new JsonDataEntry(key, dataValue)));
            value++;
        }
        return kvEntriesList;
    }

}
/*
 * 本类总结：{@code TbGetAttributesNodeTest} 为 {@code TbGetAttributesNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
