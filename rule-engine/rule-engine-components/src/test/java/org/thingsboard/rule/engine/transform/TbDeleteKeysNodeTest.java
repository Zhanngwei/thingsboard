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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * `TbDeleteKeysNodeTest` 测试类，用于验证 `TbDeleteKeysNode` 相关行为。
 */
public class TbDeleteKeysNodeTest {
    /**
     * 设备ID，用于定位对应业务对象。
     */
    DeviceId deviceId;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    TbDeleteKeysNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    TbDeleteKeysNodeConfiguration config;
    /**
     * 节点实例，保存当前对象的配置选项。
     */
    TbNodeConfiguration nodeConfiguration;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    TbContext ctx;
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    TbMsgCallback callback;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        config.setKeys(Set.of("TestKey_1", "TestKey_2", "TestKey_3", "(\\w*)Data(\\w*)"));
        config.setDeleteFrom(TbMsgSource.METADATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbDeleteKeysNode());
        node.init(ctx, nodeConfiguration);
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
     * 功能：验证 `givenDefaultConfig_whenVerify_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbDeleteKeysNodeConfiguration defaultConfig = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptySet());
        assertThat(defaultConfig.getDeleteFrom()).isEqualTo(TbMsgSource.DATA);
    }

    /**
     * 功能：验证 `givenDeleteFromMetadata_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDeleteFromMetadata_whenOnMsg_thenVerifyOutput() throws Exception {
        node.onMsg(ctx, getTbMsg(deviceId, TbMsg.EMPTY_JSON_OBJECT));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        Map<String, String> metaDataMap = newMsg.getMetaData().getData();
        assertThat(metaDataMap.containsKey("TestKey_1")).isEqualTo(false);
        assertThat(metaDataMap.containsKey("voltageDataValue")).isEqualTo(false);
    }

    /**
     * 功能：验证 `givenDeleteFromMsgConfig_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDeleteFromMsgConfig_whenOnMsg_thenVerifyOutput() throws Exception {
        config.setDeleteFrom(TbMsgSource.DATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        String data = "{\"Voltage\":22.5,\"TempDataValue\":10.5}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        JsonNode dataNode = JacksonUtil.toJsonNode(newMsg.getData());
        assertThat(dataNode.has("TempDataValue")).isEqualTo(false);
        assertThat(dataNode.has("Voltage")).isEqualTo(true);
    }

    /**
     * 功能：验证 `givenEmptyKeys_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenEmptyKeys_whenOnMsg_thenVerifyOutput() throws Exception {
        TbDeleteKeysNodeConfiguration defaultConfig = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(defaultConfig));
        node.init(ctx, nodeConfiguration);

        String data = "{\"Voltage\":220,\"Humidity\":56}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getData()).isEqualTo(data);
    }

    /**
     * 功能：验证 `givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig` 描述的测试场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig() {
        return Stream.of(
                Arguments.of(0, "{\"fromMetadata\":false,\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(0, "{\"fromMetadata\":true,\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"dataToFetch\":\"METADATA\",\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"dataToFetch\":\"DATA\",\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}", false, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}", false, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig(int givenVersion, String givenConfigStr,
                                                                                boolean hasChanges, String expectedConfigStr) throws Exception {
        // GIVEN
        JsonNode givenConfig = JacksonUtil.toJsonNode(givenConfigStr);
        JsonNode expectedConfig = JacksonUtil.toJsonNode(expectedConfigStr);

        // WHEN
        var upgradeResult = node.upgrade(givenVersion, givenConfig);

        // THEN
        assertThat(upgradeResult.getFirst()).isEqualTo(hasChanges);
        ObjectNode upgradedConfig = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradedConfig).isEqualTo(expectedConfig);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `entityId`：实体IDID。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(EntityId entityId, String data) {
        final Map<String, String> mdMap = Map.of(
                "TestKey_1", "Test",
                "country", "US",
                "voltageDataValue", "220",
                "city", "NY"
        );
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, new TbMsgMetaData(mdMap), data, callback);
    }

}
/*
 * 本类总结：{@code TbDeleteKeysNodeTest} 为 {@code TbDeleteKeysNode} 的 消息转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
