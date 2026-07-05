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
import com.jayway.jsonpath.PathNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * `TbJsonPathNodeTest` 测试类，用于验证 `TbJsonPathNode` 相关行为。
 */
public class TbJsonPathNodeTest {
    /**
     * 设备ID，用于定位对应业务对象。
     */
    DeviceId deviceId;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    TbJsonPathNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    TbJsonPathNodeConfiguration config;
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
        config = new TbJsonPathNodeConfiguration();
        config.setJsonPath("$.Attribute_2");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbJsonPathNode());
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
     * 功能：验证 `givenDefaultConfig_whenInit_thenFail` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDefaultConfig_whenInit_thenFail() {
        config.setJsonPath("");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctx, nodeConfiguration)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 功能：验证 `givenDefaultConfig_whenVerify_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbJsonPathNodeConfiguration defaultConfig = new TbJsonPathNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getJsonPath()).isEqualTo(TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH);
    }

    /**
     * 功能：验证 `givenJsonMsg_whenOnMsg_thenVerifyOutputJsonPrimitiveNode` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenJsonMsg_whenOnMsg_thenVerifyOutputJsonPrimitiveNode() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":100}";
        VerifyOutputMsg(data, 1, 100);

        data = "{\"Attribute_1\":22.5,\"Attribute_2\":\"StringValue\"}";
        VerifyOutputMsg(data, 2, "StringValue");
    }

    /**
     * 功能：验证 `givenJsonMsg_whenOnMsg_thenVerifyJavaPrimitiveOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenJsonMsg_whenOnMsg_thenVerifyJavaPrimitiveOutput() throws Exception {
        config.setJsonPath("$.attributes.length()");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        String data = "{\"attributes\":[{\"attribute_1\":10},{\"attribute_2\":20},{\"attribute_3\":30},{\"attribute_4\":40}]}";
        VerifyOutputMsg(data, 1, 4);

    }

    /**
     * 功能：验证 `givenJsonArray_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenJsonArray_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":[{\"Attribute_3\":22.5,\"Attribute_4\":10.3}, {\"Attribute_5\":22.5,\"Attribute_6\":10.3}]}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode(data).get("Attribute_2"));
    }

    /**
     * 功能：验证 `givenJsonNode_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenJsonNode_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":{\"Attribute_3\":22.5,\"Attribute_4\":10.3}}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode(data).get("Attribute_2"));
    }

    /**
     * 功能：验证 `givenJsonArrayWithFilter_whenOnMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenJsonArrayWithFilter_whenOnMsg_thenVerifyOutput() throws Exception {
        config.setJsonPath("$.Attribute_2[?(@.voltage > 200)]");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":[{\"voltage\":220}, {\"voltage\":250}, {\"voltage\":110}]}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode("[{\"voltage\":220}, {\"voltage\":250}]"));
    }

    /**
     * 功能：验证 `givenNoArrayMsg_whenOnMsg_thenTellFailure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNoArrayMsg_whenOnMsg_thenTellFailure() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_5\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    /**
     * 功能：验证 `givenNoResultsForPath_whenOnMsg_thenTellFailure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNoResultsForPath_whenOnMsg_thenTellFailure() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_5\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(PathNotFoundException.class);
    }

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private void VerifyOutputMsg(String data, int countTellSuccess, Object value) throws Exception {
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        node.onMsg(ctx, getTbMsg(deviceId, dataNode.toString()));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(countTellSuccess)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        assertThat(newMsgCaptor.getValue().getData()).isEqualTo(JacksonUtil.toString(value));
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `entityId`：实体IDID。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(EntityId entityId, String data) {
        Map<String, String> mdMap = Map.of("country", "US",
                "city", "NY"
        );
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, new TbMsgMetaData(mdMap), data, callback);
    }
}
/*
 * 本类总结：{@code TbJsonPathNodeTest} 为 {@code TbJsonPathNode} 的 消息转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
