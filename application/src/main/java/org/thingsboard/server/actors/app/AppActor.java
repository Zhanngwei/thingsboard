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
package org.thingsboard.server.actors.app;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ProcessFailureStrategy;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.device.SessionTimeoutCheckMsg;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.tenant.TenantActor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.edge.EdgeSessionMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 中文说明：
 * 1. `AppActor` 是 ThingsBoard Application 中处理 `App Actor` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `ContextAwareActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Slf4j
public class AppActor extends ContextAwareActor {

    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;
    private final Set<TenantId> deletedTenants;
    /**
     * 是否满足`ruleChainsInitialized`条件。
     */
    private volatile boolean ruleChainsInitialized;

    /**
     * 功能：创建 `AppActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * 返回：新创建的对象实例。
     */
    private AppActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.tenantService = systemContext.getTenantService();
        this.deletedTenants = new HashSet<>();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        if (systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE)) {
            systemContext.schedulePeriodicMsgWithDelay(ctx, SessionTimeoutCheckMsg.instance(),
                    systemContext.getSessionReportTimeout(), systemContext.getSessionReportTimeout());
        }
    }

    /**
     * 功能：执行 `doProcess` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    protected boolean doProcess(TbActorMsg msg) {
        if (!ruleChainsInitialized) {
            if (MsgType.APP_INIT_MSG.equals(msg.getMsgType())) {
                initTenantActors();
                ruleChainsInitialized = true;
            } else {
                if (!msg.getMsgType().isIgnoreOnStart()) {
                    log.warn("Attempt to initialize Rule Chains by unexpected message: {}", msg);
                }
                return true;
            }
        }
        switch (msg.getMsgType()) {
            case APP_INIT_MSG:
                break;
            case PARTITION_CHANGE_MSG:
                ctx.broadcastToChildren(msg, true);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, false);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, true);
                break;
            case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG:
                onToEdgeSessionMsg((EdgeSessionMsg) msg);
                break;
            case SESSION_TIMEOUT_MSG:
                ctx.broadcastToChildrenByType(msg, EntityType.TENANT);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 功能：初始化或启动租户。
     * 参数：无。
     * 返回：无。
     */
    private void initTenantActors() {
        log.info("Starting main system actor.");
        try {
            if (systemContext.isTenantComponentsInitEnabled()) {
                PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT);
                for (Tenant tenant : tenantIterator) {
                    log.debug("[{}] Creating tenant actor", tenant.getId());
                    getOrCreateTenantActor(tenant.getId()).ifPresentOrElse(tenantActor -> {
                        log.debug("[{}] Tenant actor created.", tenant.getId());
                    }, () -> {
                        log.debug("[{}] Skipped actor creation", tenant.getId());
                    });
                }
            }
            log.info("Main system actor started.");
        } catch (Exception e) {
            log.warn("Unknown failure", e);
        }
    }

    /**
     * 功能：处理规则引擎。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onQueueToRuleEngineMsg(QueueToRuleEngineMsg msg) {
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            msg.getMsg().getCallback().onFailure(new RuleEngineException("Message has system tenant id!"));
        } else {
            getOrCreateTenantActor(msg.getTenantId()).ifPresentOrElse(actor -> {
                actor.tell(msg);
            }, () -> msg.getMsg().getCallback().onSuccess());
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        TbActorRef target = null;
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            if (!EntityType.TENANT_PROFILE.equals(msg.getEntityId().getEntityType())) {
                log.warn("Message has system tenant id: {}", msg);
            }
        } else {
            if (EntityType.TENANT.equals(msg.getEntityId().getEntityType())) {
                TenantId tenantId = TenantId.fromUUID(msg.getEntityId().getId());
                if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                    log.info("[{}] Handling tenant deleted notification: {}", msg.getTenantId(), msg);
                    deletedTenants.add(tenantId);
                    ctx.stop(new TbEntityActorId(tenantId));
                    return;
                }
            }
            target = getOrCreateTenantActor(msg.getTenantId()).orElseGet(() -> {
                log.debug("Ignoring component lifecycle msg for tenant {} because it is not managed by this service", msg.getTenantId());
                return null;
            });
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid component lifecycle msg: {}", msg.getTenantId(), msg);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msg`：待处理消息。
     * - `priority`：`priority` 参数。
     * 返回：无。
     */
    private void onToDeviceActorMsg(TenantAwareMsg msg, boolean priority) {
        getOrCreateTenantActor(msg.getTenantId()).ifPresentOrElse(tenantActor -> {
            if (priority) {
                tenantActor.tellWithHighPriority(msg);
            } else {
                tenantActor.tell(msg);
            }
        }, () -> {
            if (msg instanceof TransportToDeviceActorMsgWrapper) {
                ((TransportToDeviceActorMsgWrapper) msg).getCallback().onSuccess();
            }
        });
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：可能存在的结果。
     */
    private Optional<TbActorRef> getOrCreateTenantActor(TenantId tenantId) {
        if (deletedTenants.contains(tenantId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ctx.getOrCreateChildActor(new TbEntityActorId(tenantId),
                () -> DefaultActorService.TENANT_DISPATCHER_NAME,
                () -> new TenantActor.ActorCreator(systemContext, tenantId),
                () -> true));
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onToEdgeSessionMsg(EdgeSessionMsg msg) {
        TbActorRef target = null;
        if (ModelConstants.SYSTEM_TENANT.equals(msg.getTenantId())) {
            log.warn("Message has system tenant id: {}", msg);
        } else {
            target = getOrCreateTenantActor(msg.getTenantId()).orElse(null);
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid edge session msg: {}", msg.getTenantId(), msg);
        }
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
        log.error("Failed to process msg: {}", msg, t);
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
         * 功能：创建 `AppActor` 实例，并初始化必要字段。
         * 参数：
         * - `context`：处理上下文。
         * 返回：新创建的对象实例。
         */
        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(TenantId.SYS_TENANT_ID);
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActor createActor() {
            return new AppActor(context);
        }
    }

}
