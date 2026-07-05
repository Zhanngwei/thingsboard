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
package org.thingsboard.server.service.edge;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.constructor.edge.EdgeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.settings.AdminSettingsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.telemetry.TelemetryEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.GrpcCallbackExecutorService;

/**
 * 中文说明：
 * 1. 类目的：`EdgeContextComponent` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Component
@TbCoreComponent
@Data
@Lazy
public class EdgeContextComponent {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TbClusterService clusterService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    private EdgeService edgeService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    private EdgeEventService edgeEventService;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AdminSettingsService adminSettingsService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceService deviceService;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetService assetService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityViewService entityViewService;

    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceProfileService deviceProfileService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileService assetProfileService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AttributesService attributesService;

    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @Autowired
    private DashboardService dashboardService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    private RuleChainService ruleChainService;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    private UserService userService;

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    private CustomerService customerService;

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @Autowired
    private WidgetTypeService widgetTypeService;

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @Autowired
    private WidgetsBundleService widgetsBundleService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    private EdgeRequestsService edgeRequestsService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private OtaPackageService otaPackageService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantService tenantService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantProfileService tenantProfileService;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @Autowired
    private QueueService queueService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ResourceService resourceService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private RateLimitService rateLimitService;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    /**
     * 告警，负责处理对应任务或消息。
     */
    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    /**
     * 设备配置，负责处理对应任务或消息。
     */
    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileEdgeProcessor assetProfileProcessor;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @Autowired
    private EdgeProcessor edgeProcessor;

    /**
     * 设备，负责处理对应任务或消息。
     */
    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetEdgeProcessor assetProcessor;

    /**
     * 实体视图，负责处理对应任务或消息。
     */
    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    /**
     * 用户，负责处理对应任务或消息。
     */
    @Autowired
    private UserEdgeProcessor userProcessor;

    /**
     * 关系，负责处理对应任务或消息。
     */
    @Autowired
    private RelationEdgeProcessor relationProcessor;

    /**
     * 遥测，负责处理对应任务或消息。
     */
    @Autowired
    private TelemetryEdgeProcessor telemetryProcessor;

    /**
     * 仪表盘，负责处理对应任务或消息。
     */
    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    /**
     * 规则链，负责处理对应任务或消息。
     */
    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    /**
     * 客户，负责处理对应任务或消息。
     */
    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    /**
     * 部件，负责处理对应任务或消息。
     */
    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    /**
     * 部件类型，负责处理对应任务或消息。
     */
    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AdminSettingsEdgeProcessor adminSettingsProcessor;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @Autowired
    private OtaPackageEdgeProcessor otaPackageEdgeProcessor;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @Autowired
    private QueueEdgeProcessor queueEdgeProcessor;

    /**
     * 租户，负责处理对应任务或消息。
     */
    @Autowired
    private TenantEdgeProcessor tenantEdgeProcessor;

    /**
     * 租户，负责处理对应任务或消息。
     */
    @Autowired
    private TenantProfileEdgeProcessor tenantProfileEdgeProcessor;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @Autowired
    private ResourceEdgeProcessor resourceEdgeProcessor;

    /**
     * 边缘节点，承载当前步骤需要处理的内容。
     */
    @Autowired
    private EdgeMsgConstructor edgeMsgConstructor;

    /**
     * 告警，用于按场景创建或提供目标对象。
     */
    @Autowired
    private AlarmEdgeProcessorFactory alarmEdgeProcessorFactory;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetEdgeProcessorFactory assetEdgeProcessorFactory;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileEdgeProcessorFactory assetProfileEdgeProcessorFactory;

    /**
     * 仪表盘，用于按场景创建或提供目标对象。
     */
    @Autowired
    private DashboardEdgeProcessorFactory dashboardEdgeProcessorFactory;

    /**
     * 设备，用于按场景创建或提供目标对象。
     */
    @Autowired
    private DeviceEdgeProcessorFactory deviceEdgeProcessorFactory;

    /**
     * 设备配置，用于按场景创建或提供目标对象。
     */
    @Autowired
    private DeviceProfileEdgeProcessorFactory deviceProfileEdgeProcessorFactory;

    /**
     * 实体视图，用于按场景创建或提供目标对象。
     */
    @Autowired
    private EntityViewProcessorFactory entityViewProcessorFactory;

    /**
     * 关系，用于按场景创建或提供目标对象。
     */
    @Autowired
    private RelationEdgeProcessorFactory relationEdgeProcessorFactory;

    /**
     * 边缘节点，用于按场景创建或提供目标对象。
     */
    @Autowired
    private ResourceEdgeProcessorFactory resourceEdgeProcessorFactory;

    /**
     * 边缘节点集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    /**
     * 回调，负责处理对应任务或消息。
     */
    @Autowired
    private DbCallbackExecutorService dbCallbackExecutor;

    /**
     * 回调，提供当前类调用的业务操作。
     */
    @Autowired
    private GrpcCallbackExecutorService grpcCallbackExecutorService;
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeContextComponent` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
