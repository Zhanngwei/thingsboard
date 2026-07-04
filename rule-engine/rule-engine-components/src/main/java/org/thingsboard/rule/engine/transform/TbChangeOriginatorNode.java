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
/**
 * 中文说明：`TbChangeOriginatorNode` 是变更发起实体节点规则节点，用于转换消息体、元数据、发起实体或拆分/包装规则链消息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbChangeOriginatorNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbChangeOriginatorNode extends TbAbstractTransformNode<TbChangeOriginatorNodeConfiguration> {

    /**
     * 常量字段：定义 `CUSTOMER_SOURCE`，用于客户名称、客户标识或客户缓存，本身不触发外部系统调用。
     */
    private static final String CUSTOMER_SOURCE = "CUSTOMER";
    /**
     * 常量字段：定义 `TENANT_SOURCE`，用于租户上下文，本身不触发外部系统调用。
     */
    private static final String TENANT_SOURCE = "TENANT";
    /**
     * 常量字段：定义 `RELATED_SOURCE`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    private static final String RELATED_SOURCE = "RELATED";
    /**
     * 常量字段：定义 `ALARM_ORIGINATOR_SOURCE`，用于告警类型、严重级别或详情，本身不触发外部系统调用。
     */
    private static final String ALARM_ORIGINATOR_SOURCE = "ALARM_ORIGINATOR";
    /**
     * 常量字段：定义 `ENTITY_SOURCE`，用于目标实体类型、名称或标识，本身不触发外部系统调用。
     */
    private static final String ENTITY_SOURCE = "ENTITY";

    @Override
    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbChangeOriginatorNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected TbChangeOriginatorNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbChangeOriginatorNodeConfiguration.class);
        validateConfig(config);
        return config;
    }

    @Override
    /**
     * 方法说明：转换规则链消息或转换异步处理结果，供 `TbChangeOriginatorNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends EntityId> newOriginatorFuture = getNewOriginator(ctx, msg);
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(newOriginatorFuture, newOriginator -> {
            if (newOriginator == null || newOriginator.isNullUid()) {
                return Futures.immediateFailedFuture(new NoSuchElementException("Failed to find new originator!"));
            }
            return Futures.immediateFuture(List.of(ctx.transformMsgOriginator(msg, newOriginator)));
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbChangeOriginatorNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：校验配置或数据是否满足节点要求，供 `TbChangeOriginatorNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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

    /*
     * 本类总结：`TbChangeOriginatorNode` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
