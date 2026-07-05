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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;

/**
 * 中文说明：`TbGetTenantAttributeNode` 是获取租户属性节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGetEntityDataNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "tenant attributes",
        configClazz = TbGetEntityDataNodeConfiguration.class,
        version = 1,
        nodeDescription = "Adds message originator tenant attributes or latest telemetry into message or message metadata",
        nodeDetails = "Useful when you need to retrieve some common configuration or threshold set " +
                "that is stored as tenant attributes or telemetry data and use it for further message processing.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeTenantAttributesConfig")
public class TbGetTenantAttributeNode extends TbAbstractGetEntityDataNode<TenantId> {

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public TbGetEntityDataNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetEntityDataNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getDataMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        return config;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `originator`：`originator` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<TenantId> findEntityAsync(TbContext ctx, EntityId originator) {
        return Futures.immediateFuture(ctx.getTenantId());
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
        return fromVersion == 0 ? upgradeToUseFetchToAndDataToFetch(oldConfiguration) : new TbPair<>(false, oldConfiguration);
    }

    /*
     * 本类总结：`TbGetTenantAttributeNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
