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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `TbCopyKeysNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Copy Keys` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractTransformNodeWithTbMsgSource`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "copy key-value pairs",
        version = 2,
        configClazz = TbCopyKeysNodeConfiguration.class,
        nodeDescription = "Copies key-value pairs from message to message metadata or vice-versa.",
        nodeDetails = "Copies key-value pairs from the message to message metadata, or vice-versa, according to the configured direction and keys. " +
                "Regular expressions can be used to define which keys-value pairs to copy. Any configured key not found in the source will be ignored.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeCopyKeysConfig",
        icon = "content_copy"
)
public class TbCopyKeysNode extends TbAbstractTransformNodeWithTbMsgSource {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbCopyKeysNodeConfiguration config;
    /**
     * `copyFrom` 字段，保存当前对象的对应属性。
     */
    private TbMsgSource copyFrom;
    /**
     * 键列表，用于保存一组待处理对象。
     */
    private List<Pattern> compiledKeyPatterns;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCopyKeysNodeConfiguration.class);
        this.copyFrom = config.getCopyFrom();
        if (copyFrom == null) {
            throw new TbNodeException("CopyFrom can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        this.compiledKeyPatterns = config.getKeys().stream().map(Pattern::compile).collect(Collectors.toList());
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
        var metaDataCopy = msg.getMetaData().copy();
        String msgData = msg.getData();
        boolean msgChanged = false;
        JsonNode dataNode = JacksonUtil.toJsonNode(msgData);
        if (dataNode.isObject()) {
            switch (copyFrom) {
                case METADATA:
                    ObjectNode msgDataNode = (ObjectNode) dataNode;
                    Map<String, String> metaDataMap = metaDataCopy.getData();
                    for (Map.Entry<String, String> entry : metaDataMap.entrySet()) {
                        String mdKey = entry.getKey();
                        String mdValue = entry.getValue();
                        if (matches(mdKey)) {
                            msgChanged = true;
                            msgDataNode.put(mdKey, mdValue);
                        }
                    }
                    msgData = JacksonUtil.toString(msgDataNode);
                    break;
                case DATA:
                    Iterator<Map.Entry<String, JsonNode>> iteratorNode = dataNode.fields();
                    while (iteratorNode.hasNext()) {
                        Map.Entry<String, JsonNode> entry = iteratorNode.next();
                        String msgKey = entry.getKey();
                        JsonNode msgValue = entry.getValue();
                        if (matches(msgKey)) {
                            msgChanged = true;
                            String value = msgValue.isTextual() ?
                                    msgValue.asText() : JacksonUtil.toString(msgValue);
                            metaDataCopy.putValue(msgKey, value);
                        }
                    }
                    break;
                default:
                    log.debug("Unexpected CopyFrom value: {}. Allowed values: {}", copyFrom, TbMsgSource.values());
            }
        }
        ctx.tellSuccess(msgChanged ? TbMsg.transformMsg(msg, metaDataCopy, msgData) : msg);
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getNewKeyForUpgradeFromVersionZero() {
        return "copyFrom";
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

    /**
     * 功能：执行 `matches` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    boolean matches(String key) {
        return compiledKeyPatterns.stream().anyMatch(pattern -> pattern.matcher(key).matches());
    }
}
