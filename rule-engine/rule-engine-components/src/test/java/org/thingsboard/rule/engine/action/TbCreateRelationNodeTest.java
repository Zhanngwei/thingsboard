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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbCreateRelationNodeTest} 覆盖的 动作节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbCreateRelationNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbCreateRelationNodeTest {

    /** 可变 fixture 字段：{@code node} 保存 {@code TbCreateRelationNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbCreateRelationNode node;

    /** Mock 依赖字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctx;
    /** Mock 依赖字段：{@code assetService} 保存 {@code AssetService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AssetService assetService;
    /** Mock 依赖字段：{@code relationService} 保存 {@code RelationService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RelationService relationService;

    /** 可变 fixture 字段：{@code msg} 保存 {@code TbMsg} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsg msg;

    /** 可变 fixture 字段：{@code ruleChainId} 保存 {@code RuleChainId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    /** 可变 fixture 字段：{@code ruleNodeId} 保存 {@code RuleNodeId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    /** 可变 fixture 字段：{@code dbExecutor} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private ListeningExecutor dbExecutor;

    /**
     * 生命周期方法：{@code before} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Before
    public void before() {
        dbExecutor = new TestDbCallbackExecutor();
    }

    /**
     * 测试方法：覆盖 {@code testCreateNewRelation} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testCreateNewRelation() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        init(createRelationNodeConfig());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    /**
     * 测试方法：覆盖 {@code testDeleteCurrentRelationsCreateNewRelation} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testDeleteCurrentRelationsCreateNewRelation() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        init(createRelationNodeConfigWithRemoveCurrentRelations());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        EntityRelation relation = new EntityRelation();
        when(ctx.getRelationService().findByToAndTypeAsync(any(), eq(msg.getOriginator()), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(Collections.singletonList(relation)));
        when(ctx.getRelationService().deleteRelationAsync(any(), eq(relation))).thenReturn(Futures.immediateFuture(true));
        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    /**
     * 测试方法：覆盖 {@code testCreateNewRelationAndChangeOriginator} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testCreateNewRelationAndChangeOriginator() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        init(createRelationNodeConfigWithChangeOriginator());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(ctx).transformMsgOriginator(msgCaptor.capture(), originatorCaptor.capture());

        assertEquals(assetId, originatorCaptor.getValue());
    }

    /**
     * 辅助方法：{@code init} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    public void init(TbCreateRelationNodeConfiguration configuration) throws TbNodeException {
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(configuration));

        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getAssetService()).thenReturn(assetService);

        node = new TbCreateRelationNode();
        node.init(ctx, nodeConfiguration);
    }

    /**
     * 辅助方法：{@code createRelationNodeConfig} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbCreateRelationNodeConfiguration createRelationNodeConfig() {
        TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType(EntityRelation.CONTAINS_TYPE);
        configuration.setEntityCacheExpiration(300);
        configuration.setEntityType(EntityType.ASSET.name());
        configuration.setEntityNamePattern("${name}");
        configuration.setEntityTypePattern("${type}");
        configuration.setCreateEntityIfNotExists(false);
        configuration.setChangeOriginatorToRelatedEntity(false);
        configuration.setRemoveCurrentRelations(false);
        return configuration;
    }

    /**
     * 辅助方法：{@code createRelationNodeConfigWithRemoveCurrentRelations} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithRemoveCurrentRelations() {
        TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setRemoveCurrentRelations(true);
        return configuration;
    }

    /**
     * 辅助方法：{@code createRelationNodeConfigWithChangeOriginator} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithChangeOriginator() {
        TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setChangeOriginatorToRelatedEntity(true);
        return configuration;
    }
}
/*
 * 本类总结：{@code TbCreateRelationNodeTest} 为 {@code TbCreateRelationNode} 的 动作节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
