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
 * 测试目标：验证 {@code TbCopyKeysNodeTest} 覆盖的 消息转换节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbCopyKeysNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class TbCopyKeysNodeTest {
    /** 可变 fixture 字段：{@code deviceId} 保存 {@code DeviceId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    DeviceId deviceId;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbCopyKeysNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbCopyKeysNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbCopyKeysNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbCopyKeysNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbNodeConfiguration nodeConfiguration;
    /** 可变 fixture 字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbContext ctx;
    /** 可变 fixture 字段：{@code callback} 保存 {@code TbMsgCallback} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbMsgCallback callback;

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
        config = new TbCopyKeysNodeConfiguration().defaultConfiguration();
        config.setKeys(Set.of("TestKey_1", "TestKey_2", "TestKey_3", "(\\w*)Data(\\w*)"));
        config.setCopyFrom(TbMsgSource.METADATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbCopyKeysNode());
        node.init(ctx, nodeConfiguration);
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
        TbCopyKeysNodeConfiguration defaultConfig = new TbCopyKeysNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptySet());
        assertThat(defaultConfig.getCopyFrom()).isEqualTo(TbMsgSource.DATA);
    }

    /**
     * 测试方法：覆盖 {@code givenMsgFromMetadata_whenOnMsg_thenVerifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsgFromMetadata_whenOnMsg_thenVerifyOutput() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        node.onMsg(ctx, getTbMsg(deviceId, TbMsg.EMPTY_JSON_OBJECT));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        JsonNode dataNode = JacksonUtil.toJsonNode(newMsg.getData());
        assertThat(dataNode.has("TestKey_1")).isEqualTo(true);
        assertThat(dataNode.has("voltageDataValue")).isEqualTo(true);
    }

    /**
     * 测试方法：覆盖 {@code givenMsgFromMsg_whenOnMsg_thenVerifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsgFromMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        config.setCopyFrom(TbMsgSource.DATA);
        config.setKeys(Set.of(".*Key$"));
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        String data = "{\"nullKey\":null,\"stringKey\":\"value1\",\"booleanKey\":true,\"doubleKey\":42.0,\"longKey\":73," +
                "\"jsonKey\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        Map<String, String> metaDataMap = newMsg.getMetaData().getData();
        assertThat(metaDataMap.get("nullKey")).isEqualTo("null");
        assertThat(metaDataMap.get("stringKey")).isEqualTo("value1");
        assertThat(metaDataMap.get("booleanKey")).isEqualTo("true");
        assertThat(metaDataMap.get("doubleKey")).isEqualTo("42.0");
        assertThat(metaDataMap.get("longKey")).isEqualTo("73");
        assertThat(metaDataMap.get("jsonKey"))
                .isEqualTo("{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}");
    }

    /**
     * 测试方法：覆盖 {@code givenEmptyKeys_whenOnMsg_thenVerifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenEmptyKeys_whenOnMsg_thenVerifyOutput() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbCopyKeysNodeConfiguration defaultConfig = new TbCopyKeysNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(defaultConfig));
        node.init(ctx, nodeConfiguration);

        String data = "{\"DigitData\":22.5,\"TempDataValue\":10.5}";
        TbMsg msg = getTbMsg(deviceId, data);
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 测试方法：覆盖 {@code givenMsgDataNotJSONObject_whenOnMsg_thenTVerifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsgDataNotJSONObject_whenOnMsg_thenTVerifyOutput() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbMsg msg = getTbMsg(deviceId, TbMsg.EMPTY_JSON_ARRAY);
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg).isSameAs(msg);
    }

    /** 参数源方法：{@code givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig() {
        return Stream.of(
                Arguments.of(0, "{\"fromMetadata\":false,\"keys\":[\"temperature\"]}", true, "{\"copyFrom\":\"DATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(0, "{\"fromMetadata\":true,\"keys\":[\"temperature\"]}", true, "{\"copyFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"fromMetadata\":\"METADATA\",\"keys\":[\"temperature\"]}", true, "{\"copyFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"fromMetadata\":\"DATA\",\"keys\":[\"temperature\"]}", true, "{\"copyFrom\":\"DATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"copyFrom\":\"METADATA\",\"keys\":[\"temperature\"]}", false, "{\"copyFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"copyFrom\":\"DATA\",\"keys\":[\"temperature\"]}", false, "{\"copyFrom\":\"DATA\",\"keys\":[\"temperature\"]}")
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
     * 辅助方法：{@code getTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
 * 本类总结：{@code TbCopyKeysNodeTest} 为 {@code TbCopyKeysNode} 的 消息转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
