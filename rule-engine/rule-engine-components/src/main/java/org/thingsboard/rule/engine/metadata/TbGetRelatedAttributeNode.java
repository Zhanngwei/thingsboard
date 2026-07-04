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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Arrays;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "related entity data",
        configClazz = TbGetRelatedDataNodeConfiguration.class,
        version = 1,
        nodeDescription = "Adds originators related entity attributes or latest telemetry or fields into message or message metadata",
        nodeDetails = "Related entity lookup based on the configured relation query. " +
                "If multiple related entities are found, only first entity is used for message enrichment, other entities are discarded. " +
                "Useful when you need to retrieve data from an entity that has a relation to the message originator and use them for further message processing.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeRelatedAttributesConfig")
/**
 * 中文说明：`TbGetRelatedAttributeNode` 是获取关联属性节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGetRelatedDataNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbGetRelatedAttributeNode extends TbAbstractGetEntityDataNode<EntityId> {

    /**
     * 常量字段：定义 `RELATED_ENTITY_NOT_FOUND_MESSAGE`，用于目标实体类型、名称或标识，本身不触发外部系统调用。
     */
    private static final String RELATED_ENTITY_NOT_FOUND_MESSAGE = "Failed to find related entity to message originator using relation query specified in the configuration!";

    @Override
    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbGetRelatedAttributeNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbGetRelatedDataNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetRelatedDataNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getDataMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        return config;
    }

    @Override
    /**
     * 方法说明：按租户、实体或关系条件查询数据，供 `TbGetRelatedAttributeNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public ListenableFuture<EntityId> findEntityAsync(TbContext ctx, EntityId originator) {
        var relatedAttrConfig = (TbGetRelatedDataNodeConfiguration) config;
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(
                EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, originator, relatedAttrConfig.getRelationsQuery()),
                checkIfEntityIsPresentOrThrow(RELATED_ENTITY_NOT_FOUND_MESSAGE),
                ctx.getDbCallbackExecutor());
    }

    @Override
    /**
     * 方法说明：从消息或服务层获取需要补充的数据，供 `TbGetRelatedAttributeNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected void checkDataToFetchSupportedOrElseThrow(DataToFetch dataToFetch) throws TbNodeException {
        if (dataToFetch == null) {
            throw new TbNodeException("DataToFetch property cannot be null! Supported values are: " + Arrays.toString(DataToFetch.values()));
        }
    }

    @Override
    /**
     * 方法说明：迁移旧版本规则节点 JSON 配置结构。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ? upgradeToUseFetchToAndDataToFetch(oldConfiguration) : new TbPair<>(false, oldConfiguration);
    }

    /*
     * 本类总结：`TbGetRelatedAttributeNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
