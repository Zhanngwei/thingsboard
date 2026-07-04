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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbCheckRelationNodeTest} 覆盖的 过滤/分流节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbCheckRelationNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
class TbCheckRelationNodeTest {

    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /** 测试常量字段：{@code ORIGINATOR_ID} 保存 {@code DeviceId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final DeviceId ORIGINATOR_ID = new DeviceId(UUID.randomUUID());
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code TestDbCallbackExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TestDbCallbackExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /** 测试常量字段：{@code EMPTY_POST_ATTRIBUTES_MSG} 保存 {@code TbMsg} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TbMsg EMPTY_POST_ATTRIBUTES_MSG = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, ORIGINATOR_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

    /** 可变 fixture 字段：{@code node} 保存 {@code TbCheckRelationNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbCheckRelationNode node;

    /** 可变 fixture 字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbContext ctx;
    /** 可变 fixture 字段：{@code relationService} 保存 {@code RelationService} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RelationService relationService;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        relationService = mock(RelationService.class);

        when(ctx.getTenantId()).thenReturn(TENANT_ID);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        node = new TbCheckRelationNode();
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
     * 测试方法：覆盖 {@code givenDefaultConfig_whenInit_then_throwException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenInit_then_throwException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Entity should be specified!");
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_True} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_True() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());

        when(relationService.checkRelationAsync(TENANT_ID, ORIGINATOR_ID, assetId, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(true));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_False} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_False() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());

        when(relationService.checkRelationAsync(TENANT_ID, ORIGINATOR_ID, assetId, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(false));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_True} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_True() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());
        config.setDirection(EntitySearchDirection.TO.name());

        when(relationService.checkRelationAsync(TENANT_ID, assetId, ORIGINATOR_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(true));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_False} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_False() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());
        config.setDirection(EntitySearchDirection.TO.name());

        when(relationService.checkRelationAsync(TENANT_ID, assetId, ORIGINATOR_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(false));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfig_whenOnMsg_then_True} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfig_whenOnMsg_then_True() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);
        var entityRelation = new EntityRelation();
        entityRelation.setFrom(ORIGINATOR_ID);
        entityRelation.setTo(new AssetId(UUID.randomUUID()));
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        entityRelation.setTypeGroup(RelationTypeGroup.COMMON);

        when(relationService.findByFromAndTypeAsync(TENANT_ID, ORIGINATOR_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByToAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfig_whenOnMsg_then_False} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfig_whenOnMsg_then_False() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);

        when(relationService.findByFromAndTypeAsync(TENANT_ID, ORIGINATOR_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByToAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfigDirectionTo_whenOnMsg_then_True} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfigDirectionTo_whenOnMsg_then_True() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);
        config.setDirection(EntitySearchDirection.TO.name());
        var entityRelation = new EntityRelation();
        entityRelation.setFrom(new AssetId(UUID.randomUUID()));
        entityRelation.setTo(ORIGINATOR_ID);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        entityRelation.setTypeGroup(RelationTypeGroup.COMMON);

        when(relationService.findByToAndTypeAsync(TENANT_ID, ORIGINATOR_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByFromAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfigDirectionTo_whenOnMsg_then_False} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenCustomConfigDirectionTo_whenOnMsg_then_False() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);
        config.setDirection(EntitySearchDirection.TO.name());

        when(relationService.findByToAndTypeAsync(TENANT_ID, ORIGINATOR_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByFromAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
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
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setEntityType(ORIGINATOR_ID.getEntityType().name());
        config.setEntityId(ORIGINATOR_ID.getId().toString());
        String oldConfig = "{\"checkForSingleEntity\":true,\"direction\":\"TO\",\"entityType\":\"" + config.getEntityType() + "\",\"entityId\":\"" + config.getEntityId() + "\",\"relationType\":\"Contains\"}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        // WHEN
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        // THEN
        assertTrue(upgrade.getFirst());
        assertEquals(config, JacksonUtil.treeToValue(upgrade.getSecond(), config.getClass()));
    }

}
/*
 * 本类总结：{@code TbCheckRelationNodeTest} 为 {@code TbCheckRelationNode} 的 过滤/分流节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
