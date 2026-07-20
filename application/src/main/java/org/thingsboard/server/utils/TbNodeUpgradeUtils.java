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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

/**
 * 中文说明：
 * 1. `TbNodeUpgradeUtils` 是 ThingsBoard Application 中处理规则节点通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class TbNodeUpgradeUtils {

    /**
     * 功能：执行 `upgradeConfigurationAndVersion` 对应的处理。
     * 参数：
     * - `node`：`node` 参数。
     * - `nodeInfo`：`nodeInfo` 参数。
     * 返回：无。
     */
    public static void upgradeConfigurationAndVersion(RuleNode node, RuleNodeClassInfo nodeInfo) {
        JsonNode oldConfiguration = node.getConfiguration();
        int configurationVersion = node.getConfigurationVersion();

        int currentVersion = nodeInfo.getCurrentVersion();
        var configClass = nodeInfo.getAnnotation().configClazz();

        if (oldConfiguration == null || !oldConfiguration.isObject()) {
            log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                            "Current configuration is null or not a json object. " +
                            "Going to set default configuration ... ",
                    node.getId(), node.getType(), configurationVersion, currentVersion);
            node.setConfiguration(getDefaultConfig(configClass));
        } else {
            var tbVersionedNode = getTbVersionedNode(nodeInfo);
            try {
                JsonNode queueName = oldConfiguration.get(QUEUE_NAME);
                TbPair<Boolean, JsonNode> upgradeResult = tbVersionedNode.upgrade(configurationVersion, oldConfiguration);
                if (upgradeResult.getFirst()) {
                    node.setConfiguration(upgradeResult.getSecond());
                    if (nodeInfo.getAnnotation().hasQueueName() && queueName != null && queueName.isTextual()) {
                        node.setQueueName(queueName.asText());
                    }
                }
            } catch (Exception e) {
                try {
                    JacksonUtil.treeToValue(oldConfiguration, configClass);
                } catch (Exception ex) {
                    log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                                    "Going to set default configuration ... ",
                            node.getId(), node.getType(), configurationVersion, currentVersion, e);
                    node.setConfiguration(getDefaultConfig(configClass));
                }
            }
        }
        node.setConfigurationVersion(currentVersion);
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `nodeInfo`：`nodeInfo` 参数。
     * 返回：处理结果。
     */
    @SneakyThrows
    private static TbNode getTbVersionedNode(RuleNodeClassInfo nodeInfo) {
        return (TbNode) nodeInfo.getClazz().getDeclaredConstructor().newInstance();
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `configClass`：配置对象。
     * 返回：处理结果。
     */
    @SneakyThrows
    private static JsonNode getDefaultConfig(Class<? extends NodeConfiguration> configClass) {
        return JacksonUtil.valueToTree(configClass.getDeclaredConstructor().newInstance().defaultConfiguration());
    }

}
