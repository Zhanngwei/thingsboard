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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbGetAttributesNodeTest} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbGetAttributesNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbGetAttributesNodeTest {

    /** 测试常量字段：{@code ORIGINATOR} 保存 {@code EntityId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final EntityId ORIGINATOR = new DeviceId(Uuids.timeBased());
    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = TenantId.fromUUID(Uuids.timeBased());
    /** 可变 fixture 字段：{@code dbExecutor} 保存 {@code AbstractListeningExecutor} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private AbstractListeningExecutor dbExecutor;

    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code attributesServiceMock} 保存 {@code AttributesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AttributesService attributesServiceMock;
    /** Mock 依赖字段：{@code timeseriesServiceMock} 保存 {@code TimeseriesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TimeseriesService timeseriesServiceMock;

    /** 可变 fixture 字段：{@code clientAttributes} 保存 {@code List<String>} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private List<String> clientAttributes;
    /** 可变 fixture 字段：{@code serverAttributes} 保存 {@code List<String>} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private List<String> serverAttributes;
    /** 可变 fixture 字段：{@code sharedAttributes} 保存 {@code List<String>} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private List<String> sharedAttributes;
    /** 可变 fixture 字段：{@code tsKeys} 保存 {@code List<String>} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private List<String> tsKeys;
    /** 可变 fixture 字段：{@code ts} 保存 {@code long} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private long ts;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbGetAttributesNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGetAttributesNode node;

    /**
     * 生命周期方法：{@code before} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Before
    public void before() throws TbNodeException {
        dbExecutor = new AbstractListeningExecutor() {
            /** 实现方法：{@code getThreadPollSize} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

        clientAttributes = getAttributeNames("client");
        serverAttributes = getAttributeNames("server");
        sharedAttributes = getAttributeNames("shared");
        tsKeys = List.of("temperature", "humidity", "unknown");
        ts = System.currentTimeMillis();

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.CLIENT_SCOPE, clientAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(clientAttributes, ts)));

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.SERVER_SCOPE, serverAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(serverAttributes, ts)));

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.SHARED_SCOPE, sharedAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(sharedAttributes, ts)));

        when(timeseriesServiceMock.findLatest(TENANT_ID, ORIGINATOR, tsKeys))
                .thenReturn(Futures.immediateFuture(getListTsKvEntry(tsKeys, ts)));
    }

    /**
     * 生命周期方法：{@code after} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @After
    public void after() {
        dbExecutor.destroy();
    }

    /**
     * 测试方法：覆盖 {@code givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node = initNode(TbMsgSource.METADATA, false, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.METADATA, false, tsKeys);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchLatestTimeseriesToMetadata_whenOnMsg_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchLatestTimeseriesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node = initNode(TbMsgSource.METADATA, true, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.METADATA, true, tsKeys);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchAttributesToData_whenOnMsg_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchAttributesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node = initNode(TbMsgSource.DATA, false, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.DATA, false, tsKeys);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellSuccess} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.DATA, true, tsKeys);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellFailure() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node = initNode(TbMsgSource.METADATA, false, true);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(actualMsg, TbMsgSource.METADATA, false, tsKeys);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellFailure() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, true);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(actualMsg, TbMsgSource.DATA, true, tsKeys);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchLatestTimeseriesToDataAndDataIsNotJsonObject_whenOnMsg_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchLatestTimeseriesToDataAndDataIsNotJsonObject_whenOnMsg_thenException() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, true);
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, ORIGINATOR, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        verify(ctxMock, never()).tellSuccess(any());
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
    }

    /**
     * 测试方法：覆盖 {@code givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 测试方法：覆盖 {@code givenOldConfigWithNoFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenOldConfigWithNoFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 测试方法：覆盖 {@code givenOldConfigWithNullFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenOldConfigWithNullFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"fetchToData\":null," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 辅助方法：{@code checkMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg checkMsg(boolean checkSuccess) {
        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        if (checkSuccess) {
            verify(ctxMock, timeout(5000)).tellSuccess(msgCaptor.capture());
        } else {
            var exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
            verify(ctxMock, never()).tellSuccess(any());
            verify(ctxMock, timeout(5000)).tellFailure(msgCaptor.capture(), exceptionCaptor.capture());
            var exception = exceptionCaptor.getValue();
            assertNotNull(exception);
            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().startsWith("The following attribute/telemetry keys is not present in the DB:"));
        }

        var resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getMetaData());
        assertNotNull(resultMsg.getData());
        return resultMsg;
    }

    /**
     * 辅助方法：{@code checkAttributes} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void checkAttributes(TbMsg actualMsg, TbMsgSource fetchTo, String prefix, List<String> attributes) {
        var msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        attributes.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .forEach(attribute -> {
                    String result = null;
                    if (TbMsgSource.DATA.equals(fetchTo)) {
                        result = msgData.get(prefix + attribute).asText();
                    } else if (TbMsgSource.METADATA.equals(fetchTo)) {
                        result = actualMsg.getMetaData().getValue(prefix + attribute);
                    }
                    assertNotNull(result);
                    assertEquals(attribute + "_value", result);
                });
    }

    /**
     * 辅助方法：{@code checkTs} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void checkTs(TbMsg actualMsg, TbMsgSource fetchTo, boolean getLatestValueWithTs, List<String> tsKeys) {
        var msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        long value = 1L;
        for (var key : tsKeys) {
            if (key.equals("unknown")) {
                continue;
            }
            String actualValue = null;
            String expectedValue;
            if (getLatestValueWithTs) {
                expectedValue = "{\"ts\":" + ts + ",\"value\":{\"data\":" + value + "}}";
            } else {
                expectedValue = "{\"data\":" + value + "}";
            }
            if (TbMsgSource.DATA.equals(fetchTo)) {
                actualValue = JacksonUtil.toString(msgData.get(key));
            } else if (TbMsgSource.METADATA.equals(fetchTo)) {
                actualValue = actualMsg.getMetaData().getValue(key);
            }
            assertNotNull(actualValue);
            assertEquals(expectedValue, actualValue);
            value++;
        }
    }

    /**
     * 辅助方法：{@code initNode} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbGetAttributesNode initNode(TbMsgSource fetchTo, boolean getLatestValueWithTs, boolean isTellFailureIfAbsent) throws TbNodeException {
        var config = new TbGetAttributesNodeConfiguration();
        config.setClientAttributeNames(List.of("client_attr_1", "client_attr_2", "${client_attr_metadata}", "unknown"));
        config.setServerAttributeNames(List.of("server_attr_1", "server_attr_2", "${server_attr_metadata}", "unknown"));
        config.setSharedAttributeNames(List.of("shared_attr_1", "shared_attr_2", "$[shared_attr_data]", "unknown"));
        config.setLatestTsKeyNames(List.of("temperature", "humidity", "unknown"));
        config.setFetchTo(fetchTo);
        config.setGetLatestValueWithTs(getLatestValueWithTs);
        config.setTellFailureIfAbsent(isTellFailureIfAbsent);

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var node = new TbGetAttributesNode();
        node.init(ctxMock, nodeConfiguration);
        return node;
    }

    /**
     * 辅助方法：{@code getTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getTbMsg(EntityId entityId) {
        var msgData = JacksonUtil.newObjectNode();
        msgData.put("shared_attr_data", "shared_attr_3");

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("client_attr_metadata", "client_attr_3");
        msgMetaData.putValue("server_attr_metadata", "server_attr_3");

        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, entityId, msgMetaData, JacksonUtil.toString(msgData));
    }

    /**
     * 辅助方法：{@code getAttributeNames} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private List<String> getAttributeNames(String prefix) {
        return List.of(prefix + "_attr_1", prefix + "_attr_2", prefix + "_attr_3", "unknown");
    }

    /**
     * 辅助方法：{@code getListAttributeKvEntry} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private List<AttributeKvEntry> getListAttributeKvEntry(List<String> attributesList, long ts) {
        return attributesList.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .map(attribute -> toAttributeKvEntry(ts, attribute))
                .collect(Collectors.toList());
    }

    /**
     * 辅助方法：{@code toAttributeKvEntry} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private BaseAttributeKvEntry toAttributeKvEntry(long ts, String attribute) {
        return new BaseAttributeKvEntry(ts, new StringDataEntry(attribute, attribute + "_value"));
    }

    /**
     * 辅助方法：{@code getListTsKvEntry} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private List<TsKvEntry> getListTsKvEntry(List<String> keysList, long ts) {
        long value = 1L;
        var kvEntriesList = new ArrayList<TsKvEntry>();
        for (var key : keysList) {
            if (key.equals("unknown")) {
                continue;
            }
            String dataValue = "{\"data\":" + value + "}";
            kvEntriesList.add(new BasicTsKvEntry(ts, new JsonDataEntry(key, dataValue)));
            value++;
        }
        return kvEntriesList;
    }

}
/*
 * 本类总结：{@code TbGetAttributesNodeTest} 为 {@code TbGetAttributesNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
