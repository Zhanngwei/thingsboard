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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `TbDeleteKeysNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Delete Keys` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractTransformNodeWithTbMsgSource`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "delete key-value pairs",
        version = 2,
        configClazz = TbDeleteKeysNodeConfiguration.class,
        nodeDescription = "Deletes key-value pairs from message or message metadata.",
        nodeDetails = "Deletes key-value pairs from the message or message metadata according to the configured " +
                "keys and/or regular expressions.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDeleteKeysConfig",
        icon = "remove_circle"
)
public class TbDeleteKeysNode extends TbAbstractTransformNodeWithTbMsgSource {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbDeleteKeysNodeConfiguration config;
    /**
     * `deleteFrom` 字段，保存当前对象的对应属性。
     */
    private TbMsgSource deleteFrom;
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
        this.config = TbNodeUtils.convert(configuration, TbDeleteKeysNodeConfiguration.class);
        this.deleteFrom = config.getDeleteFrom();
        if (deleteFrom == null) {
            throw new TbNodeException("DeleteFrom can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
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
        var msgDataStr = msg.getData();
        boolean hasNoChanges = false;
        switch (deleteFrom) {
            case METADATA:
                var metaDataMap = metaDataCopy.getData();
                var mdKeysToDelete = metaDataMap.keySet()
                        .stream()
                        .filter(this::matches)
                        .collect(Collectors.toList());
                mdKeysToDelete.forEach(metaDataMap::remove);
                metaDataCopy = new TbMsgMetaData(metaDataMap);
                hasNoChanges = mdKeysToDelete.isEmpty();
                break;
            case DATA:
                JsonNode dataNode = JacksonUtil.toJsonNode(msgDataStr);
                if (dataNode.isObject()) {
                    var msgDataObject = (ObjectNode) dataNode;
                    var msgKeysToDelete = new ArrayList<String>();
                    dataNode.fieldNames().forEachRemaining(key -> {
                        if (matches(key)) {
                            msgKeysToDelete.add(key);
                        }
                    });
                    msgDataObject.remove(msgKeysToDelete);
                    msgDataStr = JacksonUtil.toString(msgDataObject);
                    hasNoChanges = msgKeysToDelete.isEmpty();
                }
                break;
            default:
                log.debug("Unexpected DeleteFrom value: {}. Allowed values: {}", deleteFrom, TbMsgSource.values());
        }
        ctx.tellSuccess(hasNoChanges ? msg : TbMsg.transformMsg(msg, metaDataCopy, msgDataStr));
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getNewKeyForUpgradeFromVersionZero() {
        return "deleteFrom";
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getKeyToUpgradeFromVersionOne() {
        return "dataToFetch";
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
