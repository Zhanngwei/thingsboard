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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;

/**
 * 测试目标：验证 {@code TbMsgAttributesNodeTest} 覆盖的 遥测与属性节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbMsgAttributesNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@Slf4j
class TbMsgAttributesNodeTest extends AbstractRuleNodeUpgradeTest {

    /** 可变 fixture 字段：{@code tenantId} 保存 {@code TenantId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TenantId tenantId;
    /** 可变 fixture 字段：{@code deviceId} 保存 {@code DeviceId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private DeviceId deviceId;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbMsgAttributesNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsgAttributesNode node;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.fromString("6c18691e-4470-4766-9739-aface71d761f"));
        deviceId = new DeviceId(UUID.fromString("b66159d7-c77e-45e8-bb41-a8f557f434c1"));
        node = spy(TbMsgAttributesNode.class);
    }

    /**
     * 测试方法：覆盖 {@code testFilterChangedAttr_whenCurrentAttributesEmpty_thenReturnNewAttributes} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void testFilterChangedAttr_whenCurrentAttributesEmpty_thenReturnNewAttributes() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        List<AttributeKvEntry> newAttributes = new ArrayList<>();

        List<AttributeKvEntry> filtered = node.filterChangedAttr(Collections.emptyList(), newAttributes);
        assertThat(filtered).isSameAs(newAttributes);
    }

    /**
     * 测试方法：覆盖 {@code testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnEmptyList} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnEmptyList() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(1694000000L, new StringDataEntry("address", "Peremohy ave 1")),
                new BaseAttributeKvEntry(1694000000L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(1694000000L, new LongDataEntry("counter", 100L)),
                new BaseAttributeKvEntry(1694000000L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000000L, new JsonDataEntry("json", "{\"warning\":\"out of paper\"}"))
        );
        List<AttributeKvEntry> newAttributes = new ArrayList<>(currentAttributes);
        newAttributes.add(newAttributes.get(0));
        newAttributes.remove(0);
        assertThat(newAttributes).hasSize(currentAttributes.size());
        assertThat(currentAttributes).isNotEmpty();
        assertThat(newAttributes).containsExactlyInAnyOrderElementsOf(currentAttributes);

        List<AttributeKvEntry> filtered = node.filterChangedAttr(currentAttributes, newAttributes);
        assertThat(filtered).isEmpty(); //no changes
    }

    /**
     * 测试方法：覆盖 {@code testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnExpectedList} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnExpectedList() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(1694000000L, new StringDataEntry("address", "Peremohy ave 1")),
                new BaseAttributeKvEntry(1694000000L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(1694000000L, new LongDataEntry("counter", 100L)),
                new BaseAttributeKvEntry(1694000000L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000000L, new JsonDataEntry("json", "{\"warning\":\"out of paper\"}"))
        );
        List<AttributeKvEntry> newAttributes = List.of(
                new BaseAttributeKvEntry(1694000999L, new JsonDataEntry("json", "{\"status\":\"OK\"}")), // value changed, reordered
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("valid", "true")), //type changed
                new BaseAttributeKvEntry(1694000999L, new LongDataEntry("counter", 101L)), //value changed
                new BaseAttributeKvEntry(1694000999L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("address", "Peremohy ave 1")) // reordered
        );
        List<AttributeKvEntry> expected = List.of(
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("valid", "true")),
                new BaseAttributeKvEntry(1694000999L, new LongDataEntry("counter", 101L)),
                new BaseAttributeKvEntry(1694000999L, new JsonDataEntry("json", "{\"status\":\"OK\"}"))
        );

        List<AttributeKvEntry> filtered = node.filterChangedAttr(currentAttributes, newAttributes);
        assertThat(filtered).containsExactlyInAnyOrderElementsOf(expected);
    }

    // Notify device backward-compatibility test arguments
    /** 参数源方法：{@code givenNotifyDeviceMdValue_whenSaveAndNotify_thenVerifyExpectedArgumentForNotifyDeviceInSaveAndNotifyMethod} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenNotifyDeviceMdValue_whenSaveAndNotify_thenVerifyExpectedArgumentForNotifyDeviceInSaveAndNotifyMethod() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("null", false),
                Arguments.of("true", true),
                Arguments.of("false", false)
        );
    }

    // Notify device backward-compatibility test
    /**
     * 测试方法：覆盖 {@code givenNotifyDeviceMdValue_whenSaveAndNotify_thenVerifyExpectedArgumentForNotifyDeviceInSaveAndNotifyMethod} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @MethodSource
    void givenNotifyDeviceMdValue_whenSaveAndNotify_thenVerifyExpectedArgumentForNotifyDeviceInSaveAndNotifyMethod(String mdValue, boolean expectedArgumentValue) throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var ctxMock = mock(TbContext.class);
        var telemetryServiceMock = mock(RuleEngineTelemetryService.class);
        ObjectNode defaultConfig = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        defaultConfig.put("notifyDevice", false);
        var tbNodeConfiguration = new TbNodeConfiguration(defaultConfig);

        assertThat(defaultConfig.has("notifyDevice")).as("pre condition has notifyDevice").isTrue();

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        willCallRealMethod().given(node).init(any(TbContext.class), any(TbNodeConfiguration.class));
        willCallRealMethod().given(node).saveAttr(any(), eq(ctxMock), any(TbMsg.class), anyString(), anyBoolean());

        node.init(ctxMock, tbNodeConfiguration);

        TbMsgMetaData md = new TbMsgMetaData();
        if (mdValue != null) {
            md.putValue(NOTIFY_DEVICE_METADATA_KEY, mdValue);
        }
        // dummy list with one ts kv to pass the empty list check.
        var testTbMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, md, TbMsg.EMPTY_STRING);
        List<AttributeKvEntry> testAttrList = List.of(new BaseAttributeKvEntry(0L, new StringDataEntry("testKey", "testValue")));

        node.saveAttr(testAttrList, ctxMock, testTbMsg, DataConstants.SHARED_SCOPE, false);

        ArgumentCaptor<Boolean> notifyDeviceCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(telemetryServiceMock, times(1)).saveAndNotify(
                eq(tenantId), eq(deviceId), eq(DataConstants.SHARED_SCOPE),
                eq(testAttrList), notifyDeviceCaptor.capture(), any()
        );
        boolean notifyDevice = notifyDeviceCaptor.getValue();
        assertThat(notifyDevice).isEqualTo(expectedArgumentValue);
    }


    // Rule nodes upgrade
    /** 参数源方法：{@code givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"false\",\"sendAttributesUpdatedNotification\":\"false\"}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":false}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        false,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}"),
                // all flags are booleans
                Arguments.of(1,
                        "{\"scope\":\"SHARED_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        false,
                        "{\"scope\":\"SHARED_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}"),
                // no boolean flags set
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\"}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}"),
                // all flags are boolean strings
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"false\",\"sendAttributesUpdatedNotification\":\"false\",\"updateAttributesOnlyOnValueChange\":\"true\"}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}"),
                // at least one flag is boolean string
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"false\",\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}"),
                // notify device flag is null
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"null\",\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}")
        );
    }

    /** 实现方法：{@code getTestNode} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
/*
 * 本类总结：{@code TbMsgAttributesNodeTest} 为 {@code TbMsgAttributesNode} 的 遥测与属性节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
