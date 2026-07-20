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
package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * `TbMsgDeleteAttributesNodeTest` 测试类，用于验证 `TbMsgDeleteAttributesNode` 相关行为。
 */
@Slf4j
public class TbMsgDeleteAttributesNodeTest {
    /**
     * 设备ID，用于定位对应业务对象。
     */
    DeviceId deviceId;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    TbMsgDeleteAttributesNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    TbMsgDeleteAttributesNodeConfiguration config;
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
     * 遥测，提供当前类调用的业务操作。
     */
    RuleEngineTelemetryService telemetryService;

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
        config = new TbMsgDeleteAttributesNodeConfiguration().defaultConfiguration();
        config.setKeys(List.of("${TestAttribute_1}", "$[TestAttribute_2]", "$[TestAttribute_3]", "TestAttribute_4"));
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbMsgDeleteAttributesNode());
        node.init(ctx, nodeConfiguration);
        telemetryService = mock(RuleEngineTelemetryService.class);

        willReturn(telemetryService).given(ctx).getTelemetryService();
        willAnswer(invocation -> {
            TelemetryNodeCallback callBack = invocation.getArgument(5);
            callBack.onSuccess(null);
            return null;
        }).given(telemetryService).deleteAndNotify(
                any(), any(), anyString(), anyList(), anyBoolean(), any());
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
        TbMsgDeleteAttributesNodeConfiguration defaultConfig = new TbMsgDeleteAttributesNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getScope()).isEqualTo(DataConstants.SERVER_SCOPE);
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptyList());
    }

    /**
     * 功能：验证 `givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NoNotifyDevice` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NoNotifyDevice() throws Exception {
        onMsg_thenVerifyOutput(false, false, false);
    }

    /**
     * 功能：验证 `givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NoNotifyDevice` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NoNotifyDevice() throws Exception {
        config.setSendAttributesDeletedNotification(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(true, false, false);
    }

    /**
     * 功能：验证 `givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NotifyDevice` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NotifyDevice() throws Exception {
        config.setSendAttributesDeletedNotification(true);
        config.setNotifyDevice(true);
        config.setScope(DataConstants.SHARED_SCOPE);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(true, true, false);
    }

    /**
     * 功能：验证 `givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NotifyDeviceMetadata` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NotifyDeviceMetadata() throws Exception {
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(false, false, true);
    }

    /**
     * 功能：验证 `onMsg_thenVerifyOutput` 描述的测试场景。
     * 参数：
     * - `sendAttributesDeletedNotification`：`sendAttributesDeletedNotification` 参数。
     * - `notifyDevice`：设备信息或设备标识。
     * - `notifyDeviceMetadata`：设备信息或设备标识。
     * 返回：无。
     */
    void onMsg_thenVerifyOutput(boolean sendAttributesDeletedNotification, boolean notifyDevice, boolean notifyDeviceMetadata) throws Exception {
        final Map<String, String> mdMap = Map.of(
                "TestAttribute_1", "temperature",
                "city", "NY"
        );
        TbMsgMetaData metaData = new TbMsgMetaData(mdMap);
        if (notifyDeviceMetadata) {
            metaData.putValue(DataConstants.NOTIFY_DEVICE_METADATA_KEY, "true");
            metaData.putValue(DataConstants.SCOPE, DataConstants.SHARED_SCOPE);
        }
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, metaData, data, callback);
        node.onMsg(ctx, msg);

        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        if (sendAttributesDeletedNotification) {
            verify(ctx, times(1)).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
            successCaptor.getValue().run();
            verify(ctx, times(1)).attributesDeletedActionMsg(any(), any(), anyString(), anyList());
        }
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());
        verify(telemetryService, times(1)).deleteAndNotify(any(), any(), anyString(), anyList(), eq(notifyDevice || notifyDeviceMetadata), any());
    }
}
