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
package org.thingsboard.server.actors;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.rule.RuleNodeStateService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.ExternalCallExecutorService;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.executors.PubSubRuleNodeExecutorProvider;
import org.thingsboard.server.service.executors.SharedEventLoopGroupService;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.TbRpcService;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.session.DeviceSessionCacheService;
import org.thingsboard.server.service.sms.SmsExecutorService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`ActorSystemContext` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@Slf4j
@Component
public class ActorSystemContext {

    private static final FutureCallback<Void> RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Rule Chain", th);
        }
    };
    private static final FutureCallback<Void> RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Node", th);
        }
    };

    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private TbApiUsageStateService apiUsageStateService;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Autowired
    @Getter
    private TbApiUsageReportClient apiUsageClient;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    @Setter
    private TbServiceInfoProvider serviceInfoProvider;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Getter
    @Setter
    private ActorService actorService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    @Setter
    private ComponentDiscoveryService componentService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private DiscoveryService discoveryService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private DataDecodingEncodingService encodingService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private DeviceService deviceService;

    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private DeviceProfileService deviceProfileService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    @Getter
    private AssetProfileService assetProfileService;

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private DeviceCredentialsService deviceCredentialsService;

    /**
     * 设备状态管理器，负责处理对应任务或消息。
     */
    @Autowired(required = false)
    @Getter
    private RuleEngineDeviceStateManager deviceStateManager;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    @Getter
    private TbTenantProfileCache tenantProfileCache;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @Autowired
    @Getter
    private TbDeviceProfileCache deviceProfileCache;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    @Getter
    private TbAssetProfileCache assetProfileCache;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    @Getter
    private AssetService assetService;

    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private DashboardService dashboardService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private TenantService tenantService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private TenantProfileService tenantProfileService;

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private CustomerService customerService;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private UserService userService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private RuleChainService ruleChainService;

    /**
     * 规则节点，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private RuleNodeStateService ruleNodeStateService;

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private PartitionService partitionService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private TbClusterService clusterService;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private TimeseriesService tsService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private AttributesService attributesService;

    /**
     * 事件，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private EventService eventService;

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private RelationService relationService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private AuditLogService auditLogService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private EntityViewService entityViewService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbEntityViewService tbEntityViewService;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private TelemetrySubscriptionService tsSubService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private AlarmSubscriptionService alarmService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private AlarmCommentService alarmCommentService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private JsInvokeService jsInvokeService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    @Getter
    private TbelInvokeService tbelInvokeService;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    @Getter
    private MailExecutorService mailExecutor;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    @Getter
    private SmsExecutorService smsExecutor;

    /**
     * 回调，负责处理对应任务或消息。
     */
    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private ExternalCallExecutorService externalCallExecutorService;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    @Getter
    private NotificationExecutorService notificationExecutor;

    /**
     * 规则节点，用于按场景创建或提供目标对象。
     */
    @Lazy
    @Autowired
    @Getter
    private PubSubRuleNodeExecutorProvider pubSubRuleNodeExecutorProvider;

    /**
     * 事件循环，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private MailService mailService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private SmsService smsService;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Autowired
    @Getter
    private SmsSenderFactory smsSenderFactory;

    /**
     * 通知，表示当前对象的对应属性。
     */
    @Autowired
    @Getter
    private NotificationCenter notificationCenter;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    @Getter
    private NotificationRuleProcessor notificationRuleProcessor;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private NotificationTargetService notificationTargetService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private NotificationTemplateService notificationTemplateService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private NotificationRequestService notificationRequestService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private NotificationRuleService notificationRuleService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    private SlackService slackService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private ClaimDevicesService claimDevicesService;

    /**
     * `jsInvokeStats` 字段，保存当前对象的对应属性。
     */
    @Autowired
    @Getter
    private JsInvokeStats jsInvokeStats;

    //TODO: separate context for TbCore and TbRuleEngine
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    @Getter
    private DeviceStateService deviceStateService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    @Getter
    private DeviceSessionCacheService deviceSessionCacheService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    @Getter
    private TbCoreToTransportService tbCoreToTransportService;

    /**
     * The following Service will be null if we operate in tb-core mode
     */
    /**
     * 规则引擎，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRuleEngineDeviceRpcService tbRuleEngineDeviceRpcService;

    /**
     * The following Service will be null if we operate in tb-rule-engine mode
     */
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeService edgeService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeEventService edgeEventService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeRpcService edgeRpcService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private ResourceService resourceService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private OtaPackageService otaPackageService;

    /**
     * RPC，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRpcService tbRpcService;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private QueueService queueService;

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetsBundleService widgetsBundleService;

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetTypeService widgetTypeService;

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private EntityService entityService;

    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Value("${actors.session.max_concurrent_sessions_per_device:1}")
    @Getter
    private long maxConcurrentSessionsPerDevice;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${actors.session.sync.timeout:10000}")
    @Getter
    private long syncSessionTimeout;

    /**
     * 规则链，记录当前处理过程中的失败原因。
     */
    @Value("${actors.rule.chain.error_persist_frequency:3000}")
    @Getter
    private long ruleChainErrorPersistFrequency;

    /**
     * 规则节点，记录当前处理过程中的失败原因。
     */
    @Value("${actors.rule.node.error_persist_frequency:3000}")
    @Getter
    private long ruleNodeErrorPersistFrequency;

    /**
     * 是否启用`statistics`。
     */
    @Value("${actors.statistics.enabled:true}")
    @Getter
    private boolean statisticsEnabled;

    /**
     * `statisticsPersistFrequency` 字段，保存当前对象的对应属性。
     */
    @Value("${actors.statistics.persist_frequency:3600000}")
    @Getter
    private long statisticsPersistFrequency;

    /**
     * 是否启用`edges`。
     */
    @Value("${edges.enabled:true}")
    @Getter
    private boolean edgesEnabled;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Value("${cache.type:caffeine}")
    @Getter
    private String cacheType;

    /**
     * 是否满足类型条件。
     */
    @Getter
    private boolean localCacheType;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        this.localCacheType = "caffeine".equals(cacheType);
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${actors.statistics.js_print_interval_ms}")
    public void printStats() {
        if (statisticsEnabled) {
            if (jsInvokeStats.getRequests() > 0 || jsInvokeStats.getResponses() > 0 || jsInvokeStats.getFailures() > 0) {
                log.info("Rule Engine JS Invoke Stats: requests [{}] responses [{}] failures [{}]",
                        jsInvokeStats.getRequests(), jsInvokeStats.getResponses(), jsInvokeStats.getFailures());
                jsInvokeStats.reset();
            }
        }
    }

    /**
     * 是否启用租户。
     */
    @Value("${actors.tenant.create_components_on_init:true}")
    @Getter
    private boolean tenantComponentsInitEnabled;

    /**
     * 当前操作是否被允许。
     */
    @Value("${actors.rule.allow_system_mail_service:true}")
    @Getter
    private boolean allowSystemMailService;

    /**
     * 当前操作是否被允许。
     */
    @Value("${actors.rule.allow_system_sms_service:true}")
    @Getter
    private boolean allowSystemSmsService;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${transport.sessions.inactivity_timeout:300000}")
    @Getter
    private long sessionInactivityTimeout;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${transport.sessions.report_timeout:3000}")
    @Getter
    private long sessionReportTimeout;

    /**
     * 是否启用租户。
     */
    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled:true}")
    @Getter
    private boolean debugPerTenantEnabled;

    /**
     * 租户，保存当前对象的配置选项。
     */
    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.configuration:50000:3600}")
    @Getter
    private String debugPerTenantLimitsConfiguration;

    /**
     * RPC，表示当前对象的对应属性。
     */
    @Value("${actors.rpc.submit_strategy:BURST}")
    @Getter
    private String rpcSubmitStrategy;

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Value("${actors.rpc.response_timeout_ms:30000}")
    @Getter
    private long rpcResponseTimeout;

    /**
     * RPC，表示当前对象的对应属性。
     */
    @Value("${actors.rpc.max_retries:5}")
    @Getter
    private int maxRpcRetries;

    /**
     * 是否满足节点实例条件。
     */
    @Value("${actors.rule.external.force_ack:false}")
    @Getter
    private boolean externalNodeForceAck;

    /**
     * 设备，保存当前对象的配置选项。
     */
    @Value("${state.rule.node.deviceState.rateLimit:1:1,30:60,60:3600}")
    @Getter
    private String deviceStateNodeRateLimitConfig;

    /**
     * Actor 系统，表示当前对象的对应属性。
     */
    @Getter
    @Setter
    private TbActorSystem actorSystem;

    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    @Setter
    private TbActorRef appActor;

    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    @Getter
    @Setter
    private TbActorRef statsActor;

    /**
     * Cassandra 集群，用于支撑当前网络或外部服务交互。
     */
    @Autowired(required = false)
    @Getter
    private CassandraCluster cassandraCluster;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    /**
     * `redisTemplate` 字段，保存当前对象的对应属性。
     */
    @Autowired(required = false)
    @Getter
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 功能：获取调度器。
     * 参数：无。
     * 返回：处理结果。
     */
    public ScheduledExecutorService getScheduler() {
        return actorSystem.getScheduler();
    }

    /**
     * 功能：执行 `persistError` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `method`：`method` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    public void persistError(TenantId tenantId, EntityId entityId, String method, Exception e) {
        eventService.saveAsync(ErrorEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .method(method)
                .error(toString(e)).build());
    }

    /**
     * 功能：执行 `persistLifecycleEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `lcEvent`：`lcEvent` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        LifecycleEvent.LifecycleEventBuilder event = LifecycleEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .lcEventType(lcEvent.name());

        if (e != null) {
            event.success(false).error(toString(e));
        } else {
            event.success(true);
        }

        eventService.saveAsync(event.build());
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：文本结果。
     */
    private String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `serviceType`：服务对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, tenantId, entityId);
    }

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `serviceType`：服务对象。
     * - `queueName`：队列名称或队列对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, queueName, tenantId, entityId);
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getServiceId() {
        return serviceInfoProvider.getServiceId();
    }

    /**
     * 功能：执行 `persistDebugInput` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tbMsg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, null, null);
    }

    /**
     * 功能：执行 `persistDebugInput` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tbMsg`：待处理消息。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, error, null);
    }

    /**
     * 功能：执行 `persistDebugOutput` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tbMsg`：待处理消息。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error, String failureMessage) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, failureMessage);
    }

    /**
     * 功能：执行 `persistDebugOutput` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tbMsg`：待处理消息。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, null);
    }

    /**
     * 功能：执行 `persistDebugOutput` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tbMsg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, null, null);
    }

    /**
     * 功能：执行 `persistDebugAsync` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `type`：类型。
     * - `tbMsg`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void persistDebugAsync(TenantId tenantId, EntityId entityId, String type, TbMsg tbMsg, String relationType, Throwable error, String failureMessage) {
        if (checkLimits(tenantId, tbMsg, error)) {
            try {
                RuleNodeDebugEvent.RuleNodeDebugEventBuilder event = RuleNodeDebugEvent.builder()
                        .tenantId(tenantId)
                        .entityId(entityId.getId())
                        .serviceId(getServiceId())
                        .eventType(type)
                        .eventEntity(tbMsg.getOriginator())
                        .msgId(tbMsg.getId())
                        .msgType(tbMsg.getType())
                        .dataType(tbMsg.getDataType().name())
                        .relationType(relationType)
                        .data(tbMsg.getData())
                        .metadata(JacksonUtil.toString(tbMsg.getMetaData().getData()));

                if (error != null) {
                    event.error(toString(error));
                } else if (failureMessage != null) {
                    event.error(failureMessage);
                }

                ListenableFuture<Void> future = eventService.saveAsync(event.build());
                Futures.addCallback(future, RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
            } catch (IllegalArgumentException ex) {
                log.warn("Failed to persist rule node debug message", ex);
            }
        }
    }

    /**
     * 功能：校验`Limits`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tbMsg`：待处理消息。
     * - `error`：错误信息。
     * 返回：判断结果。
     */
    private boolean checkLimits(TenantId tenantId, TbMsg tbMsg, Throwable error) {
        if (debugPerTenantEnabled) {
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugPerTenantLimitsConfiguration), false));

            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 功能：执行 `persistRuleChainDebugModeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `error`：错误信息。
     * 返回：无。
     */
    private void persistRuleChainDebugModeEvent(TenantId tenantId, EntityId entityId, Throwable error) {
        RuleChainDebugEvent.RuleChainDebugEventBuilder event = RuleChainDebugEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .message("Reached debug mode rate limit!");
        if (error != null) {
            event.error(toString(error));
        }

        ListenableFuture<Void> future = eventService.saveAsync(event.build());
        Futures.addCallback(future, RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `toException` 对应的处理。
     * 参数：
     * - `error`：错误信息。
     * 返回：处理结果。
     */
    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }

    /**
     * 功能：执行 `tell` 对应的处理。
     * 参数：
     * - `tbActorMsg`：待处理消息。
     * 返回：无。
     */
    public void tell(TbActorMsg tbActorMsg) {
        appActor.tell(tbActorMsg);
    }

    /**
     * 功能：执行 `tellWithHighPriority` 对应的处理。
     * 参数：
     * - `tbActorMsg`：待处理消息。
     * 返回：无。
     */
    public void tellWithHighPriority(TbActorMsg tbActorMsg) {
        appActor.tellWithHighPriority(tbActorMsg);
    }

    /**
     * 功能：执行 `schedulePeriodicMsgWithDelay` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `delayInMs`：`delayInMs` 参数。
     * - `periodInMs`：`periodInMs` 参数。
     * 返回：无。
     */
    public void schedulePeriodicMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs, long periodInMs) {
        log.debug("Scheduling periodic msg {} every {} ms with delay {} ms", msg, periodInMs, delayInMs);
        getScheduler().scheduleWithFixedDelay(() -> ctx.tell(msg), delayInMs, periodInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 功能：执行 `scheduleMsgWithDelay` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `delayInMs`：`delayInMs` 参数。
     * 返回：无。
     */
    public void scheduleMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs) {
        log.debug("Scheduling msg {} with delay {} ms", msg, delayInMs);
        if (delayInMs > 0) {
            getScheduler().schedule(() -> ctx.tell(msg), delayInMs, TimeUnit.MILLISECONDS);
        } else {
            ctx.tell(msg);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ActorSystemContext` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
