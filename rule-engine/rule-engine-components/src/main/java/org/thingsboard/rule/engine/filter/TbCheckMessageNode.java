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
package org.thingsboard.rule.engine.filter;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：
 * 1. `TbCheckMessageNode` 是 ThingsBoard Rule Engine Components 中处理消息的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check fields presence",
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        configClazz = TbCheckMessageNodeConfiguration.class,
        nodeDescription = "Checks the presence of the specified fields in the message and/or metadata.",
        nodeDetails = "By default, the rule node checks that all specified fields are present. " +
                "Uncheck the 'Check that all selected fields are present' if the presence of at least one field is sufficient.<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckMessageConfig")
public class TbCheckMessageNode implements TbNode {

    /**
     * `gson`常量，用于统一引用固定值。
     */
    private static final Gson gson = new Gson();

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbCheckMessageNodeConfiguration config;
    /**
     * 消息列表，用于保存一组待处理对象。
     */
    private List<String> messageNamesList;
    /**
     * `metadataNamesList`列表，用于保存一组待处理对象。
     */
    private List<String> metadataNamesList;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `tbContext`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckMessageNodeConfiguration.class);
        messageNamesList = config.getMessageNames();
        metadataNamesList = config.getMetadataNames();
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            String relationType = config.isCheckAllKeys() ?
                    allKeysData(msg) && allKeysMetadata(msg) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE :
                    atLeastOneData(msg) || atLeastOneMetadata(msg) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE;
            ctx.tellNext(msg, relationType);
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    /**
     * 功能：执行 `allKeysData` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean allKeysData(TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            Map<String, String> dataMap = dataToMap(msg);
            return processAllKeys(messageNamesList, dataMap);
        }
        return true;
    }

    /**
     * 功能：执行 `allKeysMetadata` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean allKeysMetadata(TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadataMap = metadataToMap(msg);
            return processAllKeys(metadataNamesList, metadataMap);
        }
        return true;
    }

    /**
     * 功能：执行 `atLeastOneData` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean atLeastOneData(TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            Map<String, String> dataMap = dataToMap(msg);
            return processAtLeastOne(messageNamesList, dataMap);
        }
        return false;
    }

    /**
     * 功能：执行 `atLeastOneMetadata` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean atLeastOneMetadata(TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadataMap = metadataToMap(msg);
            return processAtLeastOne(metadataNamesList, metadataMap);
        }
        return false;
    }

    /**
     * 功能：处理`All Keys`。
     * 参数：
     * - `data`：待处理数据。
     * - `map`：键值映射。
     * 返回：判断结果。
     */
    private boolean processAllKeys(List<String> data, Map<String, String> map) {
        for (String field : data) {
            if (!map.containsKey(field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 功能：处理`At Least One`。
     * 参数：
     * - `data`：待处理数据。
     * - `map`：键值映射。
     * 返回：判断结果。
     */
    private boolean processAtLeastOne(List<String> data, Map<String, String> map) {
        for (String field : data) {
            if (map.containsKey(field)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：执行 `metadataToMap` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private Map<String, String> metadataToMap(TbMsg msg) {
        return msg.getMetaData().getData();
    }

    /**
     * 功能：执行 `dataToMap` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> dataToMap(TbMsg msg) {
        return (Map<String, String>) gson.fromJson(msg.getData(), Map.class);
    }
}