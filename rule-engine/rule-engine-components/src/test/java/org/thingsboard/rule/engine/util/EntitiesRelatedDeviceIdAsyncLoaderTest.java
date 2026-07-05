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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `EntitiesRelatedDeviceIdAsyncLoaderTest` 测试类，用于验证 `EntitiesRelatedDeviceIdAsyncLoader` 相关行为。
 */
@ExtendWith(MockitoExtension.class)
public class EntitiesRelatedDeviceIdAsyncLoaderTest {

    /**
     * `DUMMY_ORIGINATOR`常量，用于统一引用固定值。
     */
    private static final EntityId DUMMY_ORIGINATOR = new DeviceId(UUID.randomUUID());
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
    @Mock
    private TbContext ctxMock;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Mock
    private DeviceService deviceServiceMock;

    /**
     * 功能：验证 `givenDeviceRelationsQuery_whenFindDeviceAsync_ShouldBuildCorrectDeviceSearchQuery` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceRelationsQuery_whenFindDeviceAsync_ShouldBuildCorrectDeviceSearchQuery() {
        // GIVEN
        var deviceRelationsQuery = new DeviceRelationsQuery();
        deviceRelationsQuery.setDeviceTypes(List.of("Device type 1", "Device type 2", "default"));
        deviceRelationsQuery.setDirection(EntitySearchDirection.FROM);
        deviceRelationsQuery.setMaxLevel(2);
        deviceRelationsQuery.setRelationType(EntityRelation.CONTAINS_TYPE);

        var expectedDeviceSearchQuery = new DeviceSearchQuery();
        var parameters = new RelationsSearchParameters(
                DUMMY_ORIGINATOR,
                deviceRelationsQuery.getDirection(),
                deviceRelationsQuery.getMaxLevel(),
                deviceRelationsQuery.isFetchLastLevelOnly()
        );
        expectedDeviceSearchQuery.setParameters(parameters);
        expectedDeviceSearchQuery.setRelationType(deviceRelationsQuery.getRelationType());
        expectedDeviceSearchQuery.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDevicesByQuery(eq(TENANT_ID), eq(expectedDeviceSearchQuery)))
                .thenReturn(Futures.immediateFuture(null));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        EntitiesRelatedDeviceIdAsyncLoader.findDeviceAsync(ctxMock, DUMMY_ORIGINATOR, deviceRelationsQuery);

        // THEN
        verify(deviceServiceMock, times(1)).findDevicesByQuery(eq(TENANT_ID), eq(expectedDeviceSearchQuery));
    }

    /**
     * 功能：验证 `givenSeveralDevicesFound_whenFindDeviceAsync_ShouldKeepOneAndDiscardOthers` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenSeveralDevicesFound_whenFindDeviceAsync_ShouldKeepOneAndDiscardOthers() throws Exception {
        // GIVEN
        var deviceRelationsQuery = new DeviceRelationsQuery();
        deviceRelationsQuery.setDeviceTypes(List.of("Device type 1", "Device type 2", "default"));
        deviceRelationsQuery.setDirection(EntitySearchDirection.FROM);
        deviceRelationsQuery.setMaxLevel(2);
        deviceRelationsQuery.setRelationType(EntityRelation.CONTAINS_TYPE);

        var expectedDeviceSearchQuery = new DeviceSearchQuery();
        var parameters = new RelationsSearchParameters(
                DUMMY_ORIGINATOR,
                deviceRelationsQuery.getDirection(),
                deviceRelationsQuery.getMaxLevel(),
                deviceRelationsQuery.isFetchLastLevelOnly()
        );
        expectedDeviceSearchQuery.setParameters(parameters);
        expectedDeviceSearchQuery.setRelationType(deviceRelationsQuery.getRelationType());
        expectedDeviceSearchQuery.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());

        var device1 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 1");
        var device2 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 2");
        var device3 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 3");

        var devicesList = List.of(device1, device2, device3);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDevicesByQuery(eq(TENANT_ID), eq(expectedDeviceSearchQuery)))
                .thenReturn(Futures.immediateFuture(devicesList));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var entityIdFuture = EntitiesRelatedDeviceIdAsyncLoader.findDeviceAsync(ctxMock, DUMMY_ORIGINATOR, deviceRelationsQuery);

        // THEN
        assertNotNull(entityIdFuture);

        var actualEntityId = entityIdFuture.get();
        assertNotNull(actualEntityId);
        assertEquals(device1.getId(), actualEntityId);
    }

}
/*
 * 本类总结：{@code EntitiesRelatedDeviceIdAsyncLoaderTest} 为 {@code EntitiesRelatedDeviceIdAsyncLoader} 的 规则引擎工具组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
