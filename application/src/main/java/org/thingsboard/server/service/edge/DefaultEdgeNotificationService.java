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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultEdgeNotificationService` 是 ThingsBoard Application 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `EdgeNotificationService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_IS_ROOT_BODY_KEY = "isRoot";

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    private EdgeService edgeService;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @Autowired
    private EdgeProcessor edgeProcessor;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetEdgeProcessor assetProcessor;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileEdgeProcessor assetProfileEdgeProcessor;

    /**
     * 设备，负责处理对应任务或消息。
     */
    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    /**
     * 设备配置，负责处理对应任务或消息。
     */
    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileEdgeProcessor;

    /**
     * 实体视图，负责处理对应任务或消息。
     */
    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

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
     * 用户，负责处理对应任务或消息。
     */
    @Autowired
    private UserEdgeProcessor userProcessor;

    /**
     * 客户，负责处理对应任务或消息。
     */
    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    private OtaPackageEdgeProcessor otaPackageProcessor;

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
     * 队列，负责处理对应任务或消息。
     */
    @Autowired
    private QueueEdgeProcessor queueProcessor;

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
     * 告警，负责处理对应任务或消息。
     */
    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    /**
     * 关系，负责处理对应任务或消息。
     */
    @Autowired
    private RelationEdgeProcessor relationProcessor;

    /**
     * 边缘节点，负责处理对应任务或消息。
     */
    @Autowired
    private ResourceEdgeProcessor resourceEdgeProcessor;

    /**
     * 事件，表示当前对象的对应属性。
     */
    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    @Value("${actors.system.edge_dispatcher_pool_size:4}")
    private int edgeDispatcherSize;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private ExecutorService executor;

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initExecutor() {
        executor = ThingsBoardExecutors.newWorkStealingPool(edgeDispatcherSize, "edge-notifications");
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        ObjectNode isRootBody = JacksonUtil.newObjectNode();
        isRootBody.put(EDGE_IS_ROOT_BODY_KEY, Boolean.TRUE);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edge.getId()).entityId(ruleChainId)
                .body(JacksonUtil.toString(isRootBody)).actionType(ActionType.UPDATED).build());
        return savedEdge;
    }

    /**
     * 功能：发送或提交边缘节点。
     * 参数：
     * - `edgeNotificationMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
        log.debug("[{}] Pushing notification to edge {}", tenantId, edgeNotificationMsg);
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        try {
            executor.submit(() -> {
                try {
                    if (deadline < System.nanoTime()) {
                        log.warn("[{}] Skipping notification message because deadline reached {}", tenantId, edgeNotificationMsg);
                        return;
                    }
                    EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
                    switch (type) {
                        case EDGE:
                            edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ASSET:
                            assetProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ASSET_PROFILE:
                            assetProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case DEVICE:
                            deviceProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case DEVICE_PROFILE:
                            deviceProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ENTITY_VIEW:
                            entityViewProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case DASHBOARD:
                            dashboardProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case RULE_CHAIN:
                            ruleChainProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case USER:
                            userProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case CUSTOMER:
                            customerProcessor.processCustomerNotification(tenantId, edgeNotificationMsg);
                            break;
                        case OTA_PACKAGE:
                            otaPackageProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case WIDGETS_BUNDLE:
                            widgetBundleProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case WIDGET_TYPE:
                            widgetTypeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case QUEUE:
                            queueProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ALARM:
                            alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ALARM_COMMENT:
                            alarmProcessor.processAlarmCommentNotification(tenantId, edgeNotificationMsg);
                            break;
                        case RELATION:
                            relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                            break;
                        case TENANT:
                            tenantEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case TENANT_PROFILE:
                            tenantProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case TB_RESOURCE:
                            resourceEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        default:
                            log.warn("[{}] Edge event type [{}] is not designed to be pushed to edge", tenantId, type);
                    }
                } catch (Exception e) {
                    callBackFailure(tenantId, edgeNotificationMsg, callback, e);
                }
            });
            callback.onSuccess();
        } catch (Exception e) {
            callBackFailure(tenantId, edgeNotificationMsg, callback, e);
        }
    }

    /**
     * 功能：执行 `callBackFailure` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeNotificationMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * - `throwable`：`throwable` 参数。
     * 返回：无。
     */
    private void callBackFailure(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("[{}] Can't push to edge updates, edgeNotificationMsg [{}]", tenantId, edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
    }
}
