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
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.tools.TbRateLimits;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * 测试目标：验证 {@code TbDeviceStateNodeTest} 覆盖的 动作节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbDeviceStateNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class TbDeviceStateNodeTest {

    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code deviceStateManagerMock} 保存 {@code RuleEngineDeviceStateManager} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private static RuleEngineDeviceStateManager deviceStateManagerMock;
    /** 参数捕获字段：{@code callbackCaptor} 保存 {@code ArgumentCaptor<TbCallback>} 测试数据或依赖，来源：由 Mockito 注解创建，用于捕获被测逻辑传给 Mock 的参数，生命周期随单个测试实例。 */
    @Captor
    private static ArgumentCaptor<TbCallback> callbackCaptor;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbDeviceStateNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbDeviceStateNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbDeviceStateNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbDeviceStateNodeConfiguration config;

    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    /** 测试常量字段：{@code DEVICE_ID} 保存 {@code DeviceId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    /** 测试常量字段：{@code METADATA_TS} 保存 {@code long} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final long METADATA_TS = 123L;
    /** 可变 fixture 字段：{@code msg} 保存 {@code TbMsg} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsg msg;

    /**
     * 生命周期方法：{@code setup} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void setup() {
        var metaData = new TbMsgMetaData();
        metaData.putValue("deviceName", "My humidity sensor");
        metaData.putValue("deviceType", "Humidity sensor");
        metaData.putValue("ts", String.valueOf(METADATA_TS));
        var data = JacksonUtil.newObjectNode();
        data.put("humidity", 58.3);
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, JacksonUtil.toString(data));
    }

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void setUp() {
        node = new TbDeviceStateNode();
        config = new TbDeviceStateNodeConfiguration().defaultConfiguration();
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfiguration_whenInvoked_thenCorrectValuesAreSet} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDefaultConfiguration_whenInvoked_thenCorrectValuesAreSet() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        assertThat(config.getEvent()).isEqualTo(TbMsgType.ACTIVITY_EVENT);
    }

    /**
     * 测试方法：覆盖 {@code givenNullEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenNullEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN-WHEN-THEN
        assertThatThrownBy(() -> initNode(null))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Event cannot be null!")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    /**
     * 测试方法：覆盖 {@code givenInvalidRateLimitConfig_whenInit_thenUsesDefaultConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenInvalidRateLimitConfig_whenInit_thenUsesDefaultConfig() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        given(ctxMock.getDeviceStateNodeRateLimitConfig()).willReturn("invalid rate limit config");
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getSelfId()).willReturn(new RuleNodeId(UUID.randomUUID()));

        // WHEN
        try {
            initNode(TbMsgType.ACTIVITY_EVENT);
        } catch (Exception e) {
            fail("Node failed to initialize!", e);
        }

        // THEN
        String actualRateLimitConfig = (String) ReflectionTestUtils.getField(node, "rateLimitConfig");
        assertThat(actualRateLimitConfig).isEqualTo("1:1,30:60,60:3600");
    }

    /**
     * 测试方法：覆盖 {@code givenMsgArrivedTooFast_whenOnMsg_thenRateLimitsThisMsg} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenMsgArrivedTooFast_whenOnMsg_thenRateLimitsThisMsg() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        ConcurrentReferenceHashMap<DeviceId, TbRateLimits> rateLimits = new ConcurrentReferenceHashMap<>();
        ReflectionTestUtils.setField(node, "rateLimits", rateLimits);

        var rateLimitMock = mock(TbRateLimits.class);
        rateLimits.put(DEVICE_ID, rateLimitMock);

        given(rateLimitMock.tryConsume()).willReturn(false);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().tellNext(msg, "Rate limited");
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
        then(ctxMock).shouldHaveNoMoreInteractions();
        then(deviceStateManagerMock).shouldHaveNoInteractions();
    }

    /**
     * 测试方法：覆盖 {@code givenHasNonLocalDevices_whenOnPartitionChange_thenRemovesEntriesForNonLocalDevices} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenHasNonLocalDevices_whenOnPartitionChange_thenRemovesEntriesForNonLocalDevices() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        ConcurrentReferenceHashMap<DeviceId, TbRateLimits> rateLimits = new ConcurrentReferenceHashMap<>();
        ReflectionTestUtils.setField(node, "rateLimits", rateLimits);

        rateLimits.put(DEVICE_ID, new TbRateLimits("1:1"));
        given(ctxMock.isLocalEntity(eq(DEVICE_ID))).willReturn(true);

        DeviceId nonLocalDeviceId1 = new DeviceId(UUID.randomUUID());
        rateLimits.put(nonLocalDeviceId1, new TbRateLimits("2:2"));
        given(ctxMock.isLocalEntity(eq(nonLocalDeviceId1))).willReturn(false);

        DeviceId nonLocalDeviceId2 = new DeviceId(UUID.randomUUID());
        rateLimits.put(nonLocalDeviceId2, new TbRateLimits("3:3"));
        given(ctxMock.isLocalEntity(eq(nonLocalDeviceId2))).willReturn(false);

        // WHEN
        node.onPartitionChangeMsg(ctxMock, new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE));

        // THEN
        assertThat(rateLimits)
                .containsKey(DEVICE_ID)
                .doesNotContainKey(nonLocalDeviceId1)
                .doesNotContainKey(nonLocalDeviceId2)
                .size().isOne();
    }

    /** 可变 fixture 字段：{@code value} 保存 {@code 字段} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    @ParameterizedTest
    @EnumSource(
            value = TbMsgType.class,
            /** 可变 fixture 字段：{@code names} 保存 {@code 字段} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
            names = {"CONNECT_EVENT", "ACTIVITY_EVENT", "DISCONNECT_EVENT", "INACTIVITY_EVENT"},
            /** 可变 fixture 字段：{@code mode} 保存 {@code 字段} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
            mode = EnumSource.Mode.EXCLUDE
    )
    /**
     * 辅助方法：{@code givenUnsupportedEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    public void givenUnsupportedEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException(TbMsgType unsupportedEvent) {
        // GIVEN-WHEN-THEN
        assertThatThrownBy(() -> initNode(unsupportedEvent))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Unsupported event: " + unsupportedEvent)
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    /**
     * 测试方法：覆盖 {@code givenNonDeviceOriginator_whenOnMsg_thenTellsSuccessAndNoActivityActionsTriggered} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @EnumSource(value = EntityType.class, names = "DEVICE", mode = EnumSource.Mode.EXCLUDE)
    public void givenNonDeviceOriginator_whenOnMsg_thenTellsSuccessAndNoActivityActionsTriggered(EntityType unsupportedType) {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var nonDeviceOriginator = new EntityId() {

            /** 实现方法：{@code getId} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
            @Override
            public UUID getId() {
                return UUID.randomUUID();
            }

            /** 实现方法：{@code getEntityType} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
            @Override
            public EntityType getEntityType() {
                return unsupportedType;
            }
        };
        var msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, nonDeviceOriginator, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        then(ctxMock).should().tellFailure(eq(msg), exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported originator entity type: [" + unsupportedType + "]. Only DEVICE entity type is supported.");

        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    /**
     * 测试方法：覆盖 {@code givenMetadataDoesNotContainTs_whenOnMsg_thenMsgTsIsUsedAsEventTs} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenMetadataDoesNotContainTs_whenOnMsg_thenMsgTsIsUsedAsEventTs() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        given(ctxMock.getDeviceStateNodeRateLimitConfig()).willReturn("1:1");
        try {
            initNode(TbMsgType.ACTIVITY_EVENT);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }

        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDeviceStateManager()).willReturn(deviceStateManagerMock);

        long msgTs = METADATA_TS + 1;
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT, msgTs);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(deviceStateManagerMock).should().onDeviceActivity(eq(TENANT_ID), eq(DEVICE_ID), eq(msgTs), any());
    }

    /**
     * 测试方法：覆盖 {@code givenSupportedEventAndDeviceOriginator_whenOnMsg_thenCorrectEventIsSentWithCorrectCallback} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @MethodSource
    public void givenSupportedEventAndDeviceOriginator_whenOnMsg_thenCorrectEventIsSentWithCorrectCallback(TbMsgType supportedEventType, Runnable actionVerification) {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDeviceStateNodeRateLimitConfig()).willReturn("1:1");
        given(ctxMock.getDeviceStateManager()).willReturn(deviceStateManagerMock);

        try {
            initNode(supportedEventType);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        actionVerification.run();

        TbCallback actualCallback = callbackCaptor.getValue();

        actualCallback.onSuccess();
        then(ctxMock).should().tellSuccess(msg);

        var throwable = new Throwable();
        actualCallback.onFailure(throwable);
        then(ctxMock).should().tellFailure(msg, throwable);


        then(deviceStateManagerMock).shouldHaveNoMoreInteractions();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    /** 参数源方法：{@code givenSupportedEventAndDeviceOriginator_whenOnMsg_thenCorrectEventIsSentWithCorrectCallback} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenSupportedEventAndDeviceOriginator_whenOnMsg_thenCorrectEventIsSentWithCorrectCallback() {
        return Stream.of(
                Arguments.of(TbMsgType.CONNECT_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceConnect(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.ACTIVITY_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceActivity(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.DISCONNECT_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceDisconnect(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.INACTIVITY_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceInactivity(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture()))
        );
    }

    /**
     * 辅助方法：{@code initNode} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void initNode(TbMsgType event) throws TbNodeException {
        config.setEvent(event);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfig);
    }

}
/*
 * 本类总结：{@code TbDeviceStateNodeTest} 为 {@code TbDeviceStateNode} 的 动作节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
