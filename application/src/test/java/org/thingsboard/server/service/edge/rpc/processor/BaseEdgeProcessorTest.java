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
package org.thingsboard.server.service.edge.rpc.processor;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.edge.EdgeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.queue.QueueMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.queue.QueueMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.queue.QueueMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.telemetry.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.user.UserMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.user.UserMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.user.UserMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessorV2;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessorV1;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessorV2;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. `BaseEdgeProcessorTest` 是 ThingsBoard Application 中验证 `BaseEdgeProcessor` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public abstract class BaseEdgeProcessorTest {

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @MockBean
    protected TelemetrySubscriptionService tsSubService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbNotificationEntityService notificationEntityService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @MockBean
    protected RuleChainService ruleChainService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @MockBean
    protected AlarmService alarmService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @MockBean
    protected AlarmCommentService alarmCommentService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceService deviceService;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected TbAssetProfileCache assetProfileCache;

    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @MockBean
    protected DashboardService dashboardService;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected AssetService assetService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @MockBean
    protected EntityViewService entityViewService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @MockBean
    protected TenantService tenantService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @MockBean
    protected TenantProfileService tenantProfileService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @MockBean
    protected EdgeService edgeService;

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @MockBean
    protected CustomerService customerService;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @MockBean
    protected UserService userService;

    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceProfileService deviceProfileService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected AssetProfileService assetProfileService;

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @MockBean
    protected RelationService relationService;

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceCredentialsService deviceCredentialsService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected AttributesService attributesService;

    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    @MockBean
    protected TimeseriesService timeseriesService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbClusterService tbClusterService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceStateService deviceStateService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @MockBean
    protected EdgeEventService edgeEventService;

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @MockBean
    protected WidgetsBundleService widgetsBundleService;

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @MockBean
    protected WidgetTypeService widgetTypeService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected OtaPackageService otaPackageService;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @MockBean
    protected QueueService queueService;

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @MockBean
    protected PartitionService partitionService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected ResourceService resourceService;

    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @MockBean
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    /**
     * 设备对象，用于描述当前业务场景。
     */
    @MockBean
    protected DataValidator<Device> deviceValidator;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @MockBean
    protected DataValidator<DeviceProfile> deviceProfileValidator;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected DataValidator<Asset> assetValidator;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected DataValidator<AssetProfile> assetProfileValidator;

    /**
     * 仪表盘对象，用于描述当前业务场景。
     */
    @MockBean
    protected DataValidator<Dashboard> dashboardValidator;

    /**
     * 实体视图对象，用于描述当前业务场景。
     */
    @MockBean
    protected DataValidator<EntityView> entityViewValidator;

    /**
     * 校验器，封装可复用的处理规则。
     */
    @MockBean
    protected DataValidator<TbResource> resourceValidator;

    /**
     * 边缘节点，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected EdgeMsgConstructor edgeMsgConstructor;

    /**
     * 实体，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected AdminSettingsMsgConstructorV1 adminSettingsMsgConstructorV1;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected AdminSettingsMsgConstructorV2 adminSettingsMsgConstructorV2;

    /**
     * 告警，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected AlarmMsgConstructorV1 alarmMsgConstructorV1;

    /**
     * 告警，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected AlarmMsgConstructorV2 alarmMsgConstructorV2;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetMsgConstructorV1 assetMsgConstructorV1;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetMsgConstructorV2 assetMsgConstructorV2;

    /**
     * 客户，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected CustomerMsgConstructorV1 customerMsgConstructorV1;

    /**
     * 客户，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected CustomerMsgConstructorV2 customerMsgConstructorV2;

    /**
     * 仪表盘，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected DashboardMsgConstructorV1 dashboardMsgConstructorV1;

    /**
     * 仪表盘，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected DashboardMsgConstructorV2 dashboardMsgConstructorV2;

    /**
     * 设备，承载当前步骤需要处理的内容。
     */
    @SpyBean
    protected DeviceMsgConstructorV1 deviceMsgConstructorV1;

    /**
     * 设备，承载当前步骤需要处理的内容。
     */
    @SpyBean
    protected DeviceMsgConstructorV2 deviceMsgConstructorV2;

    /**
     * 实体视图，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected EntityViewMsgConstructorV1 entityViewMsgConstructorV1;

    /**
     * 实体视图，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected EntityViewMsgConstructorV2 entityViewMsgConstructorV2;

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected OtaPackageMsgConstructorV1 otaPackageMsgConstructorV1;

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected OtaPackageMsgConstructorV2 otaPackageMsgConstructorV2;

    /**
     * 队列，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected QueueMsgConstructorV1 queueMsgConstructorV1;

    /**
     * 队列，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected QueueMsgConstructorV2 queueMsgConstructorV2;

    /**
     * 关系，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected RelationMsgConstructorV1 relationMsgConstructorV1;

    /**
     * 关系，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected RelationMsgConstructorV2 relationMsgConstructorV2;

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected ResourceMsgConstructorV1 resourceMsgConstructorV1;

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected ResourceMsgConstructorV2 resourceMsgConstructorV2;

    /**
     * 规则链，承载当前步骤需要处理的内容。
     */
    @SpyBean
    protected RuleChainMsgConstructorV1 ruleChainMsgConstructorV1;

    /**
     * 规则链，承载当前步骤需要处理的内容。
     */
    @SpyBean
    protected RuleChainMsgConstructorV2 ruleChainMsgConstructorV2;

    /**
     * 租户，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected TenantMsgConstructorV1 tenantMsgConstructorV1;

    /**
     * 租户，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected TenantMsgConstructorV2 tenantMsgConstructorV2;

    /**
     * 用户，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected UserMsgConstructorV1 userMsgConstructorV1;

    /**
     * 用户，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected UserMsgConstructorV2 userMsgConstructorV2;

    /**
     * 部件，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected WidgetMsgConstructorV1 widgetMsgConstructorV1;

    /**
     * 部件，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected WidgetMsgConstructorV2 widgetMsgConstructorV2;

    /**
     * 告警，负责处理对应任务或消息。
     */
    @MockBean
    protected AlarmEdgeProcessorV1 alarmProcessorV1;

    /**
     * 告警，负责处理对应任务或消息。
     */
    @MockBean
    protected AlarmEdgeProcessorV2 alarmProcessorV2;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetEdgeProcessorV1 assetProcessorV1;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetEdgeProcessorV2 assetProcessorV2;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetProfileEdgeProcessorV1 assetProfileProcessorV1;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetProfileEdgeProcessorV2 assetProfileProcessorV2;

    /**
     * 仪表盘，负责处理对应任务或消息。
     */
    @MockBean
    protected DashboardEdgeProcessorV1 dashboardProcessorV1;

    /**
     * 仪表盘，负责处理对应任务或消息。
     */
    @MockBean
    protected DashboardEdgeProcessorV2 dashboardProcessorV2;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected ImageService imageService;

    /**
     * 设备，负责处理对应任务或消息。
     */
    @SpyBean
    protected DeviceEdgeProcessorV1 deviceEdgeProcessorV1;

    /**
     * 设备，负责处理对应任务或消息。
     */
    @SpyBean
    protected DeviceEdgeProcessorV2 deviceEdgeProcessorV2;

    /**
     * 设备配置，负责处理对应任务或消息。
     */
    @SpyBean
    protected DeviceProfileEdgeProcessorV1 deviceProfileProcessorV1;

    /**
     * 设备配置，负责处理对应任务或消息。
     */
    @SpyBean
    protected DeviceProfileEdgeProcessorV2 deviceProfileProcessorV2;

    /**
     * 实体视图，负责处理对应任务或消息。
     */
    @MockBean
    protected EntityViewProcessorV1 entityViewProcessorV1;

    /**
     * 实体视图，负责处理对应任务或消息。
     */
    @MockBean
    protected EntityViewProcessorV2 entityViewProcessorV2;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @MockBean
    protected ResourceEdgeProcessorV1 resourceEdgeProcessorV1;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @MockBean
    protected ResourceEdgeProcessorV2 resourceEdgeProcessorV2;

    /**
     * 关系，负责处理对应任务或消息。
     */
    @MockBean
    protected RelationEdgeProcessorV1 relationEdgeProcessorV1;

    /**
     * 关系，负责处理对应任务或消息。
     */
    @MockBean
    protected RelationEdgeProcessorV2 relationEdgeProcessorV2;

    /**
     * 规则链，用于按场景创建或提供目标对象。
     */
    @SpyBean
    protected RuleChainMsgConstructorFactory ruleChainMsgConstructorFactory;

    /**
     * 告警，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected AlarmMsgConstructorFactory alarmMsgConstructorFactory;

    /**
     * 设备，用于按场景创建或提供目标对象。
     */
    @SpyBean
    protected DeviceMsgConstructorFactory deviceMsgConstructorFactory;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetMsgConstructorFactory assetMsgConstructorFactory;

    /**
     * 仪表盘，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected DashboardMsgConstructorFactory dashboardMsgConstructorFactory;

    /**
     * 实体视图，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected EntityViewMsgConstructorFactory entityViewMsgConstructorFactory;

    /**
     * 关系，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected RelationMsgConstructorFactory relationMsgConstructorFactory;

    /**
     * 用户，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected UserMsgConstructorFactory userMsgConstructorFactory;

    /**
     * 客户，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected CustomerMsgConstructorFactory customerMsgConstructorFactory;

    /**
     * 租户，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected TenantMsgConstructorFactory tenantMsgConstructorFactory;

    /**
     * 部件，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected WidgetMsgConstructorFactory widgetBundleMsgConstructorFactory;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected AdminSettingsMsgConstructorFactory adminSettingsMsgConstructorFactory;

    /**
     * 消息，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected OtaPackageMsgConstructorFactory otaPackageMsgConstructorFactory;

    /**
     * 队列，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected QueueMsgConstructorFactory queueMsgConstructorFactory;

    /**
     * 消息，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected ResourceMsgConstructorFactory resourceMsgConstructorFactory;

    /**
     * 告警，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected AlarmEdgeProcessorFactory alarmEdgeProcessorFactory;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @SpyBean
    protected AssetEdgeProcessorFactory assetEdgeProcessorFactory;

    /**
     * 仪表盘，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected DashboardEdgeProcessorFactory dashboardEdgeProcessorFactory;

    /**
     * 设备，用于按场景创建或提供目标对象。
     */
    @SpyBean
    protected DeviceEdgeProcessorFactory deviceEdgeProcessorFactory;

    /**
     * 实体视图，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected EntityViewProcessorFactory entityViewProcessorFactory;

    /**
     * 关系，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected RelationEdgeProcessorFactory relationEdgeProcessorFactory;

    /**
     * 边缘节点，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected ResourceEdgeProcessorFactory resourceEdgeProcessorFactory;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @MockBean
    protected EdgeSynchronizationManager edgeSynchronizationManager;

    /**
     * 回调，提供当前类调用的业务操作。
     */
    @MockBean
    protected DbCallbackExecutorService dbCallbackExecutorService;
    
    /**
     * 数据，提供当前类调用的业务操作。
     */
    @MockBean
    protected DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 边缘节点ID，用于定位对应业务对象。
     */
    protected EdgeId edgeId;
    protected TenantId tenantId;
    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    protected EdgeEvent edgeEvent;

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `expectedDashboardIdMSB`：`expectedDashboardIdMSB` 参数。
     * - `expectedDashboardIdLSB`：`expectedDashboardIdLSB` 参数。
     * 返回：处理结果。
     */
    protected DashboardId getDashboardId(long expectedDashboardIdMSB, long expectedDashboardIdLSB) {
        DashboardId dashboardId;
        if (expectedDashboardIdMSB != 0 && expectedDashboardIdLSB != 0) {
            dashboardId = new DashboardId(new UUID(expectedDashboardIdMSB, expectedDashboardIdLSB));
        } else {
            dashboardId = new DashboardId(UUID.randomUUID());
        }
        return dashboardId;
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `expectedRuleChainIdMSB`：`expectedRuleChainIdMSB` 参数。
     * - `expectedRuleChainIdLSB`：`expectedRuleChainIdLSB` 参数。
     * 返回：处理结果。
     */
    protected RuleChainId getRuleChainId(long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        RuleChainId ruleChainId;
        if (expectedRuleChainIdMSB != 0 && expectedRuleChainIdLSB != 0) {
            ruleChainId = new RuleChainId(new UUID(expectedRuleChainIdMSB, expectedRuleChainIdLSB));
        } else {
            ruleChainId = new RuleChainId(UUID.randomUUID());
        }
        return ruleChainId;
    }

    /**
     * 功能：执行 `provideParameters` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected static Stream<Arguments> provideParameters() {
        UUID dashoboardUUID = UUID.randomUUID();
        UUID ruleChaindUUID = UUID.randomUUID();
        return Stream.of(
                Arguments.of(EdgeVersion.V_3_3_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_3_3, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_4_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_6_0,
                        dashoboardUUID.getMostSignificantBits(),
                        dashoboardUUID.getLeastSignificantBits(),
                        ruleChaindUUID.getMostSignificantBits(),
                        ruleChaindUUID.getLeastSignificantBits())
        );
    }
}
