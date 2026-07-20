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
package org.thingsboard.rule.engine.transform;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesAlarmOriginatorIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesByNameAndTypeLoader;
import org.thingsboard.rule.engine.util.EntitiesCustomerIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 中文说明：
 * 1. `TbChangeOriginatorNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Change Originator` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractTransformNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "change originator",
        configClazz = TbChangeOriginatorNodeConfiguration.class,
        nodeDescription = "Change message originator to Tenant/Customer/Related Entity/Alarm Originator/Entity by name pattern.",
        nodeDetails = "Configuration: <ul><li><strong>Customer</strong> - use customer of incoming message originator as new originator. " +
                "Only for assigned to customer originators with one of the following type: 'User', 'Asset', 'Device'.</li>" +
                "<li><strong>Tenant</strong> - use current tenant as new originator.</li>" +
                "<li><strong>Related Entity</strong> - use related entity as new originator. Lookup based on configured relation query. " +
                "If multiple related entities are found, only first entity is used as new originator, other entities are discarded.</li>" +
                "<li><strong>Alarm Originator</strong> - use alarm originator as new originator. Only if incoming message originator is alarm entity.</li>" +
                "<li><strong>Entity by name pattern</strong> - specify entity type and name pattern of new originator. Following entity types are supported: " +
                "'Device', 'Asset', 'Entity View', 'Edge' or 'User'.</li></ul>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeChangeOriginatorConfig",
        icon = "find_replace"
)
public class TbChangeOriginatorNode extends TbAbstractTransformNode<TbChangeOriginatorNodeConfiguration> {

    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_SOURCE = "CUSTOMER";
    /**
     * 租户常量，用于统一引用固定值。
     */
    private static final String TENANT_SOURCE = "TENANT";
    /**
     * `RELATED_SOURCE`常量，用于统一引用固定值。
     */
    private static final String RELATED_SOURCE = "RELATED";
    /**
     * 告警常量，用于统一引用固定值。
     */
    private static final String ALARM_ORIGINATOR_SOURCE = "ALARM_ORIGINATOR";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String ENTITY_SOURCE = "ENTITY";

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbChangeOriginatorNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbChangeOriginatorNodeConfiguration.class);
        validateConfig(config);
        return config;
    }

    /**
     * 功能：执行 `transform` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends EntityId> newOriginatorFuture = getNewOriginator(ctx, msg);
        return Futures.transformAsync(newOriginatorFuture, newOriginator -> {
            if (newOriginator == null || newOriginator.isNullUid()) {
                return Futures.immediateFailedFuture(new NoSuchElementException("Failed to find new originator!"));
            }
            return Futures.immediateFuture(List.of(ctx.transformMsgOriginator(msg, newOriginator)));
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取`New Originator`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private ListenableFuture<? extends EntityId> getNewOriginator(TbContext ctx, TbMsg msg) {
        switch (config.getOriginatorSource()) {
            case CUSTOMER_SOURCE:
                return EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctx, msg.getOriginator());
            case TENANT_SOURCE:
                return Futures.immediateFuture(ctx.getTenantId());
            case RELATED_SOURCE:
                return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, msg.getOriginator(), config.getRelationsQuery());
            case ALARM_ORIGINATOR_SOURCE:
                return EntitiesAlarmOriginatorIdAsyncLoader.findEntityIdAsync(ctx, msg.getOriginator());
            case ENTITY_SOURCE:
                EntityType entityType = EntityType.valueOf(config.getEntityType());
                String entityName = TbNodeUtils.processPattern(config.getEntityNamePattern(), msg);
                try {
                    EntityId targetEntity = EntitiesByNameAndTypeLoader.findEntityId(ctx, entityType, entityName);
                    return Futures.immediateFuture(targetEntity);
                } catch (IllegalStateException e) {
                    return Futures.immediateFailedFuture(e);
                }
            default:
                return Futures.immediateFailedFuture(new IllegalStateException("Unexpected originator source " + config.getOriginatorSource()));
        }
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `conf`：`conf` 参数。
     * 返回：无。
     */
    private void validateConfig(TbChangeOriginatorNodeConfiguration conf) {
        HashSet<String> knownSources = Sets.newHashSet(CUSTOMER_SOURCE, TENANT_SOURCE, RELATED_SOURCE, ALARM_ORIGINATOR_SOURCE, ENTITY_SOURCE);
        if (!knownSources.contains(conf.getOriginatorSource())) {
            log.error("Unsupported source [{}] for TbChangeOriginatorNode", conf.getOriginatorSource());
            throw new IllegalArgumentException("Unsupported source TbChangeOriginatorNode" + conf.getOriginatorSource());
        }

        if (conf.getOriginatorSource().equals(RELATED_SOURCE)) {
            if (conf.getRelationsQuery() == null) {
                log.error("Related source for TbChangeOriginatorNode should have relations query. Actual [{}]",
                        conf.getRelationsQuery());
                throw new IllegalArgumentException("Wrong config for RElated Source in TbChangeOriginatorNode" + conf.getOriginatorSource());
            }
        }

        if (conf.getOriginatorSource().equals(ENTITY_SOURCE)) {
            if (conf.getEntityType() == null) {
                log.error("Entity type not specified for [{}]", ENTITY_SOURCE);
                throw new IllegalArgumentException("Wrong config for [{}] in TbChangeOriginatorNode!" + ENTITY_SOURCE);
            }
            if (StringUtils.isEmpty(conf.getEntityNamePattern())) {
                log.error("EntityNamePattern not specified for type [{}]", conf.getEntityType());
                throw new IllegalArgumentException("Wrong config for [{}] in TbChangeOriginatorNode!" + ENTITY_SOURCE);
            }
            EntitiesByNameAndTypeLoader.checkEntityType(EntityType.valueOf(conf.getEntityType()));
        }
    }
}
