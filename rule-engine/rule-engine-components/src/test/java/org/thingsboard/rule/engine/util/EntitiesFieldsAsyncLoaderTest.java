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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityFieldsData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code EntitiesFieldsAsyncLoaderTest} 覆盖的 规则引擎工具组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code EntitiesFieldsAsyncLoader}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class EntitiesFieldsAsyncLoaderTest {

    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /** 可变 fixture 字段：{@code SUPPORTED_ENTITY_TYPES} 保存 {@code EnumSet<EntityType>} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private static EnumSet<EntityType> SUPPORTED_ENTITY_TYPES;
    /** 可变 fixture 字段：{@code RANDOM_UUID} 保存 {@code UUID} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private static UUID RANDOM_UUID;
    /** 可变 fixture 字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private static TenantId TENANT_ID;
    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code tenantServiceMock} 保存 {@code TenantService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TenantService tenantServiceMock;
    /** Mock 依赖字段：{@code customerServiceMock} 保存 {@code CustomerService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private CustomerService customerServiceMock;
    /** Mock 依赖字段：{@code userServiceMock} 保存 {@code UserService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private UserService userServiceMock;
    /** Mock 依赖字段：{@code assetServiceMock} 保存 {@code AssetService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AssetService assetServiceMock;
    /** Mock 依赖字段：{@code deviceServiceMock} 保存 {@code DeviceService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DeviceService deviceServiceMock;
    /** Mock 依赖字段：{@code ruleEngineAlarmServiceMock} 保存 {@code RuleEngineAlarmService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineAlarmService ruleEngineAlarmServiceMock;
    /** Mock 依赖字段：{@code ruleChainServiceMock} 保存 {@code RuleChainService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleChainService ruleChainServiceMock;
    /** Mock 依赖字段：{@code entityViewServiceMock} 保存 {@code EntityViewService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EntityViewService entityViewServiceMock;
    /** Mock 依赖字段：{@code edgeServiceMock} 保存 {@code EdgeService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EdgeService edgeServiceMock;

    /**
     * 生命周期方法：{@code setup} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeAll
    public static void setup() {
        RANDOM_UUID = UUID.randomUUID();
        TENANT_ID = new TenantId(UUID.randomUUID());
        SUPPORTED_ENTITY_TYPES = EnumSet.of(
                EntityType.TENANT,
                EntityType.CUSTOMER,
                EntityType.USER,
                EntityType.ASSET,
                EntityType.DEVICE,
                EntityType.ALARM,
                EntityType.RULE_CHAIN,
                EntityType.ENTITY_VIEW,
                EntityType.EDGE
        );
    }

    /**
     * 测试方法：覆盖 {@code givenSupportedEntityTypes_whenFindAsync_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenSupportedEntityTypes_whenFindAsync_thenOK() throws ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        for (var entityType : SUPPORTED_ENTITY_TYPES) {
            var entityId = EntityIdFactory.getByTypeAndUuid(entityType, RANDOM_UUID);

            initMocks(entityType, false);

            when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

            var actualEntityFieldsData = EntitiesFieldsAsyncLoader.findAsync(ctxMock, entityId).get();
            var expectedEntityFieldsData = new EntityFieldsData(getEntityFromEntityId(entityId));

            Assertions.assertEquals(expectedEntityFieldsData, actualEntityFieldsData);
        }
    }

    /**
     * 测试方法：覆盖 {@code givenUnsupportedEntityTypes_whenFindAsync_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenUnsupportedEntityTypes_whenFindAsync_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        for (var entityType : EntityType.values()) {
            if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
                var entityId = EntityIdFactory.getByTypeAndUuid(entityType, RANDOM_UUID);

                var expectedExceptionMsg = "org.thingsboard.rule.engine.api.TbNodeException: Unexpected originator EntityType: " + entityType;

                var exception = assertThrows(ExecutionException.class,
                        () -> EntitiesFieldsAsyncLoader.findAsync(ctxMock, entityId).get());

                assertInstanceOf(TbNodeException.class, exception.getCause());
                assertThat(exception.getMessage()).isEqualTo(expectedExceptionMsg);
            }
        }
    }

    /**
     * 测试方法：覆盖 {@code givenSupportedTypeButEntityDoesNotExist_whenFindAsync_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void givenSupportedTypeButEntityDoesNotExist_whenFindAsync_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        for (var entityType : SUPPORTED_ENTITY_TYPES) {
            var entityId = EntityIdFactory.getByTypeAndUuid(entityType, RANDOM_UUID);

            initMocks(entityType, true);
            when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

            var expectedExceptionMsg = "java.util.NoSuchElementException: Entity not found!";

            var exception = assertThrows(ExecutionException.class,
                    () -> EntitiesFieldsAsyncLoader.findAsync(ctxMock, entityId).get());

            assertInstanceOf(NoSuchElementException.class, exception.getCause());
            assertThat(exception.getMessage()).isEqualTo(expectedExceptionMsg);
        }
    }

    /**
     * 辅助方法：{@code initMocks} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void initMocks(EntityType entityType, boolean entityDoesNotExist) {
        switch (entityType) {
            case TENANT:
                var tenant = Futures.immediateFuture(entityDoesNotExist ? null : new Tenant(new TenantId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getTenantService()).thenReturn(tenantServiceMock);
                doReturn(tenant).when(tenantServiceMock).findTenantByIdAsync(eq(TENANT_ID), any());

                break;
            case CUSTOMER:
                var customer = Futures.immediateFuture(entityDoesNotExist ? null : new Customer(new CustomerId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
                doReturn(customer).when(customerServiceMock).findCustomerByIdAsync(eq(TENANT_ID), any());

                break;
            case USER:
                var user = Futures.immediateFuture(entityDoesNotExist ? null : new User(new UserId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getUserService()).thenReturn(userServiceMock);
                doReturn(user).when(userServiceMock).findUserByIdAsync(eq(TENANT_ID), any());

                break;
            case ASSET:
                var asset = Futures.immediateFuture(entityDoesNotExist ? null : new Asset(new AssetId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                doReturn(asset).when(assetServiceMock).findAssetByIdAsync(eq(TENANT_ID), any());

                break;
            case DEVICE:
                var device = entityDoesNotExist ? null : new Device(new DeviceId(RANDOM_UUID));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                doReturn(device).when(deviceServiceMock).findDeviceById(eq(TENANT_ID), any());

                break;
            case ALARM:
                var alarm = Futures.immediateFuture(entityDoesNotExist ? null : new Alarm(new AlarmId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getAlarmService()).thenReturn(ruleEngineAlarmServiceMock);
                doReturn(alarm).when(ruleEngineAlarmServiceMock).findAlarmByIdAsync(eq(TENANT_ID), any());

                break;
            case RULE_CHAIN:
                var ruleChain = Futures.immediateFuture(entityDoesNotExist ? null : new RuleChain(new RuleChainId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getRuleChainService()).thenReturn(ruleChainServiceMock);
                doReturn(ruleChain).when(ruleChainServiceMock).findRuleChainByIdAsync(eq(TENANT_ID), any());

                break;
            case ENTITY_VIEW:
                var entityView = Futures.immediateFuture(entityDoesNotExist ? null : new EntityView(new EntityViewId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
                doReturn(entityView).when(entityViewServiceMock).findEntityViewByIdAsync(eq(TENANT_ID), any());

                break;
            case EDGE:
                var edge = Futures.immediateFuture(entityDoesNotExist ? null : new Edge(new EdgeId(RANDOM_UUID)));

                when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
                when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
                doReturn(edge).when(edgeServiceMock).findEdgeByIdAsync(eq(TENANT_ID), any());

                break;
            default:
                throw new RuntimeException("Unexpected EntityType: " + entityType);
        }
    }

    /**
     * 辅助方法：{@code getEntityFromEntityId} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private BaseData<? extends UUIDBased> getEntityFromEntityId(EntityId entityId) {
        switch (entityId.getEntityType()) {
            case TENANT:
                return new Tenant((TenantId) entityId);
            case CUSTOMER:
                return new Customer((CustomerId) entityId);
            case USER:
                return new User((UserId) entityId);
            case ASSET:
                return new Asset((AssetId) entityId);
            case DEVICE:
                return new Device((DeviceId) entityId);
            case ALARM:
                return new Alarm((AlarmId) entityId);
            case RULE_CHAIN:
                return new RuleChain((RuleChainId) entityId);
            case ENTITY_VIEW:
                return new EntityView((EntityViewId) entityId);
            case EDGE:
                return new Edge((EdgeId) entityId);
            default:
                throw new RuntimeException("Unexpected EntityType: " + entityId.getEntityType());
        }
    }

}
/*
 * 本类总结：{@code EntitiesFieldsAsyncLoaderTest} 为 {@code EntitiesFieldsAsyncLoader} 的 规则引擎工具组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
