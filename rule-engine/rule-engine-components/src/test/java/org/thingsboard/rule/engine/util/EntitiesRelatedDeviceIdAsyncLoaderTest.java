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
 * 测试目标：验证 {@code EntitiesRelatedDeviceIdAsyncLoaderTest} 覆盖的 规则引擎工具组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code EntitiesRelatedDeviceIdAsyncLoader}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class EntitiesRelatedDeviceIdAsyncLoaderTest {

    /** 测试常量字段：{@code DUMMY_ORIGINATOR} 保存 {@code EntityId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final EntityId DUMMY_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code deviceServiceMock} 保存 {@code DeviceService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DeviceService deviceServiceMock;

    /**
     * 测试方法：覆盖 {@code givenDeviceRelationsQuery_whenFindDeviceAsync_ShouldBuildCorrectDeviceSearchQuery} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenDeviceRelationsQuery_whenFindDeviceAsync_ShouldBuildCorrectDeviceSearchQuery() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenSeveralDevicesFound_whenFindDeviceAsync_ShouldKeepOneAndDiscardOthers} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenSeveralDevicesFound_whenFindDeviceAsync_ShouldKeepOneAndDiscardOthers() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
