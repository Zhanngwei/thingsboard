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

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AppActor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
public class AppActor extends ContextAwareActor {

    /**
     * 字段说明：
     * 1. 保存 `tenantService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TenantService tenantService;
    private final Set<TenantId> deletedTenants;
    /**
     * 字段说明：
     * 1. 保存 `ruleChainsInitialized` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private volatile boolean ruleChainsInitialized;

    /**
     * 方法说明：
     * 1. 职责：执行 `AppActor` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private AppActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.tenantService = systemContext.getTenantService();
        this.deletedTenants = new HashSet<>();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE)) {
            systemContext.schedulePeriodicMsgWithDelay(ctx, SessionTimeoutCheckMsg.instance(),
                    systemContext.getSessionReportTimeout(), systemContext.getSessionReportTimeout());
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `doProcess` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean doProcess(TbActorMsg msg) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!ruleChainsInitialized) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (MsgType.APP_INIT_MSG.equals(msg.getMsgType())) {
                initTenantActors();
                ruleChainsInitialized = true;
            } else {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!msg.getMsgType().isIgnoreOnStart()) {
                    log.warn("Attempt to initialize Rule Chains by unexpected message: {}", msg);
                }
                return true;
            }
        }
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
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
            // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
                onToDeviceActorMsg((TenantAwareMsg) msg, false);
                break;
            // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
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
     * 方法说明：
     * 1. 职责：执行 `initTenantActors` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `onQueueToRuleEngineMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `onComponentLifecycleMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `onToDeviceActorMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `getOrCreateTenantActor` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `onToEdgeSessionMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onProcessFailure` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable t) {
        log.error("Failed to process msg: {}", msg, t);
        return doProcessFailure(t);
    }

    /**
     * 中文说明：
     * 1. 类目的：`ActorCreator` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
     * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Actor / Command。
     */
    public static class ActorCreator extends ContextBasedCreator {

        /**
         * 方法说明：
         * 1. 职责：执行 `ActorCreator` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `createActorId` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public TbActorId createActorId() {
            return new TbEntityActorId(TenantId.SYS_TENANT_ID);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `createActor` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public TbActor createActor() {
            return new AppActor(context);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AppActor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
