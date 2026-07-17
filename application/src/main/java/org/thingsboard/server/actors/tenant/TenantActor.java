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
package org.thingsboard.server.actors.tenant;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ProcessFailureStrategy;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorNotRegisteredException;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.TbEntityTypeActorIdPredicate;
import org.thingsboard.server.actors.device.DeviceActorCreator;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.aware.DeviceAwareMsg;
import org.thingsboard.server.common.msg.aware.RuleChainAwareMsg;
import org.thingsboard.server.common.msg.edge.EdgeSessionMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. `TenantActor` 是 ThingsBoard Application 中处理租户消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `RuleChainManagerActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Slf4j
public class TenantActor extends RuleChainManagerActor {

    /**
     * 是否为规则引擎。
     */
    private boolean isRuleEngine;
    private boolean isCore;
    /**
     * 状态，表示当前对象所处状态。
     */
    private ApiUsageState apiUsageState;

    /**
     * `deletedDevices`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<DeviceId> deletedDevices;

    /**
     * 功能：创建 `TenantActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * - `tenantId`：租户IDID。
     * 返回：新创建的对象实例。
     */
    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, tenantId);
        this.deletedDevices = new HashSet<>();
    }

    /**
     * 是否满足租户条件。
     */
    boolean cantFindTenant = false;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}] Starting tenant actor.", tenantId);
        try {
            Tenant tenant = systemContext.getTenantService().findTenantById(tenantId);
            if (tenant == null) {
                cantFindTenant = true;
                log.info("[{}] Started tenant actor for missing tenant.", tenantId);
            } else {
                isCore = systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE);
                isRuleEngine = systemContext.getServiceInfoProvider().isService(ServiceType.TB_RULE_ENGINE);
                if (isRuleEngine) {
                    if (systemContext.getPartitionService().isManagedByCurrentService(tenantId)) {
                        try {
                            if (getApiUsageState().isReExecEnabled()) {
                                log.debug("[{}] Going to init rule chains", tenantId);
                                initRuleChains();
                            } else {
                                log.info("[{}] Skip init of the rule chains due to API limits", tenantId);
                            }
                        } catch (Exception e) {
                            log.info("Failed to check ApiUsage \"ReExecEnabled\"!!!", e);
                            cantFindTenant = true;
                        }
                    } else {
                        log.info("Tenant {} is not managed by current service, skipping rule chains init", tenantId);
                    }
                }
                log.debug("[{}] Tenant actor started.", tenantId);
            }
        } catch (Exception e) {
            log.warn("[{}] Unknown failure", tenantId, e);
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：
     * - `stopReason`：`stopReason` 参数。
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {
        log.info("[{}] Stopping tenant actor.", tenantId);
    }

    /**
     * 功能：执行 `doProcess` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    protected boolean doProcess(TbActorMsg msg) {
        if (cantFindTenant) {
            log.info("[{}] Processing missing Tenant msg: {}", tenantId, msg);
            if (msg.getMsgType().equals(MsgType.QUEUE_TO_RULE_ENGINE_MSG)) {
                QueueToRuleEngineMsg queueMsg = (QueueToRuleEngineMsg) msg;
                queueMsg.getMsg().getCallback().onSuccess();
            } else if (msg.getMsgType().equals(MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG)) {
                TransportToDeviceActorMsgWrapper transportMsg = (TransportToDeviceActorMsgWrapper) msg;
                transportMsg.getCallback().onSuccess();
            }
            return true;
        }
        switch (msg.getMsgType()) {
            case PARTITION_CHANGE_MSG:
                onPartitionChangeMsg((PartitionChangeMsg) msg);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg, false);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg, true);
                break;
            case SESSION_TIMEOUT_MSG:
                ctx.broadcastToChildrenByType(msg, EntityType.DEVICE);
                break;
            case RULE_CHAIN_INPUT_MSG:
            case RULE_CHAIN_OUTPUT_MSG:
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                onRuleChainMsg((RuleChainAwareMsg) msg);
                break;
            case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG:
                onToEdgeSessionMsg((EdgeSessionMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 功能：判断分区。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    private boolean isMyPartition(EntityId entityId) {
        return systemContext.resolve(ServiceType.TB_CORE, tenantId, entityId).isMyPartition();
    }

    /**
     * 功能：处理规则引擎。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onQueueToRuleEngineMsg(QueueToRuleEngineMsg msg) {
        if (!isRuleEngine) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
            return;
        }
        TbMsg tbMsg = msg.getMsg();
        if (getApiUsageState().isReExecEnabled()) {
            if (tbMsg.getRuleChainId() == null) {
                if (getRootChainActor() != null) {
                    getRootChainActor().tell(msg);
                } else {
                    tbMsg.getCallback().onFailure(new RuleEngineException("No Root Rule Chain available!"));
                    log.info("[{}] No Root Chain: {}", tenantId, msg);
                }
            } else {
                try {
                    ctx.tell(new TbEntityActorId(tbMsg.getRuleChainId()), msg);
                } catch (TbActorNotRegisteredException ex) {
                    log.trace("Received message for non-existing rule chain: [{}]", tbMsg.getRuleChainId());
                    //TODO: 3.1 Log it to dead letters queue;
                    tbMsg.getCallback().onSuccess();
                }
            }
        } else {
            log.trace("[{}] Ack message because Rule Engine is disabled", tenantId);
            tbMsg.getCallback().onSuccess();
        }
    }

    /**
     * 功能：处理规则链。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onRuleChainMsg(RuleChainAwareMsg msg) {
        if (getApiUsageState().isReExecEnabled()) {
            getOrCreateActor(msg.getRuleChainId()).tell(msg);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msg`：待处理消息。
     * - `priority`：`priority` 参数。
     * 返回：无。
     */
    private void onToDeviceActorMsg(DeviceAwareMsg msg, boolean priority) {
        if (!isCore) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
        }
        if (deletedDevices.contains(msg.getDeviceId())) {
            log.debug("RECEIVED MESSAGE FOR DELETED DEVICE: {}", msg);
            return;
        }
        TbActorRef deviceActor = getOrCreateDeviceActor(msg.getDeviceId());
        if (priority) {
            deviceActor.tellWithHighPriority(msg);
        } else {
            deviceActor.tell(msg);
        }
    }

    /**
     * 功能：处理分区。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onPartitionChangeMsg(PartitionChangeMsg msg) {
        ServiceType serviceType = msg.getServiceType();
        if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
            if (systemContext.getPartitionService().isManagedByCurrentService(tenantId)) {
                if (!ruleChainsInitialized) {
                    log.info("Tenant {} is now managed by this service, initializing rule chains", tenantId);
                    initRuleChains();
                }
            } else {
                if (ruleChainsInitialized) {
                    log.info("Tenant {} is no longer managed by this service, stopping rule chains", tenantId);
                    destroyRuleChains();
                }
                return;
            }

            //To Rule Chain Actors
            broadcast(msg);
        } else if (ServiceType.TB_CORE.equals(serviceType)) {
            List<TbActorId> deviceActorIds = ctx.filterChildren(new TbEntityTypeActorIdPredicate(EntityType.DEVICE) {
                @Override
                protected boolean testEntityId(EntityId entityId) {
                    return super.testEntityId(entityId) && !isMyPartition(entityId);
                }
            });
            deviceActorIds.forEach(id -> ctx.stop(id));
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        if (msg.getEntityId().getEntityType().equals(EntityType.API_USAGE_STATE)) {
            ApiUsageState old = getApiUsageState();
            apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenantId));
            if (old.isReExecEnabled() && !apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to DISABLE Rule Engine execution.", tenantId);
                destroyRuleChains();
            } else if (!old.isReExecEnabled() && apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to ENABLE Rule Engine execution.", tenantId);
                initRuleChains();
            }
        }
        if (msg.getEntityId().getEntityType() == EntityType.EDGE) {
            EdgeId edgeId = new EdgeId(msg.getEntityId().getId());
            EdgeRpcService edgeRpcService = systemContext.getEdgeRpcService();
            if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                edgeRpcService.deleteEdge(tenantId, edgeId);
            } else if (msg.getEvent() == ComponentLifecycleEvent.UPDATED) {
                Edge edge = systemContext.getEdgeService().findEdgeById(tenantId, edgeId);
                edgeRpcService.updateEdge(tenantId, edge);
            }
        }
        if (msg.getEntityId().getEntityType() == EntityType.DEVICE && ComponentLifecycleEvent.DELETED == msg.getEvent() && isMyPartition(msg.getEntityId())) {
            DeviceId deviceId = (DeviceId) msg.getEntityId();
            onToDeviceActorMsg(new DeviceDeleteMsg(tenantId, deviceId), true);
            deletedDevices.add(deviceId);
        }
        if (isRuleEngine && ruleChainsInitialized) {
            TbActorRef target = getEntityActorRef(msg.getEntityId());
            if (target != null) {
                if (msg.getEntityId().getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChain ruleChain = systemContext.getRuleChainService().
                            findRuleChainById(tenantId, new RuleChainId(msg.getEntityId().getId()));
                    if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
                        visit(ruleChain, target);
                    }
                }
                target.tellWithHighPriority(msg);
            } else {
                log.debug("[{}] Invalid component lifecycle msg: {}", tenantId, msg);
            }
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    private TbActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(deviceId),
                () -> DefaultActorService.DEVICE_DISPATCHER_NAME,
                () -> new DeviceActorCreator(systemContext, tenantId, deviceId),
                () -> true);
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onToEdgeSessionMsg(EdgeSessionMsg msg) {
        systemContext.getEdgeRpcService().onToEdgeSessionMsg(tenantId, msg);
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：处理结果。
     */
    private ApiUsageState getApiUsageState() {
        if (apiUsageState == null) {
            apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenantId));
        }
        return apiUsageState;
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `msg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    @Override
    public ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable t) {
        log.error("[{}] Failed to process msg: {}", tenantId, msg, t);
        return doProcessFailure(t);
    }

    /**
     * 中文说明：
     * 1. `ActorCreator` 是 ThingsBoard Application 中创建或提供 `Actor Creator` 对象的构造组件。
     * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
     * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
     * 4. 直接依赖的类型边界包括 `ContextBasedCreator`。
     * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
     * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
     */
    public static class ActorCreator extends ContextBasedCreator {

        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;

        /**
         * 功能：创建 `TenantActor` 实例，并初始化必要字段。
         * 参数：
         * - `context`：处理上下文。
         * - `tenantId`：租户IDID。
         * 返回：新创建的对象实例。
         */
        public ActorCreator(ActorSystemContext context, TenantId tenantId) {
            super(context);
            this.tenantId = tenantId;
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(tenantId);
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActor createActor() {
            return new TenantActor(context, tenantId);
        }
    }

}
