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
package org.thingsboard.server.actors.ruleChain;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.plugin.RuleNodeUpdatedMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.common.MultipleTbQueueTbMsgCallbackWrapper;
import org.thingsboard.server.queue.common.TbQueueTbMsgCallbackWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`RuleChainActorMessageProcessor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
public class RuleChainActorMessageProcessor extends ComponentMsgProcessor<RuleChainId> {

    /**
     * 字段说明：
     * 1. 保存 `NA_RELATION_TYPE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String NA_RELATION_TYPE = "";
    private final TbActorRef parent;
    /**
     * 字段说明：
     * 1. 保存 `self` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbActorRef self;
    private final Map<RuleNodeId, RuleNodeCtx> nodeActors;
    /**
     * 字段说明：
     * 1. 保存 `nodeRoutes` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final Map<RuleNodeId, List<RuleNodeRelation>> nodeRoutes;
    private final RuleChainService service;
    /**
     * 字段说明：
     * 1. 保存 `clusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbClusterService clusterService;
    private final TbApiUsageReportClient apiUsageClient;
    /**
     * 字段说明：
     * 1. 保存 `ruleChainName` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String ruleChainName;

    /**
     * 字段说明：
     * 1. 保存 `firstId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RuleNodeId firstId;
    private RuleNodeCtx firstNode;
    /**
     * 字段说明：
     * 1. 保存 `started` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean started;

    RuleChainActorMessageProcessor(TenantId tenantId, RuleChain ruleChain, ActorSystemContext systemContext, TbActorRef parent, TbActorRef self) {
        super(systemContext, tenantId, ruleChain.getId());
        this.apiUsageClient = systemContext.getApiUsageClient();
        this.ruleChainName = ruleChain.getName();
        this.parent = parent;
        this.self = self;
        this.nodeActors = new HashMap<>();
        this.nodeRoutes = new HashMap<>();
        this.service = systemContext.getRuleChainService();
        this.clusterService = systemContext.getClusterService();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getComponentName` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getComponentName() {
        return ruleChainName;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `start` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void start(TbActorCtx context) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!started) {
            RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
                List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
                log.trace("[{}][{}] Starting rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
                // Creating and starting the actors;
                for (RuleNode ruleNode : ruleNodeList) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    TbActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                }
                initRoutes(ruleChain, ruleNodeList);
                started = true;
            }
        } else {
            onUpdate(context);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onUpdate` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onUpdate(TbActorCtx context) {
        RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
            ruleChainName = ruleChain.getName();
            List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
            log.trace("[{}][{}] Updating rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (RuleNode ruleNode : ruleNodeList) {
                RuleNodeCtx existing = nodeActors.get(ruleNode.getId());
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (existing == null) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    TbActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                } else {
                    log.trace("[{}][{}] Updating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    existing.setSelf(ruleNode);
                    // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
                    existing.getSelfActor().tellWithHighPriority(new RuleNodeUpdatedMsg(tenantId, existing.getSelf().getId()));
                }
            }

            Set<RuleNodeId> existingNodes = ruleNodeList.stream().map(RuleNode::getId).collect(Collectors.toSet());
            List<RuleNodeId> removedRules = nodeActors.keySet().stream().filter(node -> !existingNodes.contains(node)).collect(Collectors.toList());
            removedRules.forEach(ruleNodeId -> {
                log.trace("[{}][{}] Removing rule node [{}]", tenantId, entityId, ruleNodeId);
                RuleNodeCtx removed = nodeActors.remove(ruleNodeId);
                // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
                removed.getSelfActor().tellWithHighPriority(new ComponentLifecycleMsg(tenantId, removed.getSelf().getId(), ComponentLifecycleEvent.DELETED));
            });

            initRoutes(ruleChain, ruleNodeList);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `stop` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stop(TbActorCtx ctx) {
        log.trace("[{}][{}] Stopping rule chain with {} nodes", tenantId, entityId, nodeActors.size());
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).map(TbActorRef::getActorId).forEach(ctx::stop);
        nodeActors.clear();
        nodeRoutes.clear();
        started = false;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onPartitionChangeMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onPartitionChangeMsg(PartitionChangeMsg msg) {
        log.debug("[{}][{}] onPartitionChangeMsg: [{}]", tenantId, entityId, msg);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).forEach(actorRef -> actorRef.tellWithHighPriority(msg));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createRuleNodeActor` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TbActorRef createRuleNodeActor(TbActorCtx ctx, RuleNode ruleNode) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(ruleNode.getId()),
                () -> DefaultActorService.RULE_DISPATCHER_NAME,
                () -> new RuleNodeActor.ActorCreator(systemContext, tenantId, entityId, ruleChainName, ruleNode.getId()),
                () -> true);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initRoutes` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void initRoutes(RuleChain ruleChain, List<RuleNode> ruleNodeList) {
        nodeRoutes.clear();
        // Populating the routes map;
        for (RuleNode ruleNode : ruleNodeList) {
            List<EntityRelation> relations = service.getRuleNodeRelations(TenantId.SYS_TENANT_ID, ruleNode.getId());
            log.trace("[{}][{}][{}] Processing rule node relations [{}]", tenantId, entityId, ruleNode.getId(), relations.size());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (relations.size() == 0) {
                nodeRoutes.put(ruleNode.getId(), Collections.emptyList());
            } else {
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (EntityRelation relation : relations) {
                    log.trace("[{}][{}][{}] Processing rule node relation [{}]", tenantId, entityId, ruleNode.getId(), relation.getTo());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                        RuleNodeCtx ruleNodeCtx = nodeActors.get(new RuleNodeId(relation.getTo().getId()));
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (ruleNodeCtx == null) {
                            throw new IllegalArgumentException("Rule Node [" + relation.getFrom() + "] has invalid relation to Rule node [" + relation.getTo() + "]");
                        }
                    }
                    nodeRoutes.computeIfAbsent(ruleNode.getId(), k -> new ArrayList<>())
                            .add(new RuleNodeRelation(ruleNode.getId(), relation.getTo(), relation.getType()));
                }
            }
        }

        firstId = ruleChain.getFirstRuleNodeId();
        firstNode = nodeActors.get(firstId);
        state = ComponentLifecycleState.ACTIVE;
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
    void onQueueToRuleEngineMsg(QueueToRuleEngineMsg envelope) {
        TbMsg msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        log.trace("[{}][{}] Processing message [{}]: {}", entityId, firstId, msg.getId(), msg);
        if (envelope.getRelationTypes() == null || envelope.getRelationTypes().isEmpty()) {
            onTellNext(msg, true);
        } else {
            onTellNext(msg, envelope.getMsg().getRuleNodeId(), envelope.getRelationTypes(), envelope.getFailureMessage());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onTellNext` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onTellNext(TbMsg msg, boolean useRuleNodeIdFromMsg) {
        try {
            checkComponentStateActive(msg);
            RuleNodeId targetId = useRuleNodeIdFromMsg ? msg.getRuleNodeId() : null;
            RuleNodeCtx targetCtx;
            if (targetId == null) {
                targetCtx = firstNode;
                msg = msg.copyWithRuleChainId(entityId);
            } else {
                targetCtx = nodeActors.get(targetId);
            }
            if (targetCtx != null) {
                log.trace("[{}][{}] Pushing message to target rule node", entityId, targetId);
                pushMsgToNode(targetCtx, msg, NA_RELATION_TYPE);
            } else {
                log.trace("[{}][{}] Rule node does not exist. Probably old message", entityId, targetId);
                msg.getCallback().onSuccess();
            }
        } catch (RuleNodeException rne) {
            msg.getCallback().onFailure(rne);
        } catch (Exception e) {
            msg.getCallback().onFailure(new RuleEngineException(e.getMessage(), e));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onRuleChainInputMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onRuleChainInputMsg(RuleChainInputMsg envelope) {
        var msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        if (entityId.equals(envelope.getRuleChainId())) {
            onTellNext(envelope.getMsg(), false);
        } else {
            parent.tell(envelope);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onRuleChainOutputMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onRuleChainOutputMsg(RuleChainOutputMsg envelope) {
        var msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        if (entityId.equals(envelope.getRuleChainId())) {
            var originatorNodeId = envelope.getTargetRuleNodeId();
            RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
            if (ruleNodeCtx != null && ruleNodeCtx.getSelf().isDebugMode()) {
                systemContext.persistDebugOutput(tenantId, originatorNodeId, envelope.getMsg(), envelope.getRelationType());
            }
            onTellNext(envelope.getMsg(), originatorNodeId, Collections.singleton(envelope.getRelationType()), RuleNodeException.UNKNOWN);
        } else {
            parent.tell(envelope);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onRuleChainToRuleChainMsg` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void onRuleChainToRuleChainMsg(RuleChainToRuleChainMsg envelope) {
        var msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        try {
            checkComponentStateActive(envelope.getMsg());
            if (firstNode != null) {
                pushMsgToNode(firstNode, envelope.getMsg(), envelope.getFromRelationType());
            } else {
                envelope.getMsg().getCallback().onSuccess();
            }
        } catch (RuleNodeException e) {
            log.debug("Rule Chain is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onTellNext` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void onTellNext(RuleNodeToRuleChainTellNextMsg envelope) {
        var msg = envelope.getMsg();
        if (checkMsgValid(msg)) {
            onTellNext(msg, envelope.getOriginator(), envelope.getRelationTypes(), envelope.getFailureMessage());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onTellNext` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onTellNext(TbMsg msg, RuleNodeId originatorNodeId, Set<String> relationTypes, String failureMessage) {
        try {
            checkComponentStateActive(msg);
            EntityId entityId = msg.getOriginator();
            TopicPartitionInfo tpi = systemContext.resolve(ServiceType.TB_RULE_ENGINE, msg.getQueueName(), tenantId, entityId);

            List<RuleNodeRelation> ruleNodeRelations = nodeRoutes.get(originatorNodeId);
            if (ruleNodeRelations == null) { // When unchecked, this will cause NullPointerException when rule node doesn't exist anymore
                log.warn("[{}][{}][{}] No outbound relations (null). Probably rule node does not exist. Probably old message.", tenantId, entityId, msg.getId());
                ruleNodeRelations = Collections.emptyList();
            }

            List<RuleNodeRelation> relationsByTypes = ruleNodeRelations.stream()
                    .filter(r -> contains(relationTypes, r.getType()))
                    .collect(Collectors.toList());
            int relationsCount = relationsByTypes.size();
            if (relationsCount == 0) {
                log.trace("[{}][{}][{}] No outbound relations to process", tenantId, entityId, msg.getId());
                if (relationTypes.contains(TbNodeConnectionType.FAILURE)) {
                    RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
                    if (ruleNodeCtx != null) {
                        msg.getCallback().onFailure(new RuleNodeException(failureMessage, ruleChainName, ruleNodeCtx.getSelf()));
                    } else {
                        log.debug("[{}] Failure during message processing by Rule Node [{}]. Enable and see debug events for more info", entityId, originatorNodeId.getId());
                        msg.getCallback().onFailure(new RuleEngineException("Failure during message processing by Rule Node [" + originatorNodeId.getId().toString() + "]"));
                    }
                } else {
                    msg.getCallback().onSuccess();
                }
            } else if (relationsCount == 1) {
                for (RuleNodeRelation relation : relationsByTypes) {
                    log.trace("[{}][{}][{}] Pushing message to single target: [{}]", tenantId, entityId, msg.getId(), relation.getOut());
                    pushToTarget(tpi, msg, relation.getOut(), relation.getType());
                }
            } else {
                MultipleTbQueueTbMsgCallbackWrapper callbackWrapper = new MultipleTbQueueTbMsgCallbackWrapper(relationsCount, msg.getCallback());
                log.trace("[{}][{}][{}] Pushing message to multiple targets: [{}]", tenantId, entityId, msg.getId(), relationsByTypes);
                for (RuleNodeRelation relation : relationsByTypes) {
                    EntityId target = relation.getOut();
                    putToQueue(tpi, msg, callbackWrapper, target);
                }
            }
        } catch (RuleNodeException rne) {
            msg.getCallback().onFailure(rne);
        } catch (Exception e) {
            log.warn("[" + tenantId + "]" + "[" + entityId + "]" + "[" + msg.getId() + "]" + " onTellNext failure", e);
            msg.getCallback().onFailure(new RuleEngineException("onTellNext - " + e.getMessage(), e));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `putToQueue` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void putToQueue(TopicPartitionInfo tpi, TbMsg msg, TbQueueCallback callbackWrapper, EntityId target) {
        switch (target.getEntityType()) {
            case RULE_NODE:
                putToQueue(tpi, msg.copyWithRuleNodeId(entityId, new RuleNodeId(target.getId()), UUID.randomUUID()), callbackWrapper);
                break;
            case RULE_CHAIN:
                putToQueue(tpi, msg.copyWithRuleChainId(new RuleChainId(target.getId()), UUID.randomUUID()), callbackWrapper);
                break;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushToTarget` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushToTarget(TopicPartitionInfo tpi, TbMsg msg, EntityId target, String fromRelationType) {
        if (tpi.isMyPartition()) {
            switch (target.getEntityType()) {
                case RULE_NODE:
                    pushMsgToNode(nodeActors.get(new RuleNodeId(target.getId())), msg, fromRelationType);
                    break;
                case RULE_CHAIN:
                    parent.tell(new RuleChainToRuleChainMsg(new RuleChainId(target.getId()), entityId, msg, fromRelationType));
                    break;
            }
        } else {
            putToQueue(tpi, msg, new TbQueueTbMsgCallbackWrapper(msg.getCallback()), target);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `putToQueue` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void putToQueue(TopicPartitionInfo tpi, TbMsg newMsg, TbQueueCallback callbackWrapper) {
        ToRuleEngineMsg toQueueMsg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(newMsg))
                .build();
        clusterService.pushMsgToRuleEngine(tpi, newMsg.getId(), toQueueMsg, callbackWrapper);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `contains` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean contains(Set<String> relationTypes, String type) {
        if (relationTypes == null) {
            return true;
        }
        for (String relationType : relationTypes) {
            if (relationType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushMsgToNode` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushMsgToNode(RuleNodeCtx nodeCtx, TbMsg msg, String fromRelationType) {
        if (nodeCtx != null) {
            nodeCtx.getSelfActor().tell(new RuleChainToRuleNodeMsg(new DefaultTbContext(systemContext, ruleChainName, nodeCtx), msg, fromRelationType));
        } else {
            log.error("[{}][{}] RuleNodeCtx is empty", entityId, ruleChainName);
            msg.getCallback().onFailure(new RuleEngineException("Rule Node CTX is empty"));
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getInactiveException` 对应的Actor 通信与消息处理类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 ActorService 创建，随组件初始化、消息投递和停止流程变化时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected RuleNodeException getInactiveException() {
        RuleNode firstRuleNode = firstNode != null ? firstNode.getSelf() : null;
        return new RuleNodeException("Rule Chain is not active!  Failed to initialize.", ruleChainName, firstRuleNode);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainActorMessageProcessor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
