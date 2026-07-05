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
 * 中文说明：`TbCopyKeysNode` 是复制键节点规则节点，用于转换消息体、元数据、发起实体或拆分/包装规则链消息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbCopyKeysNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
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

    /*
     * 本类总结：`TbCopyKeysNode` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
