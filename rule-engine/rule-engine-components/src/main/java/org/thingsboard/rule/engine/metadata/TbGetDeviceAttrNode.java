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
import org.thingsboard.rule.engine.util.EntitiesRelatedDeviceIdAsyncLoader;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * 中文说明：
 * 1. `TbGetDeviceAttrNode` 是 ThingsBoard Rule Engine Components 中处理设备的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractGetAttributesNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "related device attributes",
        configClazz = TbGetDeviceAttrNodeConfiguration.class,
        version = 1,
        nodeDescription = "Add originators related device attributes and/or latest telemetry values into message or message metadata",
        nodeDetails = "Related device lookup based on the configured relation query. " +
                "If multiple related devices are found, only first device is used for message enrichment, other entities are discarded. " +
                "Useful when you need to retrieve attributes and/or latest telemetry values from device that has a relation " +
                "to the message originator and use them for further message processing.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeDeviceAttributesConfig")
public class TbGetDeviceAttrNode extends TbAbstractGetAttributesNode<TbGetDeviceAttrNodeConfiguration, DeviceId> {

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String RELATED_DEVICE_NOT_FOUND_MESSAGE = "Failed to find related device to message originator using relation query specified in the configuration!";

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbGetDeviceAttrNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetDeviceAttrNodeConfiguration.class);
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<DeviceId> findEntityIdAsync(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(
                EntitiesRelatedDeviceIdAsyncLoader.findDeviceAsync(ctx, msg.getOriginator(), config.getDeviceRelationsQuery()),
                checkIfEntityIsPresentOrThrow(RELATED_DEVICE_NOT_FOUND_MESSAGE),
                ctx.getDbCallbackExecutor());
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
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "fetchToData",
                        TbMsgSource.DATA.name(),
                        TbMsgSource.METADATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }
}
