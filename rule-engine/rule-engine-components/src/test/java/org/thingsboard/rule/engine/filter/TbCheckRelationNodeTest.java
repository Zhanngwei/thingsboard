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
 * `TbCheckRelationNodeTest` 测试类，用于验证 `TbCheckRelationNode` 相关行为。
 */
class TbCheckRelationNodeTest {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /**
     * `ORIGINATOR_ID`常量，用于统一引用固定值。
     */
    private static final DeviceId ORIGINATOR_ID = new DeviceId(UUID.randomUUID());
    /**
     * 执行器常量，用于统一引用固定值。
     */
    private static final TestDbCallbackExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final TbMsg EMPTY_POST_ATTRIBUTES_MSG = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, ORIGINATOR_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbCheckRelationNode node;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;
    /**
     * 关系，提供当前类调用的业务操作。
     */
    private RelationService relationService;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
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
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    /**
     * 功能：验证 `givenDefaultConfig_whenInit_then_throwException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDefaultConfig_whenInit_then_throwException() {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Entity should be specified!");
    }

    /**
     * 功能：验证 `givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_True() throws TbNodeException {
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
     * 功能：验证 `givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_False() throws TbNodeException {
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
     * 功能：验证 `givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_True() throws TbNodeException {
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
     * 功能：验证 `givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_False() throws TbNodeException {
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
     * 功能：验证 `givenCustomConfig_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfig_whenOnMsg_then_True() throws TbNodeException {
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
     * 功能：验证 `givenCustomConfig_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfig_whenOnMsg_then_False() throws TbNodeException {
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
     * 功能：验证 `givenCustomConfigDirectionTo_whenOnMsg_then_True` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigDirectionTo_whenOnMsg_then_True() throws TbNodeException {
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
     * 功能：验证 `givenCustomConfigDirectionTo_whenOnMsg_then_False` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCustomConfigDirectionTo_whenOnMsg_then_False() throws TbNodeException {
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
     * 功能：验证 `givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
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
