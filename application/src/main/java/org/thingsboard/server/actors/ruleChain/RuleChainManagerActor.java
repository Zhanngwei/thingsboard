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
 * 1. `RuleChainManagerActor` 是 ThingsBoard Application 中处理规则链消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `ContextAwareActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
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
