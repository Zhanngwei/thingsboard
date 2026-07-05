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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * `TbSplitArrayMsgNodeTest` 测试类，用于验证 `TbSplitArrayMsgNode` 相关行为。
 */
public class TbSplitArrayMsgNodeTest {
    /**
     * 设备ID，用于定位对应业务对象。
     */
    DeviceId deviceId;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    TbSplitArrayMsgNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    EmptyNodeConfiguration config;
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
        config = new EmptyNodeConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbSplitArrayMsgNode());
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
     * 功能：验证 `givenFewMsg_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenFewMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "[{\"Attribute_1\":22.5,\"Attribute_2\":10.3}, {\"Attribute_1\":1,\"Attribute_2\":2}]";
        VerifyOutputMsg(data);
    }

    /**
     * 功能：验证 `givenOneMsg_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenOneMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "[{\"Attribute_1\":22.5,\"Attribute_2\":10.3}]";
        VerifyOutputMsg(data);
    }

    /**
     * 功能：验证 `givenZeroMsg_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenZeroMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        VerifyOutputMsg(TbMsg.EMPTY_JSON_ARRAY);
    }

    /**
     * 功能：验证 `givenNoArrayMsg_whenOnMsg_thenFailure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNoArrayMsg_whenOnMsg_thenFailure() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, never()).enqueueForTellNext(any(), anyString(), any(), any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private void VerifyOutputMsg(String data) throws Exception {
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        TbMsg tbMsg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, tbMsg);

        if (dataNode.size() > 1) {
            ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
            ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
            verify(ctx, times(dataNode.size())).enqueueForTellNext(any(), anyString(), successCaptor.capture(), failureCaptor.capture());
            for (Runnable valueCaptor : successCaptor.getAllValues()) {
                valueCaptor.run();
            }
            verify(ctx, times(1)).ack(tbMsg);
        } else {
            ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            verify(ctx, times(dataNode.size())).tellSuccess(newMsgCaptor.capture());
        }
        verify(ctx, never()).tellFailure(any(), any());
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `entityId`：实体IDID。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(EntityId entityId, String data) {
        Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
        );
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, new TbMsgMetaData(mdMap), data, callback);
    }
}
/*
 * 本类总结：{@code TbSplitArrayMsgNodeTest} 为 {@code TbSplitArrayMsgNode} 的 消息转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
