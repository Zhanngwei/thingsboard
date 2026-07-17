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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.util.TbPair;

/**
 * 中文说明：
 * 1. `TbAbstractTransformNodeWithTbMsgSource` 是 ThingsBoard Rule Engine Components 中处理消息的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
public abstract class TbAbstractTransformNodeWithTbMsgSource implements TbNode {

    /**
     * `FROM_METADATA_PROPERTY`常量，用于统一引用固定值。
     */
    protected static final String FROM_METADATA_PROPERTY = "fromMetadata";

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getNewKeyForUpgradeFromVersionZero();

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getKeyToUpgradeFromVersionOne();

    /**
     * 功能：执行 `upgrade` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * - `oldConfiguration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        ObjectNode configToUpdate = (ObjectNode) oldConfiguration;
        switch (fromVersion) {
            case 0:
                return upgradeToUseTbMsgSource(configToUpdate);
            case 1:
                return upgradeNodesWithVersionOneToUseTbMsgSource(configToUpdate);
            default:
                return new TbPair<>(false, oldConfiguration);
        }
    }

    /**
     * 功能：执行 `upgradeToUseTbMsgSource` 对应的处理。
     * 参数：
     * - `configToUpdate`：配置对象。
     * 返回：处理结果。
     */
    private TbPair<Boolean, JsonNode> upgradeToUseTbMsgSource(ObjectNode configToUpdate) throws TbNodeException {
        if (!configToUpdate.has(FROM_METADATA_PROPERTY)) {
            throw new TbNodeException("property to update: '" + FROM_METADATA_PROPERTY + "' doesn't exists in configuration!");
        }
        var value = configToUpdate.get(FROM_METADATA_PROPERTY).asText();
        if ("true".equals(value)) {
            configToUpdate.remove(FROM_METADATA_PROPERTY);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.METADATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        if ("false".equals(value)) {
            configToUpdate.remove(FROM_METADATA_PROPERTY);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.DATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        throw new TbNodeException("property to update: '" + FROM_METADATA_PROPERTY + "' has unexpected value: "
                + value + ". Allowed values: true or false!");
    }

    /**
     * 功能：执行 `upgradeNodesWithVersionOneToUseTbMsgSource` 对应的处理。
     * 参数：
     * - `configToUpdate`：配置对象。
     * 返回：处理结果。
     */
    private TbPair<Boolean, JsonNode> upgradeNodesWithVersionOneToUseTbMsgSource(ObjectNode configToUpdate) throws TbNodeException {
        if (configToUpdate.has(getNewKeyForUpgradeFromVersionZero())) {
            return new TbPair<>(false, configToUpdate);
        }
        return upgradeTbMsgSourceKey(configToUpdate, getKeyToUpgradeFromVersionOne());
    }

    /**
     * 功能：执行 `upgradeTbMsgSourceKey` 对应的处理。
     * 参数：
     * - `configToUpdate`：配置对象。
     * - `oldPropertyKey`：键。
     * 返回：处理结果。
     */
    private TbPair<Boolean, JsonNode> upgradeTbMsgSourceKey(ObjectNode configToUpdate, String oldPropertyKey) throws TbNodeException {
        if (!configToUpdate.has(oldPropertyKey)) {
            throw new TbNodeException("property to update: '" + oldPropertyKey + "' doesn't exists in configuration!");
        }
        var value = configToUpdate.get(oldPropertyKey).asText();
        if (TbMsgSource.METADATA.name().equals(value)) {
            configToUpdate.remove(oldPropertyKey);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.METADATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        if (TbMsgSource.DATA.name().equals(value)) {
            configToUpdate.remove(oldPropertyKey);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.DATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        throw new TbNodeException("property to update: '" + oldPropertyKey + "' has unexpected value: "
                + value + ". Allowed values: true or false!");
    }
}
