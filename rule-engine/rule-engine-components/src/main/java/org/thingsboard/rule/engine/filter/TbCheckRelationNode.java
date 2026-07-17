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
package org.thingsboard.rule.engine.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. `TbCheckRelationNode` 是 ThingsBoard Rule Engine Components 中处理实体关系的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check relation presence",
        configClazz = TbCheckRelationNodeConfiguration.class,
        version = 1,
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        nodeDescription = "Checks the presence of the relation between the originator of the message and other entities.",
        nodeDetails = "If 'check relation to specific entity' is selected, you should specify a related entity. " +
                "Otherwise, the rule node checks the presence of a relation to any entity. " +
                "In both cases, relation lookup is based on configured direction and type.<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckRelationConfig")
public class TbCheckRelationNode implements TbNode {

    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String DIRECTION_PROPERTY_NAME = "direction";

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbCheckRelationNodeConfiguration config;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    private EntityId singleEntityId;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckRelationNodeConfiguration.class);
        if (config.isCheckForSingleEntity()) {
            if (StringUtils.isEmpty(config.getEntityType()) || StringUtils.isEmpty(config.getEntityId())) {
                throw new TbNodeException("Entity should be specified!");
            }
            this.singleEntityId = EntityIdFactory.getByTypeAndId(config.getEntityType(), config.getEntityId());
            ctx.checkTenantEntity(singleEntityId);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ListenableFuture<Boolean> checkRelationFuture = config.isCheckForSingleEntity() ?
                processSingle(ctx, msg) : processList(ctx, msg);
        withCallback(checkRelationFuture,
                filterResult -> ctx.tellNext(msg, filterResult ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：处理`Single`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Boolean> processSingle(TbContext ctx, TbMsg msg) {
        EntityId from;
        EntityId to;
        if (EntitySearchDirection.FROM.name().equals(config.getDirection())) {
            to = singleEntityId;
            from = msg.getOriginator();
        } else {
            from = singleEntityId;
            to = msg.getOriginator();
        }
        return ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), from, to, config.getRelationType(), RelationTypeGroup.COMMON);
    }

    /**
     * 功能：处理`List`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Boolean> processList(TbContext ctx, TbMsg msg) {
        ListenableFuture<List<EntityRelation>> relationListFuture = EntitySearchDirection.FROM.name().equals(config.getDirection()) ?
                ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON) :
                ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON);
        return Futures.transformAsync(relationListFuture, this::isEmptyList, ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：判断`Empty List`。
     * 参数：
     * - `entityRelations`：实体对象。
     * 返回：判断结果。
     */
    private ListenableFuture<Boolean> isEmptyList(List<EntityRelation> entityRelations) {
        return entityRelations.isEmpty() ? Futures.immediateFuture(false) : Futures.immediateFuture(true);
    }

    /**
     * 功能：执行 `upgrade` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * - `oldConfiguration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        if (fromVersion == 0) {
            var newConfigObjectNode = (ObjectNode) oldConfiguration;
            if (!newConfigObjectNode.has(DIRECTION_PROPERTY_NAME)) {
                throw new TbNodeException("property to update: '" + DIRECTION_PROPERTY_NAME + "' doesn't exists in configuration!");
            }
            String direction = newConfigObjectNode.get(DIRECTION_PROPERTY_NAME).asText();
            if (EntitySearchDirection.TO.name().equals(direction)) {
                newConfigObjectNode.put(DIRECTION_PROPERTY_NAME, EntitySearchDirection.FROM.name());
                return new TbPair<>(true, newConfigObjectNode);
            }
            if (EntitySearchDirection.FROM.name().equals(direction)) {
                newConfigObjectNode.put(DIRECTION_PROPERTY_NAME, EntitySearchDirection.TO.name());
                return new TbPair<>(true, newConfigObjectNode);
            }
            throw new TbNodeException("property to update: '" + DIRECTION_PROPERTY_NAME + "' has invalid value!");
        }
        return new TbPair<>(false, oldConfiguration);
    }
}
