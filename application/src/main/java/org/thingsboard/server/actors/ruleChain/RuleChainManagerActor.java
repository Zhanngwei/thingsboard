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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.TbEntityTypeActorIdPredicate;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.RuleChainErrorActor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.function.Function;

/**
 * Created by ashvayka on 15.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`RuleChainManagerActor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@Slf4j
public abstract class RuleChainManagerActor extends ContextAwareActor {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected final TenantId tenantId;
    private final RuleChainService ruleChainService;
    /**
     * `rootChain` 字段，保存当前对象的对应属性。
     */
    @Getter
    protected RuleChain rootChain;
    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    @Getter
    protected TbActorRef rootChainActor;

    /**
     * 是否满足`ruleChainsInitialized`条件。
     */
    protected boolean ruleChainsInitialized;

    /**
     * 功能：创建 `RuleChainManagerActor` 实例，并初始化必要字段。
     * 参数：
     * - `systemContext`：处理上下文。
     * - `tenantId`：租户IDID。
     * 返回：新创建的对象实例。
     */
    public RuleChainManagerActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.ruleChainService = systemContext.getRuleChainService();
    }

    /**
     * 功能：初始化或启动`Rule Chains`。
     * 参数：无。
     * 返回：无。
     */
    protected void initRuleChains() {
        log.debug("[{}] Initializing rule chains", tenantId);
        for (RuleChain ruleChain : new PageDataIterable<>(link -> ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, link), ContextAwareActor.ENTITY_PACK_LIMIT)) {
            RuleChainId ruleChainId = ruleChain.getId();
            log.debug("[{}|{}] Creating rule chain actor", ruleChainId.getEntityType(), ruleChain.getId());
            TbActorRef actorRef = getOrCreateActor(ruleChainId, id -> ruleChain);
            visit(ruleChain, actorRef);
            log.debug("[{}|{}] Rule Chain actor created.", ruleChainId.getEntityType(), ruleChainId.getId());
        }
        ruleChainsInitialized = true;
    }

    /**
     * 功能：停止或关闭`Rule Chains`。
     * 参数：无。
     * 返回：无。
     */
    protected void destroyRuleChains() {
        log.debug("[{}] Destroying rule chains", tenantId);
        for (RuleChain ruleChain : new PageDataIterable<>(link -> ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, link), ContextAwareActor.ENTITY_PACK_LIMIT)) {
            ctx.stop(new TbEntityActorId(ruleChain.getId()));
        }
        ruleChainsInitialized = false;
    }

    /**
     * 功能：执行 `visit` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `actorRef`：`actorRef` 参数。
     * 返回：无。
     */
    protected void visit(RuleChain entity, TbActorRef actorRef) {
        if (entity != null && entity.isRoot() && entity.getType().equals(RuleChainType.CORE)) {
            rootChain = entity;
            rootChainActor = actorRef;
        }
    }

    /**
     * 功能：获取Actor 实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    protected TbActorRef getOrCreateActor(RuleChainId ruleChainId) {
        return getOrCreateActor(ruleChainId, eId -> ruleChainService.findRuleChainById(TenantId.SYS_TENANT_ID, eId));
    }

    /**
     * 功能：获取Actor 实例。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `provider`：`provider` 参数。
     * 返回：处理结果。
     */
    protected TbActorRef getOrCreateActor(RuleChainId ruleChainId, Function<RuleChainId, RuleChain> provider) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(ruleChainId),
                () -> DefaultActorService.RULE_DISPATCHER_NAME,
                () -> {
                    RuleChain ruleChain = provider.apply(ruleChainId);
                    if (ruleChain == null) {
                        return new RuleChainErrorActor.ActorCreator(systemContext, tenantId,
                                new RuleEngineException("Rule Chain with id: " + ruleChainId + " not found!"));
                    } else {
                        return new RuleChainActor.ActorCreator(systemContext, tenantId, ruleChain);
                    }
                },
                () -> true);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    protected TbActorRef getEntityActorRef(EntityId entityId) {
        TbActorRef target = null;
        if (entityId.getEntityType() == EntityType.RULE_CHAIN) {
            target = getOrCreateActor((RuleChainId) entityId);
        }
        return target;
    }

    /**
     * 功能：执行 `broadcast` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    protected void broadcast(TbActorMsg msg) {
        ctx.broadcastToChildren(msg, new TbEntityTypeActorIdPredicate(EntityType.RULE_CHAIN));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainManagerActor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
