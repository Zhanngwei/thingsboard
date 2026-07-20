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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * `TbCheckMessageNodeTest` 测试类，用于验证 `TbCheckMessageNode` 相关行为。
 */
class TbCheckMessageNodeTest {

    /**
     * 设备ID常量，用于统一引用固定值。
     */
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final TbMsg EMPTY_POST_ATTRIBUTES_MSG = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbCheckMessageNode node;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        node = new TbCheckMessageNode();
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
     * 功能：验证 `givenDefaultConfig_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDefaultConfig_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 功能：验证 `givenCustomConfigWithoutCheckAllKeysAndWithEmptyLists_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithoutCheckAllKeysAndWithEmptyLists_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setCheckAllKeys(false);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 功能：验证 `givenCustomConfigWithCheckAllKeys_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithCheckAllKeys_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0"));
        configuration.setMetadataNames(List.of("deviceName", "deviceType", "ts"));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg();

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    /**
     * 功能：验证 `givenCustomConfigWithCheckAllKeys_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithCheckAllKeys_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0", "temperature-1"));
        configuration.setMetadataNames(List.of("deviceName", "deviceType", "ts"));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg();

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    /**
     * 功能：验证 `givenCustomConfigWithoutCheckAllKeys_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithoutCheckAllKeys_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0", "temperature-1"));
        configuration.setCheckAllKeys(false);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg();

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    /**
     * 功能：验证 `givenCustomConfigWithoutCheckAllKeysAndEmptyMsg_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithoutCheckAllKeysAndEmptyMsg_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0", "temperature-1"));
        configuration.setCheckAllKeys(false);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg(true);

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg() {
        return getTbMsg(false);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `emptyData`：待处理数据。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(boolean emptyData) {
        String data = emptyData ? TbMsg.EMPTY_JSON_OBJECT : "{\"temperature-0\": 25}";
        var metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.DEVICE_NAME, "Test Device");
        metadata.putValue(DataConstants.DEVICE_TYPE, DataConstants.DEFAULT_DEVICE_TYPE);
        metadata.putValue("ts", String.valueOf(System.currentTimeMillis()));
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, metadata, data);
    }

}
