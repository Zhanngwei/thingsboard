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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * 测试目标：验证 {@code EntitiesRelatedEntityIdAsyncLoaderTest} 覆盖的 规则引擎工具组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code EntitiesRelatedEntityIdAsyncLoader}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class EntitiesRelatedEntityIdAsyncLoaderTest {

    /** 测试常量字段：{@code ASSET_ORIGINATOR_ID} 保存 {@code EntityId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final EntityId ASSET_ORIGINATOR_ID = new AssetId(UUID.randomUUID());
    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();

    /** 可变 fixture 字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbContext ctxMock;
    /** 可变 fixture 字段：{@code relationServiceMock} 保存 {@code RelationService} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RelationService relationServiceMock;

    /** 可变 fixture 字段：{@code relationsQuery} 保存 {@code RelationsQuery} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RelationsQuery relationsQuery;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() {
        ctxMock = mock(TbContext.class);
        relationServiceMock = mock(RelationService.class);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter entityTypeFilter = new RelationEntityTypeFilter(
                EntityRelation.CONTAINS_TYPE, Collections.emptyList()
        );
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));
    }

    /**
     * 测试方法：覆盖 {@code givenRelationsQuery_whenFindEntityAsync_ShouldBuildCorrectEntityRelationsQuery} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenRelationsQuery_whenFindEntityAsync_ShouldBuildCorrectEntityRelationsQuery() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(null));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        verify(relationServiceMock, times(1)).findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery));
    }


    /**
     * 测试方法：覆盖 {@code givenSeveralEntitiesFound_whenFindEntityAsync_ShouldKeepOneAndDiscardOthers} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenSeveralEntitiesFound_whenFindEntityAsync_ShouldKeepOneAndDiscardOthers() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        var device1 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 1");
        var device2 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 2");
        var device3 = new Device(new DeviceId(UUID.randomUUID()));
        device3.setName("Device 3");

        var entityRelationDevice1 = new EntityRelation();
        entityRelationDevice1.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice1.setTo(device1.getId());
        entityRelationDevice1.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice2 = new EntityRelation();
        entityRelationDevice2.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice2.setTo(device2.getId());
        entityRelationDevice2.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice3 = new EntityRelation();
        entityRelationDevice3.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice3.setTo(device3.getId());
        entityRelationDevice3.setType(EntityRelation.CONTAINS_TYPE);

        var expectedEntityRelationsList = List.of(entityRelationDevice1, entityRelationDevice2, entityRelationDevice3);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(expectedEntityRelationsList));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var deviceIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        assertNotNull(deviceIdFuture);

        var actualDeviceId = deviceIdFuture.get();
        assertNotNull(actualDeviceId);
        assertEquals(device1.getId(), actualDeviceId);
    }


    /**
     * 测试方法：覆盖 {@code givenRelationQuery_whenFindEntityAsync_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenOK() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));
        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, ASSET_ORIGINATOR_ID);
    }

    /**
     * 测试方法：覆盖 {@code givenRelationQuery_whenFindEntityAsync_thenReturnNull} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenReturnNull() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, null);
    }

    /**
     * 测试方法：覆盖 {@code givenRelationQuery_whenFindEntityAsync_thenFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenFailure() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        relationsQuery.setDirection(null);
        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));

        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, ASSET_ORIGINATOR_ID);
    }

    /**
     * 辅助方法：{@code verifyEntityIdFuture} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void verifyEntityIdFuture(ListenableFuture<EntityId> entityIdFuture, EntityId assetId) {
        withCallback(entityIdFuture,
                entityId -> assertThat(entityId).isEqualTo(assetId),
                throwable -> assertThat(throwable).isInstanceOf(IllegalStateException.class), ctxMock.getDbCallbackExecutor());
    }

    /**
     * 辅助方法：{@code createEntityRelation} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private static EntityRelation createEntityRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.CONTAINS_TYPE);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        return relation;
    }
}
/*
 * 本类总结：{@code EntitiesRelatedEntityIdAsyncLoaderTest} 为 {@code EntitiesRelatedEntityIdAsyncLoader} 的 规则引擎工具组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
