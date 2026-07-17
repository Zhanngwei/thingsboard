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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 中文说明：
 * 1. `TbRenameKeysNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Rename Keys` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractTransformNodeWithTbMsgSource`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "rename keys",
        version = 2,
        configClazz = TbRenameKeysNodeConfiguration.class,
        nodeDescription = "Renames message or message metadata keys.",
        nodeDetails = "Renames keys in the message or message metadata according to the provided mapping. " +
                "If key to rename doesn't exist in the specified source (message or message metadata) it will be ignored.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeRenameKeysConfig",
        icon = "find_replace"
)
public class TbRenameKeysNode extends TbAbstractTransformNodeWithTbMsgSource {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbRenameKeysNodeConfiguration config;
    private Map<String, String> renameKeysMapping;
    /**
     * `renameIn` 字段，保存当前对象的对应属性。
     */
    private TbMsgSource renameIn;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRenameKeysNodeConfiguration.class);
        this.renameIn = config.getRenameIn();
        this.renameKeysMapping = config.getRenameKeysMapping();
        if (renameIn == null) {
            throw new TbNodeException("RenameIn can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        if (renameKeysMapping == null || renameKeysMapping.isEmpty()) {
            throw new TbNodeException("At least one mapping entry should be specified!");
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
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsgMetaData metaDataCopy = msg.getMetaData().copy();
        String data = msg.getData();
        boolean msgChanged = false;
        switch (renameIn) {
            case METADATA:
                Map<String, String> metaDataMap = metaDataCopy.getData();
                for (Map.Entry<String, String> entry : renameKeysMapping.entrySet()) {
                    String currentKeyName = entry.getKey();
                    String newKeyName = entry.getValue();
                    if (metaDataMap.containsKey(currentKeyName)) {
                        msgChanged = true;
                        String value = metaDataMap.get(currentKeyName);
                        metaDataMap.put(newKeyName, value);
                        metaDataMap.remove(currentKeyName);
                    }
                }
                metaDataCopy = new TbMsgMetaData(metaDataMap);
                break;
            case DATA:
                JsonNode dataNode = JacksonUtil.toJsonNode(data);
                if (dataNode.isObject()) {
                    ObjectNode msgData = (ObjectNode) dataNode;
                    for (Map.Entry<String, String> entry : renameKeysMapping.entrySet()) {
                        String currentKeyName = entry.getKey();
                        String newKeyName = entry.getValue();
                        if (msgData.has(currentKeyName)) {
                            msgChanged = true;
                            JsonNode value = msgData.get(currentKeyName);
                            msgData.set(newKeyName, value);
                            msgData.remove(currentKeyName);
                        }
                    }
                    data = JacksonUtil.toString(msgData);
                }
                break;
            default:
                log.debug("Unexpected RenameIn value: {}. Allowed values: {}", renameIn, TbMsgSource.values());
        }
        ctx.tellSuccess(msgChanged ? TbMsg.transformMsg(msg, metaDataCopy, data) : msg);
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getNewKeyForUpgradeFromVersionZero() {
        return "renameIn";
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getKeyToUpgradeFromVersionOne() {
        return FROM_METADATA_PROPERTY;
    }
}
