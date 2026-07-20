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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntityContainer;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;


/**
 * 中文说明：
 * 1. `TbDeleteRelationNode` 是 ThingsBoard Rule Engine Components 中处理实体关系的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractRelationActionNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete relation",
        configClazz = TbDeleteRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and then delete a relation to Originator Entity by type and direction" +
                " if 'Delete single entity' is set to true, otherwise rule node will delete all relations to the originator of the message by type and direction.",
        nodeDetails = "If the relation(s) successfully deleted -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeleteRelationConfig",
        icon = "remove_circle"
)
public class TbDeleteRelationNode extends TbAbstractRelationActionNode<TbDeleteRelationNodeConfiguration> {

    /**
     * 功能：获取实体。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbDeleteRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbDeleteRelationNodeConfiguration.class);
    }

    /**
     * 功能：保存或创建实体。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean createEntityIfNotExists() {
        return false;
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<RelationContainer> processEntityRelationAction(TbContext ctx, TbMsg msg, String relationType) {
        return getRelationContainerListenableFuture(ctx, msg, relationType);
    }

    /**
     * 功能：执行 `doProcessEntityRelationAction` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `entityContainer`：实体对象。
     * - `relationType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType) {
        return Futures.transform(processSingle(ctx, msg, entityContainer, relationType), result -> new RelationContainer(msg, result), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<RelationContainer> getRelationContainerListenableFuture(TbContext ctx, TbMsg msg, String relationType) {
        if (config.isDeleteForSingleEntity()) {
            return Futures.transformAsync(getEntity(ctx, msg), entityContainer -> doProcessEntityRelationAction(ctx, msg, entityContainer, relationType), ctx.getDbCallbackExecutor());
        } else {
            return Futures.transform(processList(ctx, msg), result -> new RelationContainer(msg, result), ctx.getDbCallbackExecutor());
        }
    }

    /**
     * 功能：处理`List`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Boolean> processList(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(processListSearchDirection(ctx, msg), entityRelations -> {
            if (entityRelations.isEmpty()) {
                return Futures.immediateFuture(true);
            } else {
                List<ListenableFuture<Boolean>> listenableFutureList = new ArrayList<>();
                for (EntityRelation entityRelation : entityRelations) {
                    listenableFutureList.add(ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), entityRelation));
                }
                return Futures.transformAsync(Futures.allAsList(listenableFutureList), booleans -> {
                    for (Boolean bool : booleans) {
                        if (!bool) {
                            return Futures.immediateFuture(false);
                        }
                    }
                    return Futures.immediateFuture(true);
                }, ctx.getDbCallbackExecutor());
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：处理`Single`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `entityContainer`：实体对象。
     * - `relationType`：类型。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Boolean> processSingle(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType) {
        SearchDirectionIds sdId = processSingleSearchDirection(msg, entityContainer);
        return Futures.transformAsync(ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON),
                result -> {
                    if (result) {
                        return processSingleDeleteRelation(ctx, sdId, relationType);
                    }
                    return Futures.immediateFuture(true);
                }, ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：处理关系。
     * 参数：
     * - `ctx`：处理上下文。
     * - `sdId`：`sdId`ID。
     * - `relationType`：类型。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Boolean> processSingleDeleteRelation(TbContext ctx, SearchDirectionIds sdId, String relationType) {
        return ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON);
    }
}
