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
 * `EntitiesRelatedEntityIdAsyncLoaderTest` 测试类，用于验证 `EntitiesRelatedEntityIdAsyncLoader` 相关行为。
 */
public class EntitiesRelatedEntityIdAsyncLoaderTest {

    /**
     * 资产ID常量，用于统一引用固定值。
     */
    private static final EntityId ASSET_ORIGINATOR_ID = new AssetId(UUID.randomUUID());
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /**
     * 执行器常量，用于统一引用固定值。
     */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctxMock;
    /**
     * 关系，提供当前类调用的业务操作。
     */
    private RelationService relationServiceMock;

    /**
     * 查询条件，表示当前对象的对应属性。
     */
    private RelationsQuery relationsQuery;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
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
     * 功能：验证 `givenRelationsQuery_whenFindEntityAsync_ShouldBuildCorrectEntityRelationsQuery` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenRelationsQuery_whenFindEntityAsync_ShouldBuildCorrectEntityRelationsQuery() {
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
     * 功能：验证 `givenSeveralEntitiesFound_whenFindEntityAsync_ShouldKeepOneAndDiscardOthers` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenSeveralEntitiesFound_whenFindEntityAsync_ShouldKeepOneAndDiscardOthers() throws Exception {
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
     * 功能：验证 `givenRelationQuery_whenFindEntityAsync_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenOK() {
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
     * 功能：验证 `givenRelationQuery_whenFindEntityAsync_thenReturnNull` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenReturnNull() {
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, null);
    }

    /**
     * 功能：验证 `givenRelationQuery_whenFindEntityAsync_thenFailure` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenFailure() {
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
     * 功能：校验实体ID。
     * 参数：
     * - `entityIdFuture`：实体对象。
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    private void verifyEntityIdFuture(ListenableFuture<EntityId> entityIdFuture, EntityId assetId) {
        withCallback(entityIdFuture,
                entityId -> assertThat(entityId).isEqualTo(assetId),
                throwable -> assertThat(throwable).isInstanceOf(IllegalStateException.class), ctxMock.getDbCallbackExecutor());
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * 返回：处理结果。
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
