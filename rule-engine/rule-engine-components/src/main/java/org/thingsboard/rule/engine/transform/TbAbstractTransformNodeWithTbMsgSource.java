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
 * 中文说明：`TbAbstractTransformNodeWithTbMsgSource` 是抽象转换节点With消息Source规则节点，用于转换消息体、元数据、发起实体或拆分/包装规则链消息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`本类本身没有独立配置类，若存在配置则由具体子类或空配置提供`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
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

    /*
     * 本类总结：`TbAbstractTransformNodeWithTbMsgSource` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
