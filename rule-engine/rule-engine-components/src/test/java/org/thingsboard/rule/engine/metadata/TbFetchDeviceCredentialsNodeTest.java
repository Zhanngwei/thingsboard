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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.dao.device.DeviceCredentialsService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 测试目标：验证 {@code TbFetchDeviceCredentialsNodeTest} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbFetchDeviceCredentialsNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class TbFetchDeviceCredentialsNodeTest {

    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code callbackMock} 保存 {@code TbMsgCallback} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbMsgCallback callbackMock;
    /** Mock 依赖字段：{@code deviceCredentialsServiceMock} 保存 {@code DeviceCredentialsService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DeviceCredentialsService deviceCredentialsServiceMock;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbFetchDeviceCredentialsNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    @Spy
    private TbFetchDeviceCredentialsNode node;
    /** 可变 fixture 字段：{@code deviceId} 保存 {@code DeviceId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private DeviceId deviceId;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbFetchDeviceCredentialsNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbFetchDeviceCredentialsNodeConfiguration config;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        config = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
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
     * 测试方法：覆盖 {@code givenDefaultConfig_whenInit_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        assertThat(node.config).isEqualTo(config);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.METADATA);
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
        var defaultConfig = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getFetchTo()).isEqualTo(TbMsgSource.METADATA);
    }

    /**
     * 测试方法：覆盖 {@code givenValidMsg_whenOnMsg_thenVerifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenValidMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        doReturn(deviceCredentialsServiceMock).when(ctxMock).getDeviceCredentialsService();
        doAnswer(invocation -> {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            return deviceCredentials;
        }).when(deviceCredentialsServiceMock).findDeviceCredentialsByDeviceId(any(), any());
        doAnswer(invocation -> JacksonUtil.newObjectNode()).when(deviceCredentialsServiceMock).toCredentialsInfo(any());

        // WHEN
        node.onMsg(ctxMock, getTbMsg(deviceId));

        // THEN
        var newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());
        verify(deviceCredentialsServiceMock, times(1)).findDeviceCredentialsByDeviceId(any(), any());

        var newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getMetaData().getData().containsKey("credentials")).isEqualTo(true);
        assertThat(newMsg.getMetaData().getData().containsKey("credentialsType")).isEqualTo(true);
    }

    /**
     * 测试方法：覆盖 {@code givenUnsupportedOriginatorType_whenOnMsg_thenShouldTellFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenUnsupportedOriginatorType_whenOnMsg_thenShouldTellFailure() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var randomCustomerId = new CustomerId(UUID.randomUUID());

        // WHEN
        node.onMsg(ctxMock, getTbMsg(randomCustomerId));

        // THEN
        var newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试方法：覆盖 {@code givenGetDeviceCredentials_whenOnMsg_thenShouldTellFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenGetDeviceCredentials_whenOnMsg_thenShouldTellFailure() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        doReturn(deviceCredentialsServiceMock).when(ctxMock).getDeviceCredentialsService();
        willAnswer(invocation -> null).given(deviceCredentialsServiceMock).findDeviceCredentialsByDeviceId(any(), any());

        // WHEN
        node.onMsg(ctxMock, getTbMsg(deviceId));

        // THEN
        var newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试方法：覆盖 {@code givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        String oldConfig = "{\"fetchToMetadata\":true}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        assertTrue(upgrade.getFirst());
        assertEquals(config, JacksonUtil.treeToValue(upgrade.getSecond(), config.getClass()));
    }

    /**
     * 辅助方法：{@code getTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getTbMsg(EntityId entityId) {
        final Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
        );

        final var metaData = new TbMsgMetaData(mdMap);
        final String data = "{\"TestAttribute_1\": \"humidity\", \"TestAttribute_2\": \"voltage\"}";

        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, metaData, data, callbackMock);
    }

}
/*
 * 本类总结：{@code TbFetchDeviceCredentialsNodeTest} 为 {@code TbFetchDeviceCredentialsNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
