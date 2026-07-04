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
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.user.UserService;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code EntitiesCustomerIdAsyncLoaderTest} 覆盖的 规则引擎工具组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code EntitiesCustomerIdAsyncLoader}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class EntitiesCustomerIdAsyncLoaderTest {

    /** 测试常量字段：{@code SUPPORTED_ENTITY_TYPES} 保存 {@code EnumSet<EntityType>} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final EnumSet<EntityType> SUPPORTED_ENTITY_TYPES = EnumSet.of(
            EntityType.CUSTOMER,
            EntityType.USER,
            EntityType.ASSET,
            EntityType.DEVICE
    );
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code userServiceMock} 保存 {@code UserService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private UserService userServiceMock;
    /** Mock 依赖字段：{@code assetServiceMock} 保存 {@code AssetService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AssetService assetServiceMock;
    /** Mock 依赖字段：{@code deviceServiceMock} 保存 {@code DeviceService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DeviceService deviceServiceMock;

    /**
     * 测试方法：覆盖 {@code givenCustomerEntityType_whenFindEntityIdAsync_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenCustomerEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var customer = new Customer(new CustomerId(UUID.randomUUID()));

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, customer.getId()).get();

        // THEN
        assertEquals(customer.getId(), actualCustomerId);
    }

    /**
     * 测试方法：覆盖 {@code givenUserEntityType_whenFindEntityIdAsync_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenUserEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var user = new User(new UserId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        user.setCustomerId(expectedCustomerId);

        when(ctxMock.getUserService()).thenReturn(userServiceMock);
        doReturn(Futures.immediateFuture(user)).when(userServiceMock).findUserByIdAsync(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, user.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    /**
     * 测试方法：覆盖 {@code givenAssetEntityType_whenFindEntityIdAsync_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenAssetEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var asset = new Asset(new AssetId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        asset.setCustomerId(expectedCustomerId);

        when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
        doReturn(Futures.immediateFuture(asset)).when(assetServiceMock).findAssetByIdAsync(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, asset.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    /**
     * 测试方法：覆盖 {@code givenDeviceEntityType_whenFindEntityIdAsync_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenDeviceEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var device = new Device(new DeviceId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        device.setCustomerId(expectedCustomerId);

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        doReturn(device).when(deviceServiceMock).findDeviceById(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, device.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    /**
     * 测试方法：覆盖 {@code givenUnsupportedEntityTypes_whenFindEntityIdAsync_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenUnsupportedEntityTypes_whenFindEntityIdAsync_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        for (var entityType : EntityType.values()) {
            if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
                var entityId = EntityIdFactory.getByTypeAndUuid(entityType, UUID.randomUUID());

                var expectedExceptionMsg = "org.thingsboard.rule.engine.api.TbNodeException: Unexpected originator EntityType: " + entityType;

                var exception = assertThrows(ExecutionException.class,
                        () -> EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, entityId).get());

                assertInstanceOf(TbNodeException.class, exception.getCause());
                assertEquals(expectedExceptionMsg, exception.getMessage());
            }
        }
    }

}
/*
 * 本类总结：{@code EntitiesCustomerIdAsyncLoaderTest} 为 {@code EntitiesCustomerIdAsyncLoader} 的 规则引擎工具组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
