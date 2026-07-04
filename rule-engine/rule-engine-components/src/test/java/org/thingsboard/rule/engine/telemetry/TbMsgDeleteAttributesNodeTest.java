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
 * 测试目标：验证 {@code TbMsgDeleteAttributesNodeTest} 覆盖的 遥测与属性节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbMsgDeleteAttributesNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@Slf4j
public class TbMsgDeleteAttributesNodeTest {
    /** 可变 fixture 字段：{@code deviceId} 保存 {@code DeviceId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    DeviceId deviceId;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbMsgDeleteAttributesNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbMsgDeleteAttributesNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbMsgDeleteAttributesNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbMsgDeleteAttributesNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbNodeConfiguration nodeConfiguration;
    /** 可变 fixture 字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbContext ctx;
    /** 可变 fixture 字段：{@code callback} 保存 {@code TbMsgCallback} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbMsgCallback callback;

    /** 可变 fixture 字段：{@code telemetryService} 保存 {@code RuleEngineTelemetryService} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    RuleEngineTelemetryService telemetryService;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
     * 生命周期方法：{@code tearDown} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenVerify_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbMsgDeleteAttributesNodeConfiguration defaultConfig = new TbMsgDeleteAttributesNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getScope()).isEqualTo(DataConstants.SERVER_SCOPE);
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptyList());
    }

    /**
     * 测试方法：覆盖 {@code givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NoNotifyDevice} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NoNotifyDevice() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        onMsg_thenVerifyOutput(false, false, false);
    }

    /**
     * 测试方法：覆盖 {@code givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NoNotifyDevice} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NoNotifyDevice() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        config.setSendAttributesDeletedNotification(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(true, false, false);
    }

    /**
     * 测试方法：覆盖 {@code givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NotifyDevice} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NotifyDevice() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        config.setSendAttributesDeletedNotification(true);
        config.setNotifyDevice(true);
        config.setScope(DataConstants.SHARED_SCOPE);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(true, true, false);
    }

    /**
     * 测试方法：覆盖 {@code givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NotifyDeviceMetadata} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NotifyDeviceMetadata() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(false, false, true);
    }

    /**
     * 辅助方法：{@code onMsg_thenVerifyOutput} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
/*
 * 本类总结：{@code TbMsgDeleteAttributesNodeTest} 为 {@code TbMsgDeleteAttributesNode} 的 遥测与属性节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
