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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

/**
 * `TbMsgTypeFilterNodeTest` 测试类，用于验证 `TbMsgTypeFilterNode` 相关行为。
 */
class TbMsgTypeFilterNodeTest {

    /**
     * 设备ID，用于定位对应业务对象。
     */
    private DeviceId deviceId;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbMsgTypeFilterNode node;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() throws TbNodeException {
        ctx = mock(TbContext.class);
        var config = new TbMsgTypeFilterNodeConfiguration().defaultConfiguration();
        deviceId = new DeviceId(UUID.randomUUID());
        node = new TbMsgTypeFilterNode();
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
     * 功能：验证 `givenPostAttributes_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenPostAttributes_whenOnMsg_then_True() {
        // GIVEN
        TbMsg msg = getTbMsg(deviceId, POST_ATTRIBUTES_REQUEST);

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
     * 功能：验证 `givenAttributesUpdated_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenAttributesUpdated_whenOnMsg_then_False() {
        // GIVEN
        TbMsg msg = getTbMsg(deviceId, ATTRIBUTES_UPDATED);

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
     * 功能：获取消息。
     * 参数：
     * - `entityId`：实体IDID。
     * - `msgType`：待处理消息。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(EntityId entityId, TbMsgType msgType) {
        return TbMsg.newMsg(msgType, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
    }

}
/*
 * 本类总结：{@code TbMsgTypeFilterNodeTest} 为 {@code TbMsgTypeFilterNode} 的 过滤/分流节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
