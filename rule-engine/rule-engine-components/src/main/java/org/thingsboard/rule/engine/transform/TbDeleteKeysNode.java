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
 * 中文说明：`TbDeleteKeysNode` 是删除键节点规则节点，用于转换消息体、元数据、发起实体或拆分/包装规则链消息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbDeleteKeysNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
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

    /*
     * 本类总结：`TbDeleteKeysNode` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
