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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
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
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.edge.EdgeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.queue.QueueMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.telemetry.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.user.UserMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessorFactory;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`BaseEdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class BaseEdgeProcessor {

    protected static final Lock deviceCreationLock = new ReentrantLock();
    protected static final Lock assetCreationLock = new ReentrantLock();

    /**
     * `DEFAULT_PAGE_SIZE`常量，用于统一引用固定值。
     */
    protected static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbNotificationEntityService notificationEntityService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    protected RuleChainService ruleChainService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    protected AlarmService alarmService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    protected AlarmCommentService alarmCommentService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceService deviceService;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @Autowired
    protected TbDeviceProfileCache deviceProfileCache;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected TbAssetProfileCache assetProfileCache;

    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @Autowired
    protected DashboardService dashboardService;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetService assetService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Autowired
    protected EntityViewService entityViewService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    protected TenantService tenantService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    protected TenantProfileService tenantProfileService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    protected EdgeService edgeService;

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    protected CustomerService customerService;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    protected UserService userService;

    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceProfileService deviceProfileService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetProfileService assetProfileService;

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    protected RelationService relationService;

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected AttributesService attributesService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbClusterService tbClusterService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceStateService deviceStateService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    protected EdgeEventService edgeEventService;

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @Autowired
    protected WidgetTypeService widgetTypeService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected OtaPackageService otaPackageService;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @Autowired
    protected QueueService queueService;

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Autowired
    protected PartitionService partitionService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected ResourceService resourceService;

    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @Autowired
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Autowired
    protected DataValidator<Device> deviceValidator;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @Autowired
    protected DataValidator<DeviceProfile> deviceProfileValidator;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected DataValidator<Asset> assetValidator;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected DataValidator<AssetProfile> assetProfileValidator;

    /**
     * 仪表盘对象，用于描述当前业务场景。
     */
    @Autowired
    protected DataValidator<Dashboard> dashboardValidator;

    /**
     * 实体视图对象，用于描述当前业务场景。
     */
    @Autowired
    protected DataValidator<EntityView> entityViewValidator;

    /**
     * 校验器，封装可复用的处理规则。
     */
    @Autowired
    protected DataValidator<TbResource> resourceValidator;

    /**
     * 边缘节点，承载当前步骤需要处理的内容。
     */
    @Autowired
    protected EdgeMsgConstructor edgeMsgConstructor;

    /**
     * 实体，承载当前步骤需要处理的内容。
     */
    @Autowired
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    /**
     * 规则链，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected RuleChainMsgConstructorFactory ruleChainMsgConstructorFactory;

    /**
     * 告警，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected AlarmMsgConstructorFactory alarmMsgConstructorFactory;

    /**
     * 设备，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected DeviceMsgConstructorFactory deviceMsgConstructorFactory;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetMsgConstructorFactory assetMsgConstructorFactory;

    /**
     * 实体视图，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected EntityViewMsgConstructorFactory entityViewMsgConstructorFactory;

    /**
     * 仪表盘，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected DashboardMsgConstructorFactory dashboardMsgConstructorFactory;

    /**
     * 关系，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected RelationMsgConstructorFactory relationMsgConstructorFactory;

    /**
     * 用户，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected UserMsgConstructorFactory userMsgConstructorFactory;

    /**
     * 客户，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected CustomerMsgConstructorFactory customerMsgConstructorFactory;

    /**
     * 租户，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected TenantMsgConstructorFactory tenantMsgConstructorFactory;

    /**
     * 部件，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected WidgetMsgConstructorFactory widgetMsgConstructorFactory;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AdminSettingsMsgConstructorFactory adminSettingsMsgConstructorFactory;

    /**
     * 消息，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected OtaPackageMsgConstructorFactory otaPackageMsgConstructorFactory;

    /**
     * 队列，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected QueueMsgConstructorFactory queueMsgConstructorFactory;

    /**
     * 消息，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected ResourceMsgConstructorFactory resourceMsgConstructorFactory;

    /**
     * 告警，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected AlarmEdgeProcessorFactory alarmEdgeProcessorFactory;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetEdgeProcessorFactory assetEdgeProcessorFactory;

    /**
     * 实体视图，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected EntityViewProcessorFactory entityViewProcessorFactory;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @Autowired
    protected EdgeSynchronizationManager edgeSynchronizationManager;

    /**
     * 回调，提供当前类调用的业务操作。
     */
    @Autowired
    protected DbCallbackExecutorService dbCallbackExecutorService;

    /**
     * 功能：保存或创建边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `action`：`action` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                   EdgeId edgeId,
                                                   EdgeEventType type,
                                                   EdgeEventActionType action,
                                                   EntityId entityId,
                                                   JsonNode body) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, edgeId, DataConstants.SERVER_SCOPE, DefaultDeviceStateService.ACTIVITY_STATE);
        return Futures.transformAsync(future, activeOpt -> {
            if (activeOpt.isEmpty()) {
                log.trace("Edge is not activated. Skipping event. tenantId [{}], edgeId [{}], type[{}], " +
                                "action [{}], entityId [{}], body [{}]",
                        tenantId, edgeId, type, action, entityId, body);
                return Futures.immediateFuture(null);
            }
            if (activeOpt.get().getBooleanValue().isPresent() && activeOpt.get().getBooleanValue().get()) {
                return doSaveEdgeEvent(tenantId, edgeId, type, action, entityId, body);
            } else {
                if (doSaveIfEdgeIsOffline(type, action)) {
                    return doSaveEdgeEvent(tenantId, edgeId, type, action, entityId, body);
                } else {
                    log.trace("Edge is not active at the moment. Skipping event. tenantId [{}], edgeId [{}], type[{}], " +
                                    "action [{}], entityId [{}], body [{}]",
                            tenantId, edgeId, type, action, entityId, body);
                    return Futures.immediateFuture(null);
                }
            }
        }, dbCallbackExecutorService);
    }

    /**
     * 功能：执行 `doSaveIfEdgeIsOffline` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `action`：`action` 参数。
     * 返回：判断结果。
     */
    private boolean doSaveIfEdgeIsOffline(EdgeEventType type,
                                          EdgeEventActionType action) {
        switch (action) {
            case TIMESERIES_UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
            case ALARM_ASSIGNED:
            case ALARM_UNASSIGNED:
            case CREDENTIALS_REQUEST:
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
                return true;
        }
        switch (type) {
            case ALARM:
            case ALARM_COMMENT:
            case RULE_CHAIN:
            case RULE_CHAIN_METADATA:
            case USER:
            case CUSTOMER:
            case TENANT:
            case TENANT_PROFILE:
            case WIDGETS_BUNDLE:
            case WIDGET_TYPE:
            case ADMIN_SETTINGS:
            case OTA_PACKAGE:
            case QUEUE:
            case RELATION:
                return true;
        }
        return false;
    }

    /**
     * 功能：执行 `doSaveEdgeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `action`：`action` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> doSaveEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventType type, EdgeEventActionType action, EntityId entityId, JsonNode body) {
        log.debug("Pushing event to edge queue. tenantId [{}], edgeId [{}], type[{}], " +
                        "action [{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);

        return Futures.transform(edgeEventService.saveAsync(edgeEvent), unused -> {
            tbClusterService.onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, dbCallbackExecutorService);
    }

    /**
     * 功能：处理`Action For All Edges`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `actionType`：类型。
     * - `entityId`：实体IDID。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<Void> processActionForAllEdges(TenantId tenantId, EdgeEventType type,
                                                              EdgeEventActionType actionType, EntityId entityId,
                                                              EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
            PageData<TenantId> tenantsIds;
            do {
                tenantsIds = tenantService.findTenantsIds(pageLink);
                for (TenantId tenantId1 : tenantsIds.getData()) {
                    futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, entityId, null, sourceEdgeId));
                }
                pageLink = pageLink.nextPageLink();
            } while (tenantsIds.hasNext());
        } else {
            futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, entityId, null, sourceEdgeId);
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    /**
     * 功能：处理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `actionType`：类型。
     * - `entityId`：实体IDID。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private List<ListenableFuture<Void>> processActionForAllEdgesByTenantId(TenantId tenantId,
                                                                            EdgeEventType type,
                                                                            EdgeEventActionType actionType,
                                                                            EntityId entityId,
                                                                            JsonNode body,
                                                                            EdgeId sourceEdgeId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<Edge> pageData;
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (Edge edge : pageData.getData()) {
                    if (!edge.getId().equals(sourceEdgeId)) {
                        futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, entityId, body));
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return futures;
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `msgType`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<Void> handleUnsupportedMsgType(UpdateMsgType msgType) {
        String errMsg = String.format("Unsupported msg type %s", msgType);
        log.error(errMsg);
        return Futures.immediateFailedFuture(new RuntimeException(errMsg));
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `actionType`：类型。
     * 返回：处理结果。
     */
    protected UpdateMsgType getUpdateMsgType(EdgeEventActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
            case UPDATED_COMMENT:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case ASSIGNED_TO_EDGE:
            case RELATION_ADD_OR_UPDATE:
            case ADDED_COMMENT:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
            case RELATION_DELETED:
            case DELETED_COMMENT:
            case ALARM_DELETE:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeNotificationMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        if (type.isAllEdgesRelated()) {
            return processEntityNotificationForAllEdges(tenantId, type, actionType, entityId, originatorEdgeId);
        } else {
            JsonNode body = JacksonUtil.toJsonNode(edgeNotificationMsg.getBody());
            EdgeId edgeId = safeGetEdgeId(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB());
            switch (actionType) {
                case UPDATED:
                case CREDENTIALS_UPDATED:
                case ASSIGNED_TO_CUSTOMER:
                case UNASSIGNED_FROM_CUSTOMER:
                    if (edgeId != null) {
                        return saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, body);
                    } else {
                        return processNotificationToRelatedEdges(tenantId, entityId, type, actionType, originatorEdgeId);
                    }
                case DELETED:
                    EdgeEventActionType deleted = EdgeEventActionType.DELETED;
                    if (edgeId != null) {
                        return saveEdgeEvent(tenantId, edgeId, type, deleted, entityId, body);
                    } else {
                        return Futures.transform(Futures.allAsList(processActionForAllEdgesByTenantId(tenantId, type, deleted, entityId, body, originatorEdgeId)),
                                voids -> null, dbCallbackExecutorService);
                    }
                case ASSIGNED_TO_EDGE:
                case UNASSIGNED_FROM_EDGE:
                    if (originatorEdgeId == null) {
                        ListenableFuture<Void> future = saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, body);
                        return Futures.transformAsync(future, unused -> {
                            if (type.equals(EdgeEventType.RULE_CHAIN)) {
                                return updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                            } else {
                                return Futures.immediateFuture(null);
                            }
                        }, dbCallbackExecutorService);
                    } else {
                        return Futures.immediateFuture(null);
                    }
                default:
                    return Futures.immediateFuture(null);
            }
        }
    }

    /**
     * 功能：执行 `safeGetEdgeId` 对应的处理。
     * 参数：
     * - `edgeIdMSB`：`edgeIdMSB` 参数。
     * - `edgeIdLSB`：`edgeIdLSB` 参数。
     * 返回：处理结果。
     */
    protected EdgeId safeGetEdgeId(long edgeIdMSB, long edgeIdLSB) {
        if (edgeIdMSB != 0 && edgeIdLSB != 0) {
            return new EdgeId(new UUID(edgeIdMSB, edgeIdLSB));
        } else {
            return null;
        }
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `type`：类型。
     * - `actionType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> processNotificationToRelatedEdges(TenantId tenantId, EntityId entityId, EdgeEventType type,
                                                                     EdgeEventActionType actionType, EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterableByTenantIdEntityId<EdgeId> edgeIds =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        for (EdgeId relatedEdgeId : edgeIds) {
            if (!relatedEdgeId.equals(sourceEdgeId)) {
                futures.add(saveEdgeEvent(tenantId, relatedEdgeId, type, actionType, entityId, null));
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    /**
     * 功能：更新`Dependent Rule Chains`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `processingRuleChainId`：规则链ID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (RuleChain ruleChain : pageData.getData()) {
                    if (!ruleChain.getId().equals(processingRuleChainId)) {
                        List<RuleChainConnectionInfo> connectionInfos =
                                ruleChainService.loadRuleChainMetaData(ruleChain.getTenantId(), ruleChain.getId()).getRuleChainConnections();
                        if (connectionInfos != null && !connectionInfos.isEmpty()) {
                            for (RuleChainConnectionInfo connectionInfo : connectionInfos) {
                                if (connectionInfo.getTargetRuleChainId().equals(processingRuleChainId)) {
                                    futures.add(saveEdgeEvent(tenantId,
                                            edgeId,
                                            EdgeEventType.RULE_CHAIN_METADATA,
                                            EdgeEventActionType.UPDATED,
                                            ruleChain.getId(),
                                            null));
                                }
                            }
                        }
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `actionType`：类型。
     * - `entityId`：实体IDID。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> processEntityNotificationForAllEdges(TenantId tenantId, EdgeEventType type, EdgeEventActionType actionType, EntityId entityId, EdgeId sourceEdgeId) {
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case DELETED:
            case CREDENTIALS_UPDATED: // used by USER entity
                return processActionForAllEdges(tenantId, type, actionType, entityId, sourceEdgeId);
            default:
                return Futures.immediateFuture(null);
        }
    }

    /**
     * 功能：执行 `constructEntityId` 对应的处理。
     * 参数：
     * - `entityTypeStr`：实体对象。
     * - `entityIdMSB`：实体对象。
     * - `entityIdLSB`：实体对象。
     * 返回：处理结果。
     */
    protected EntityId constructEntityId(String entityTypeStr, long entityIdMSB, long entityIdLSB) {
        EntityType entityType = EntityType.valueOf(entityTypeStr);
        switch (entityType) {
            case DEVICE:
                return new DeviceId(new UUID(entityIdMSB, entityIdLSB));
            case ASSET:
                return new AssetId(new UUID(entityIdMSB, entityIdLSB));
            case ENTITY_VIEW:
                return new EntityViewId(new UUID(entityIdMSB, entityIdLSB));
            case DASHBOARD:
                return new DashboardId(new UUID(entityIdMSB, entityIdLSB));
            case TENANT:
                return TenantId.fromUUID(new UUID(entityIdMSB, entityIdLSB));
            case CUSTOMER:
                return new CustomerId(new UUID(entityIdMSB, entityIdLSB));
            case USER:
                return new UserId(new UUID(entityIdMSB, entityIdLSB));
            case EDGE:
                return new EdgeId(new UUID(entityIdMSB, entityIdLSB));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. entityIdMSB [{}], entityIdLSB [{}]",
                        entityTypeStr, entityIdMSB, entityIdLSB);
                return null;
        }
    }

    /**
     * 功能：执行 `safeGetUUID` 对应的处理。
     * 参数：
     * - `mSB`：`mSB` 参数。
     * - `lSB`：`lSB` 参数。
     * 返回：处理结果。
     */
    protected UUID safeGetUUID(long mSB, long lSB) {
        return mSB != 0 && lSB != 0 ? new UUID(mSB, lSB) : null;
    }

    /**
     * 功能：执行 `safeGetCustomerId` 对应的处理。
     * 参数：
     * - `mSB`：`mSB` 参数。
     * - `lSB`：`lSB` 参数。
     * 返回：处理结果。
     */
    protected CustomerId safeGetCustomerId(long mSB, long lSB) {
        CustomerId customerId = null;
        UUID customerUUID = safeGetUUID(mSB, lSB);
        if (customerUUID != null) {
            customerId = new CustomerId(customerUUID);
        }
        return customerId;
    }

    /**
     * 功能：判断实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    protected boolean isEntityExists(TenantId tenantId, EntityId entityId) {
        switch (entityId.getEntityType()) {
            case TENANT:
                return tenantService.findTenantById(tenantId) != null;
            case DEVICE:
                return deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId())) != null;
            case ASSET:
                return assetService.findAssetById(tenantId, new AssetId(entityId.getId())) != null;
            case ENTITY_VIEW:
                return entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId())) != null;
            case CUSTOMER:
                return customerService.findCustomerById(tenantId, new CustomerId(entityId.getId())) != null;
            case USER:
                return userService.findUserById(tenantId, new UserId(entityId.getId())) != null;
            case DASHBOARD:
                return dashboardService.findDashboardById(tenantId, new DashboardId(entityId.getId())) != null;
            case EDGE:
                return edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId())) != null;
            default:
                return false;
        }
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    protected void createRelationFromEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(edgeId);
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        relationService.saveRelation(tenantId, relation);
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    protected TbMsgMetaData getEdgeActionTbMsgMetaData(Edge edge, CustomerId customerId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("edgeId", edge.getId().toString());
        metaData.putValue("edgeName", edge.getName());
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `customerId`：客户IDID。
     * - `msgType`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void pushEntityEventToRuleEngine(TenantId tenantId, EntityId entityId, CustomerId customerId,
                                               TbMsgType msgType, String msgData, TbMsgMetaData metaData) {
        TbMsg tbMsg = TbMsg.newMsg(msgType, entityId, customerId, metaData, TbMsgDataType.JSON, msgData);
        tbClusterService.pushMsgToRuleEngine(tenantId, entityId, tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.debug("[{}] Successfully send ENTITY_CREATED EVENT to rule engine [{}]", tenantId, msgData);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to send ENTITY_CREATED EVENT to rule engine [{}]", tenantId, msgData, t);
            }
        });
    }

    /**
     * 功能：校验资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `assetProfile`：`assetProfile` 参数。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：判断结果。
     */
    protected AssetProfile checkIfAssetProfileDefaultFieldsAssignedToEdge(TenantId tenantId, EdgeId edgeId, AssetProfile assetProfile, EdgeVersion edgeVersion) {
        switch (edgeVersion) {
            case V_3_3_3:
            case V_3_3_0:
            case V_3_4_0:
                if (assetProfile.getDefaultDashboardId() != null
                        && isEntityNotAssignedToEdge(tenantId, assetProfile.getDefaultDashboardId(), edgeId)) {
                    assetProfile.setDefaultDashboardId(null);
                }
                if (assetProfile.getDefaultEdgeRuleChainId() != null
                        && isEntityNotAssignedToEdge(tenantId, assetProfile.getDefaultEdgeRuleChainId(), edgeId)) {
                    assetProfile.setDefaultEdgeRuleChainId(null);
                }
                break;
        }
        return assetProfile;
    }

    /**
     * 功能：校验设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `deviceProfile`：设备信息或设备标识。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：判断结果。
     */
    protected DeviceProfile checkIfDeviceProfileDefaultFieldsAssignedToEdge(TenantId tenantId, EdgeId edgeId, DeviceProfile deviceProfile, EdgeVersion edgeVersion) {
        switch (edgeVersion) {
            case V_3_3_3:
            case V_3_3_0:
            case V_3_4_0:
                if (deviceProfile.getDefaultDashboardId() != null
                        && isEntityNotAssignedToEdge(tenantId, deviceProfile.getDefaultDashboardId(), edgeId)) {
                    deviceProfile.setDefaultDashboardId(null);
                }
                if (deviceProfile.getDefaultEdgeRuleChainId() != null
                        && isEntityNotAssignedToEdge(tenantId, deviceProfile.getDefaultEdgeRuleChainId(), edgeId)) {
                    deviceProfile.setDefaultEdgeRuleChainId(null);
                }
                break;
        }
        return deviceProfile;
    }

    /**
     * 功能：判断实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：判断结果。
     */
    private boolean isEntityNotAssignedToEdge(TenantId tenantId, EntityId entityId, EdgeId edgeId) {
        PageDataIterableByTenantIdEntityId<EdgeId> edgeIds =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        for (EdgeId edgeId1 : edgeIds) {
            if (edgeId1.equals(edgeId)) {
                return false;
            }
        }
        return true;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseEdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
