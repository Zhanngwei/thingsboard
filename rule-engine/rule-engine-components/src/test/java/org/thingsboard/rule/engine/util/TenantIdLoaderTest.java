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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineApiUsageStateService;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TenantIdLoaderTest} 覆盖的 规则引擎工具组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TenantIdLoader}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@RunWith(MockitoJUnitRunner.class)
public class TenantIdLoaderTest {

    /** Mock 依赖字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctx;
    /** Mock 依赖字段：{@code customerService} 保存 {@code CustomerService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private CustomerService customerService;
    /** Mock 依赖字段：{@code userService} 保存 {@code UserService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private UserService userService;
    /** Mock 依赖字段：{@code assetService} 保存 {@code AssetService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AssetService assetService;
    /** Mock 依赖字段：{@code deviceService} 保存 {@code DeviceService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DeviceService deviceService;
    /** Mock 依赖字段：{@code alarmService} 保存 {@code RuleEngineAlarmService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineAlarmService alarmService;
    /** Mock 依赖字段：{@code ruleChainService} 保存 {@code RuleChainService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleChainService ruleChainService;
    /** Mock 依赖字段：{@code entityViewService} 保存 {@code EntityViewService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EntityViewService entityViewService;
    /** Mock 依赖字段：{@code dashboardService} 保存 {@code DashboardService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DashboardService dashboardService;
    /** Mock 依赖字段：{@code edgeService} 保存 {@code EdgeService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EdgeService edgeService;
    /** Mock 依赖字段：{@code otaPackageService} 保存 {@code OtaPackageService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private OtaPackageService otaPackageService;
    /** Mock 依赖字段：{@code assetProfileCache} 保存 {@code RuleEngineAssetProfileCache} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineAssetProfileCache assetProfileCache;
    /** Mock 依赖字段：{@code deviceProfileCache} 保存 {@code RuleEngineDeviceProfileCache} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineDeviceProfileCache deviceProfileCache;
    /** Mock 依赖字段：{@code widgetTypeService} 保存 {@code WidgetTypeService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private WidgetTypeService widgetTypeService;
    /** Mock 依赖字段：{@code widgetsBundleService} 保存 {@code WidgetsBundleService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private WidgetsBundleService widgetsBundleService;
    /** Mock 依赖字段：{@code queueService} 保存 {@code QueueService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private QueueService queueService;
    /** Mock 依赖字段：{@code resourceService} 保存 {@code ResourceService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private ResourceService resourceService;
    /** Mock 依赖字段：{@code rpcService} 保存 {@code RuleEngineRpcService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineRpcService rpcService;
    /** Mock 依赖字段：{@code ruleEngineApiUsageStateService} 保存 {@code RuleEngineApiUsageStateService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineApiUsageStateService ruleEngineApiUsageStateService;
    /** Mock 依赖字段：{@code notificationTargetService} 保存 {@code NotificationTargetService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private NotificationTargetService notificationTargetService;
    /** Mock 依赖字段：{@code notificationTemplateService} 保存 {@code NotificationTemplateService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private NotificationTemplateService notificationTemplateService;
    /** Mock 依赖字段：{@code notificationRequestService} 保存 {@code NotificationRequestService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private NotificationRequestService notificationRequestService;
    /** Mock 依赖字段：{@code notificationRuleService} 保存 {@code NotificationRuleService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private NotificationRuleService notificationRuleService;

    /** 可变 fixture 字段：{@code tenantId} 保存 {@code TenantId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TenantId tenantId;
    /** 可变 fixture 字段：{@code tenantProfileId} 保存 {@code TenantProfileId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TenantProfileId tenantProfileId;
    /** 可变 fixture 字段：{@code notificationId} 保存 {@code NotificationId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private NotificationId notificationId;
    /** 可变 fixture 字段：{@code dbExecutor} 保存 {@code AbstractListeningExecutor} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private AbstractListeningExecutor dbExecutor;

    /**
     * 生命周期方法：{@code before} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Before
    public void before() {
        dbExecutor = new AbstractListeningExecutor() {
            /** 实现方法：{@code getThreadPollSize} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();
        this.tenantId = new TenantId(UUID.randomUUID());
        this.tenantProfileId = new TenantProfileId(UUID.randomUUID());
        this.notificationId = new NotificationId(UUID.randomUUID());

        when(ctx.getTenantId()).thenReturn(tenantId);

        for (EntityType entityType : EntityType.values()) {
            initMocks(entityType, tenantId);
        }
    }

    /**
     * 生命周期方法：{@code after} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @After
    public void after() {
        dbExecutor.destroy();
    }

    /**
     * 辅助方法：{@code initMocks} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void initMocks(EntityType entityType, TenantId tenantId) {
        switch (entityType) {
            case TENANT:
            case NOTIFICATION:
                break;
            case CUSTOMER:
                Customer customer = new Customer();
                customer.setTenantId(tenantId);

                when(ctx.getCustomerService()).thenReturn(customerService);
                doReturn(customer).when(customerService).findCustomerById(eq(tenantId), any());

                break;
            case USER:
                User user = new User();
                user.setTenantId(tenantId);

                when(ctx.getUserService()).thenReturn(userService);
                doReturn(user).when(userService).findUserById(eq(tenantId), any());

                break;
            case ASSET:
                Asset asset = new Asset();
                asset.setTenantId(tenantId);

                when(ctx.getAssetService()).thenReturn(assetService);
                doReturn(asset).when(assetService).findAssetById(eq(tenantId), any());

                break;
            case DEVICE:
                Device device = new Device();
                device.setTenantId(tenantId);

                when(ctx.getDeviceService()).thenReturn(deviceService);
                doReturn(device).when(deviceService).findDeviceById(eq(tenantId), any());

                break;
            case ALARM:
                Alarm alarm = new Alarm();
                alarm.setTenantId(tenantId);

                when(ctx.getAlarmService()).thenReturn(alarmService);
                doReturn(alarm).when(alarmService).findAlarmById(eq(tenantId), any());

                break;
            case RULE_CHAIN:
                RuleChain ruleChain = new RuleChain();
                ruleChain.setTenantId(tenantId);

                when(ctx.getRuleChainService()).thenReturn(ruleChainService);
                doReturn(ruleChain).when(ruleChainService).findRuleChainById(eq(tenantId), any());

                break;
            case ENTITY_VIEW:
                EntityView entityView = new EntityView();
                entityView.setTenantId(tenantId);

                when(ctx.getEntityViewService()).thenReturn(entityViewService);
                doReturn(entityView).when(entityViewService).findEntityViewById(eq(tenantId), any());

                break;
            case DASHBOARD:
                Dashboard dashboard = new Dashboard();
                dashboard.setTenantId(tenantId);

                when(ctx.getDashboardService()).thenReturn(dashboardService);
                doReturn(dashboard).when(dashboardService).findDashboardById(eq(tenantId), any());

                break;
            case EDGE:
                Edge edge = new Edge();
                edge.setTenantId(tenantId);

                when(ctx.getEdgeService()).thenReturn(edgeService);
                doReturn(edge).when(edgeService).findEdgeById(eq(tenantId), any());

                break;
            case OTA_PACKAGE:
                OtaPackage otaPackage = new OtaPackage();
                otaPackage.setTenantId(tenantId);

                when(ctx.getOtaPackageService()).thenReturn(otaPackageService);
                doReturn(otaPackage).when(otaPackageService).findOtaPackageInfoById(eq(tenantId), any());

                break;
            case ASSET_PROFILE:
                AssetProfile assetProfile = new AssetProfile();
                assetProfile.setTenantId(tenantId);

                when(ctx.getAssetProfileCache()).thenReturn(assetProfileCache);
                doReturn(assetProfile).when(assetProfileCache).get(eq(tenantId), any(AssetProfileId.class));

                break;
            case DEVICE_PROFILE:
                DeviceProfile deviceProfile = new DeviceProfile();
                deviceProfile.setTenantId(tenantId);

                when(ctx.getDeviceProfileCache()).thenReturn(deviceProfileCache);
                doReturn(deviceProfile).when(deviceProfileCache).get(eq(tenantId), any(DeviceProfileId.class));

                break;
            case WIDGET_TYPE:
                WidgetType widgetType = new WidgetType();
                widgetType.setTenantId(tenantId);

                when(ctx.getWidgetTypeService()).thenReturn(widgetTypeService);
                doReturn(widgetType).when(widgetTypeService).findWidgetTypeById(eq(tenantId), any());

                break;
            case WIDGETS_BUNDLE:
                WidgetsBundle widgetsBundle = new WidgetsBundle();
                widgetsBundle.setTenantId(tenantId);

                when(ctx.getWidgetBundleService()).thenReturn(widgetsBundleService);
                doReturn(widgetsBundle).when(widgetsBundleService).findWidgetsBundleById(eq(tenantId), any());

                break;
            case RPC:
                Rpc rpc = new Rpc();
                rpc.setTenantId(tenantId);

                when(ctx.getRpcService()).thenReturn(rpcService);
                doReturn(rpc).when(rpcService).findRpcById(eq(tenantId), any());

                break;
            case QUEUE:
                Queue queue = new Queue();
                queue.setTenantId(tenantId);

                when(ctx.getQueueService()).thenReturn(queueService);
                doReturn(queue).when(queueService).findQueueById(eq(tenantId), any());

                break;
            case API_USAGE_STATE:
                ApiUsageState apiUsageState = new ApiUsageState();
                apiUsageState.setTenantId(tenantId);

                when(ctx.getRuleEngineApiUsageStateService()).thenReturn(ruleEngineApiUsageStateService);
                doReturn(apiUsageState).when(ruleEngineApiUsageStateService).findApiUsageStateById(eq(tenantId), any());

                break;
            case TB_RESOURCE:
                TbResource tbResource = new TbResource();
                tbResource.setTenantId(tenantId);

                when(ctx.getResourceService()).thenReturn(resourceService);
                doReturn(tbResource).when(resourceService).findResourceInfoById(eq(tenantId), any());

                break;
            case RULE_NODE:
                RuleNode ruleNode = new RuleNode();

                when(ctx.getRuleChainService()).thenReturn(ruleChainService);
                doReturn(ruleNode).when(ruleChainService).findRuleNodeById(eq(tenantId), any());

                break;
            case TENANT_PROFILE:
                TenantProfile tenantProfile = new TenantProfile(tenantProfileId);

                when(ctx.getTenantProfile()).thenReturn(tenantProfile);

                break;
            case NOTIFICATION_TARGET:
                NotificationTarget notificationTarget = new NotificationTarget();
                notificationTarget.setTenantId(tenantId);
                when(ctx.getNotificationTargetService()).thenReturn(notificationTargetService);
                doReturn(notificationTarget).when(notificationTargetService).findNotificationTargetById(eq(tenantId), any());
                break;
            case NOTIFICATION_TEMPLATE:
                NotificationTemplate notificationTemplate = new NotificationTemplate();
                notificationTemplate.setTenantId(tenantId);
                when(ctx.getNotificationTemplateService()).thenReturn(notificationTemplateService);
                doReturn(notificationTemplate).when(notificationTemplateService).findNotificationTemplateById(eq(tenantId), any());
                break;
            case NOTIFICATION_REQUEST:
                NotificationRequest notificationRequest = new NotificationRequest();
                notificationRequest.setTenantId(tenantId);
                when(ctx.getNotificationRequestService()).thenReturn(notificationRequestService);
                doReturn(notificationRequest).when(notificationRequestService).findNotificationRequestById(eq(tenantId), any());
                break;
            case NOTIFICATION_RULE:
                NotificationRule notificationRule = new NotificationRule();
                notificationRule.setTenantId(tenantId);
                when(ctx.getNotificationRuleService()).thenReturn(notificationRuleService);
                doReturn(notificationRule).when(notificationRuleService).findNotificationRuleById(eq(tenantId), any());
                break;
            default:
                throw new RuntimeException("Unexpected originator EntityType " + entityType);
        }
    }

    /**
     * 辅助方法：{@code getEntityId} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private EntityId getEntityId(EntityType entityType) {
        return EntityIdFactory.getByTypeAndUuid(entityType, UUID.randomUUID());
    }

    /**
     * 辅助方法：{@code checkTenant} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void checkTenant(TenantId checkTenantId, boolean equals) {
        for (EntityType entityType : EntityType.values()) {
            EntityId entityId;
            if (EntityType.TENANT.equals(entityType)) {
                entityId = tenantId;
            } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
                entityId = tenantProfileId;
            } else {
                entityId = getEntityId(entityType);
            }
            TenantId targetTenantId = TenantIdLoader.findTenantId(ctx, entityId);
            String msg = "Check entity type <" + entityType.name() + ">:";
            if (equals) {
                Assert.assertEquals(msg, targetTenantId, checkTenantId);
            } else {
                Assert.assertNotEquals(msg, targetTenantId, checkTenantId);
            }
        }
    }

    /**
     * 测试方法：覆盖 {@code test_findEntityIdAsync_current_tenant} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void test_findEntityIdAsync_current_tenant() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        checkTenant(tenantId, true);
    }

    /**
     * 测试方法：覆盖 {@code test_findEntityIdAsync_other_tenant} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及完整规则链，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void test_findEntityIdAsync_other_tenant() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        checkTenant(new TenantId(UUID.randomUUID()), false);
    }

}
/*
 * 本类总结：{@code TenantIdLoaderTest} 为 {@code TenantIdLoader} 的 规则引擎工具组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
