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
import com.google.common.util.concurrent.Futures;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbGetRelatedAttributeNodeTest} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbGetRelatedAttributeNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class TbGetRelatedAttributeNodeTest {

    /** 测试常量字段：{@code DUMMY_DEVICE_ORIGINATOR} 保存 {@code EntityId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final EntityId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code attributesServiceMock} 保存 {@code AttributesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AttributesService attributesServiceMock;
    /** Mock 依赖字段：{@code timeseriesServiceMock} 保存 {@code TimeseriesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TimeseriesService timeseriesServiceMock;
    /** Mock 依赖字段：{@code relationServiceMock} 保存 {@code RelationService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RelationService relationServiceMock;
    /** Mock 依赖字段：{@code deviceServiceMock} 保存 {@code DeviceService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DeviceService deviceServiceMock;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbGetRelatedAttributeNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGetRelatedAttributeNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbGetRelatedDataNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGetRelatedDataNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbNodeConfiguration nodeConfiguration;
    /** 可变 fixture 字段：{@code entityRelation} 保存 {@code EntityRelation} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private EntityRelation entityRelation;
    /** 可变 fixture 字段：{@code msg} 保存 {@code TbMsg} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsg msg;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void setUp() {
        node = new TbGetRelatedAttributeNode();
        config = new TbGetRelatedDataNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        entityRelation = new EntityRelation();
    }

    /**
     * 测试方法：覆盖 {@code givenConfigWithNullFetchTo_whenInit_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenConfigWithNullFetchTo_whenInit_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setFetchTo(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 测试方法：覆盖 {@code givenConfigWithNullDataToFetch_whenInit_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenConfigWithNullDataToFetch_whenInit_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setDataToFetch(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("DataToFetch property cannot be null! Supported values are: " + Arrays.toString(DataToFetch.values()));
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenInit_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDefaultConfig_whenInit_thenOK() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        var nodeConfig = (TbGetRelatedDataNodeConfiguration) node.config;
        assertThat(nodeConfig).isEqualTo(config);
        assertThat(nodeConfig.getDataMapping()).isEqualTo(Map.of("serialNumber", "sn"));
        assertThat(nodeConfig.getDataToFetch()).isEqualTo(DataToFetch.ATTRIBUTES);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.METADATA);

        var relationsQuery = new RelationsQuery();
        var relationEntityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        relationsQuery.setFilters(Collections.singletonList(relationEntityTypeFilter));

        assertThat(nodeConfig.getRelationsQuery()).isEqualTo(relationsQuery);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfig_whenInit_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenCustomConfig_whenInit_thenOK() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        config.setDataMapping(Map.of(
                "sourceAttr1", "targetKey1",
                "sourceAttr2", "targetKey2",
                "sourceAttr3", "targetKey3"));
        config.setDataToFetch(DataToFetch.LATEST_TELEMETRY);
        config.setFetchTo(TbMsgSource.DATA);

        var relationsQuery = new RelationsQuery();
        var relationEntityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        relationsQuery.setFilters(Collections.singletonList(relationEntityTypeFilter));

        config.setRelationsQuery(relationsQuery);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        var nodeConfig = (TbGetRelatedDataNodeConfiguration) node.config;
        assertThat(nodeConfig).isEqualTo(config);
        assertThat(nodeConfig.getDataMapping()).isEqualTo(Map.of(
                "sourceAttr1", "targetKey1",
                "sourceAttr2", "targetKey2",
                "sourceAttr3", "targetKey3"
        ));
        assertThat(nodeConfig.getDataToFetch()).isEqualTo(DataToFetch.LATEST_TELEMETRY);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.DATA);
        assertThat(nodeConfig.getRelationsQuery()).isEqualTo(relationsQuery);
    }

    /**
     * 测试方法：覆盖 {@code givenEmptyAttributesMapping_whenInit_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenEmptyAttributesMapping_whenInit_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var expectedExceptionMessage = "At least one mapping entry should be specified!";

        config.setDataMapping(Collections.emptyMap());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 测试方法：覆盖 {@code givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        node.fetchTo = TbMsgSource.DATA;
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 测试方法：覆盖 {@code givenDidNotFindEntity_whenOnMsg_thenShouldTellFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDidNotFindEntity_whenOnMsg_thenShouldTellFailure() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.ATTRIBUTES, DUMMY_DEVICE_ORIGINATOR);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        doReturn(Futures.immediateFuture(null)).when(relationServiceMock).findByQuery(eq(TENANT_ID), any());

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, times(1))
                .tellFailure(actualMessageCaptor.capture(), actualExceptionCaptor.capture());

        var actualMessage = actualMessageCaptor.getValue();
        var actualException = actualExceptionCaptor.getValue();

        var expectedExceptionMessage = "Failed to find related entity to message originator using relation query specified in the configuration!";

        assertEquals(msg, actualMessage);
        assertEquals(expectedExceptionMessage, actualException.getMessage());
        assertInstanceOf(NoSuchElementException.class, actualException);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchAttributesToData_whenOnMsg_thenShouldFetchAttributesToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchAttributesToData_whenOnMsg_thenShouldFetchAttributesToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var customer = new Customer(new CustomerId(UUID.randomUUID()));
        var user = new User(new UserId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.ATTRIBUTES, customer.getId());

        entityRelation.setFrom(customer.getId());
        entityRelation.setTo(user.getId());
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);

        List<AttributeKvEntry> attributes = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey1", "sourceValue1"), 1L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey2", "sourceValue2"), 2L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey3", "sourceValue3"), 3L)
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        doReturn(Futures.immediateFuture(List.of(entityRelation))).when(relationServiceMock).findByQuery(eq(TENANT_ID), any());

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(eq(TENANT_ID), eq(user.getId()), eq(DataConstants.SERVER_SCOPE), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(attributes));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42," +
                "\"humidity\":77," +
                "\"messageBodyPattern1\":\"targetKey2\"," +
                "\"messageBodyPattern2\":\"sourceKey3\"," +
                "\"targetKey1\":\"sourceValue1\"," +
                "\"targetKey2\":\"sourceValue2\"," +
                "\"targetKey3\":\"sourceValue3\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 测试方法：覆盖 {@code givenFetchAttributesToMetaData_whenOnMsg_thenShouldFetchAttributesToMetaData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchAttributesToMetaData_whenOnMsg_thenShouldFetchAttributesToMetaData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var firstCustomer = new Customer(new CustomerId(UUID.randomUUID()));
        var secondCustomer = new Customer(new CustomerId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.ATTRIBUTES, firstCustomer.getId());

        entityRelation.setFrom(firstCustomer.getId());
        entityRelation.setTo(secondCustomer.getId());
        entityRelation.setType(EntityRelation.MANAGES_TYPE);

        List<AttributeKvEntry> attributes = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey1", "sourceValue1"), 1L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey2", "sourceValue2"), 2L),
                new BaseAttributeKvEntry(new StringDataEntry("sourceKey3", "sourceValue3"), 3L)
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        doReturn(Futures.immediateFuture(List.of(entityRelation))).when(relationServiceMock).findByQuery(eq(TENANT_ID), any());

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(eq(TENANT_ID), eq(secondCustomer.getId()), eq(DataConstants.SERVER_SCOPE), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(attributes));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "metaDataPattern1", "sourceKey2",
                "metaDataPattern2", "targetKey3",
                "targetKey1", "sourceValue1",
                "targetKey2", "sourceValue2",
                "targetKey3", "sourceValue3"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchTelemetryToData_whenOnMsg_thenShouldFetchTelemetryToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchTelemetryToData_whenOnMsg_thenShouldFetchTelemetryToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var dashboard = new Dashboard(new DashboardId(UUID.randomUUID()));
        var entityView = new EntityView(new EntityViewId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.LATEST_TELEMETRY, dashboard.getId());

        entityRelation.setFrom(dashboard.getId());
        entityRelation.setTo(entityView.getId());
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);

        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey1", "sourceValue1")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey2", "sourceValue2")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey3", "sourceValue3"))
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        doReturn(Futures.immediateFuture(List.of(entityRelation))).when(relationServiceMock).findByQuery(eq(TENANT_ID), any());

        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(timeseriesServiceMock.findLatest(eq(TENANT_ID), eq(entityView.getId()), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(timeseries));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42," +
                "\"humidity\":77," +
                "\"messageBodyPattern1\":\"targetKey2\"," +
                "\"messageBodyPattern2\":\"sourceKey3\"," +
                "\"targetKey1\":\"sourceValue1\"," +
                "\"targetKey2\":\"sourceValue2\"," +
                "\"targetKey3\":\"sourceValue3\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 测试方法：覆盖 {@code givenFetchTelemetryToMetaData_whenOnMsg_thenShouldFetchTelemetryToMetaData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchTelemetryToMetaData_whenOnMsg_thenShouldFetchTelemetryToMetaData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var tenant = new Tenant(new TenantId(UUID.randomUUID()));
        var device = new Device(new DeviceId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.LATEST_TELEMETRY, tenant.getId());

        entityRelation.setFrom(tenant.getId());
        entityRelation.setTo(device.getId());
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);

        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey1", "sourceValue1")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey2", "sourceValue2")),
                new BasicTsKvEntry(1L, new StringDataEntry("sourceKey3", "sourceValue3"))
        );
        var expectedPatternProcessedKeysList = List.of("sourceKey1", "sourceKey2", "sourceKey3");

        when(ctxMock.getTenantId()).thenReturn(tenant.getId());

        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        doReturn(Futures.immediateFuture(List.of(entityRelation))).when(relationServiceMock).findByQuery(eq(tenant.getId()), any());

        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(timeseriesServiceMock.findLatest(eq(tenant.getId()), eq(device.getId()), argThat(new ListMatcher<>(expectedPatternProcessedKeysList))))
                .thenReturn(Futures.immediateFuture(timeseries));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "metaDataPattern1", "sourceKey2",
                "metaDataPattern2", "targetKey3",
                "targetKey1", "sourceValue1",
                "targetKey2", "sourceValue2",
                "targetKey3", "sourceValue3"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    /**
     * 测试方法：覆盖 {@code givenFetchFieldsToData_whenOnMsg_thenShouldFetchFieldsToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchFieldsToData_whenOnMsg_thenShouldFetchFieldsToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setName("Device Name");
        var asset = new Asset(new AssetId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.DATA, DataToFetch.FIELDS, asset.getId());

        entityRelation.setFrom(asset.getId());
        entityRelation.setTo(device.getId());
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        doReturn(Futures.immediateFuture(List.of(entityRelation))).when(relationServiceMock).findByQuery(eq(TENANT_ID), any());

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(deviceServiceMock.findDeviceById(eq(TENANT_ID), eq(device.getId()))).thenReturn(device);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42,\"humidity\":77,\"messageBodyPattern\":\"relatedEntityId\"," +
                "\"relatedEntityId\":\"" + device.getId().getId() + "\",\"relatedEntityName\":\"" + device.getName() + "\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 测试方法：覆盖 {@code givenFetchFieldsToMetadata_whenOnMsg_thenShouldFetchFieldsToMetadata} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenFetchFieldsToMetadata_whenOnMsg_thenShouldFetchFieldsToMetadata() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setName("Device Name");
        var asset = new Asset(new AssetId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.METADATA, DataToFetch.FIELDS, asset.getId());

        entityRelation.setFrom(asset.getId());
        entityRelation.setTo(device.getId());
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        doReturn(Futures.immediateFuture(List.of(entityRelation))).when(relationServiceMock).findByQuery(eq(TENANT_ID), any());

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(deviceServiceMock.findDeviceById(eq(TENANT_ID), eq(device.getId()))).thenReturn(device);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetadata = new TbMsgMetaData(Map.of(
                "metaDataPattern", "relatedEntityName",
                "relatedEntityId", device.getId().getId().toString(),
                "relatedEntityName", device.getName()
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetadata);
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
        var defaultConfig = new TbGetRelatedDataNodeConfiguration().defaultConfiguration();
        var node = new TbGetRelatedAttributeNode();
        String oldConfig = "{\"attrMapping\":{\"serialNumber\":\"sn\"}," +
                "\"relationsQuery\":{\"direction\":\"FROM\",\"maxLevel\":1," +
                "\"filters\":[{\"relationType\":\"Contains\",\"entityTypes\":[]}]," +
                "\"fetchLastLevelOnly\":false}," +
                "\"telemetry\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 辅助方法：{@code prepareMsgAndConfig} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void prepareMsgAndConfig(TbMsgSource fetchTo, DataToFetch dataToFetch, EntityId originator) {

        config.setDataToFetch(dataToFetch);
        config.setFetchTo(fetchTo);
        node.config = config;
        node.fetchTo = fetchTo;
        var msgMetaData = new TbMsgMetaData();
        String msgData;
        if (dataToFetch.equals(DataToFetch.FIELDS)) {
            config.setDataMapping(Map.of(
                    "id", "$[messageBodyPattern]",
                    "name", "${metaDataPattern}"));
            msgMetaData.putValue("metaDataPattern", "relatedEntityName");
            msgData = "{\"temp\":42,\"humidity\":77,\"messageBodyPattern\":\"relatedEntityId\"}";
        } else {
            config.setDataMapping(Map.of(
                    "sourceKey1", "targetKey1",
                    "${metaDataPattern1}", "$[messageBodyPattern1]",
                    "$[messageBodyPattern2]", "${metaDataPattern2}"));
            msgMetaData.putValue("metaDataPattern1", "sourceKey2");
            msgMetaData.putValue("metaDataPattern2", "targetKey3");
            msgData = "{\"temp\":42,\"humidity\":77,\"messageBodyPattern1\":\"targetKey2\",\"messageBodyPattern2\":\"sourceKey3\"}";
        }

        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, msgMetaData, msgData);
    }

    /**
     * 测试目标：验证 {@code ListMatcher} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
     * 所属生产节点/组件：{@code ListMatcher}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
     * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
     * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
     * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
     */
    @RequiredArgsConstructor
    private static class ListMatcher<T> implements ArgumentMatcher<List<T>> {

        /** 固定 fixture 字段：{@code expectedList} 保存 {@code List<T>} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
        private final List<T> expectedList;

        /** 实现方法：{@code matches} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
        @Override
        public boolean matches(List<T> actualList) {
            if (actualList == expectedList) {
                return true;
            }
            if (actualList.size() != expectedList.size()) {
                return false;
            }
            return actualList.containsAll(expectedList);
        }

    }

}
/*
 * 本类总结：{@code TbGetRelatedAttributeNodeTest} 为 {@code TbGetRelatedAttributeNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
