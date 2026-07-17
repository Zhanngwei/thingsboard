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
 * `EntitiesFieldsAsyncLoaderTest` 测试类，用于验证 `EntitiesFieldsAsyncLoader` 相关行为。
 */
@ExtendWith(MockitoExtension.class)
public class EntitiesFieldsAsyncLoaderTest {

    /**
     * 执行器常量，用于统一引用固定值。
     */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static EnumSet<EntityType> SUPPORTED_ENTITY_TYPES;
    /**
     * `RANDOM_UUID`常量，用于统一引用固定值。
     */
    private static UUID RANDOM_UUID;
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static TenantId TENANT_ID;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctxMock;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Mock
    private TenantService tenantServiceMock;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Mock
    private CustomerService customerServiceMock;
    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Mock
    private UserService userServiceMock;
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Mock
    private AssetService assetServiceMock;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Mock
    private DeviceService deviceServiceMock;
    /**
     * 规则引擎，提供当前类调用的业务操作。
     */
    @Mock
    private RuleEngineAlarmService ruleEngineAlarmServiceMock;
    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Mock
    private RuleChainService ruleChainServiceMock;
    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Mock
    private EntityViewService entityViewServiceMock;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Mock
    private EdgeService edgeServiceMock;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
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
     * 功能：验证 `givenSupportedEntityTypes_whenFindAsync_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenSupportedEntityTypes_whenFindAsync_thenOK() throws ExecutionException, InterruptedException {
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
     * 功能：验证 `givenUnsupportedEntityTypes_whenFindAsync_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenUnsupportedEntityTypes_whenFindAsync_thenException() {
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
     * 功能：验证 `givenSupportedTypeButEntityDoesNotExist_whenFindAsync_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenSupportedTypeButEntityDoesNotExist_whenFindAsync_thenException() {
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
     * 功能：初始化或启动`Mocks`。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityDoesNotExist`：实体对象。
     * 返回：无。
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
     * 功能：获取实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
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
