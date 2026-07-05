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
package org.thingsboard.rule.engine.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. 职责：提供规则节点配置转换和消息模板变量替换的通用工具方法。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的节点工具层。
 * 3. 协作对象：与 {@link TbNodeConfiguration}、{@link TbNodeException}、{@link TbMsg}、TbMsgMetaData 和 JacksonUtil 协作。
 * 4. 生命周期：工具类无实例状态，方法在节点初始化或消息处理期间被静态调用。
 * 5. 设计原因：多个规则节点都需要配置反序列化和 `${metadata}`/`$[data]` 模板处理，集中到工具类避免重复实现。
 * 6. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine 节点配置和消息处理流程。
 */
public class TbNodeUtils {

    /**
     * 数据常量，用于统一引用固定值。
     */
    private static final Pattern DATA_PATTERN = Pattern.compile("(\\$\\[)(.*?)(])");

    /**
     * 功能：执行 `convert` 对应的处理。
     * 参数：
     * - `configuration`：配置对象。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    public static <T> T convert(TbNodeConfiguration configuration, Class<T> clazz) throws TbNodeException {
        try {
            return JacksonUtil.treeToValue(configuration.getData(), clazz);
        } catch (IllegalArgumentException e) {
            throw new TbNodeException(e, true);
        }
    }

    /**
     * 功能：处理`Patterns`。
     * 参数：
     * - `patterns`：数据列表。
     * - `tbMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    public static List<String> processPatterns(List<String> patterns, TbMsg tbMsg) {
        if (!CollectionUtils.isEmpty(patterns)) {
            return patterns.stream().map(p -> processPattern(p, tbMsg)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 功能：处理`Pattern`。
     * 参数：
     * - `pattern`：`pattern` 参数。
     * - `tbMsg`：待处理消息。
     * 返回：文本结果。
     */
    public static String processPattern(String pattern, TbMsg tbMsg) {
        try {
            String result = processPattern(pattern, tbMsg.getMetaData());
            JsonNode json = JacksonUtil.toJsonNode(tbMsg.getData());
            if (json.isObject()) {
                Matcher matcher = DATA_PATTERN.matcher(result);
                while (matcher.find()) {
                    String group = matcher.group(2);
                    String[] keys = group.split("\\.");
                    JsonNode jsonNode = json;
                    for (String key : keys) {
                        if (!StringUtils.isEmpty(key) && jsonNode != null) {
                            jsonNode = jsonNode.get(key);
                        } else {
                            jsonNode = null;
                            break;
                        }
                    }

                    if (jsonNode != null && jsonNode.isValueNode()) {
                        result = result.replace(formatDataVarTemplate(group), jsonNode.asText());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process pattern!", e);
        }
    }

    /**
     * 功能：处理`Patterns`。
     * 参数：
     * - `patterns`：数据列表。
     * - `metaData`：待处理数据。
     * 返回：匹配的数据集合。
     */
    @Deprecated(since = "3.6.1", forRemoval = true)
    public static List<String> processPatterns(List<String> patterns, TbMsgMetaData metaData) {
        if (!CollectionUtils.isEmpty(patterns)) {
            return patterns.stream().map(p -> processPattern(p, metaData)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 功能：处理`Pattern`。
     * 参数：
     * - `pattern`：`pattern` 参数。
     * - `metaData`：待处理数据。
     * 返回：文本结果。
     */
    public static String processPattern(String pattern, TbMsgMetaData metaData) {
        return processTemplate(pattern, metaData.values());
    }

    /**
     * 功能：处理`Template`。
     * 参数：
     * - `template`：`template` 参数。
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    public static String processTemplate(String template, Map<String, String> data) {
        String result = template;
        for (Map.Entry<String, String> kv : data.entrySet()) {
            result = processVar(result, kv.getKey(), kv.getValue());
        }
        return result;
    }

    /**
     * 功能：处理`Var`。
     * 参数：
     * - `pattern`：`pattern` 参数。
     * - `key`：键。
     * - `val`：`val` 参数。
     * 返回：文本结果。
     */
    private static String processVar(String pattern, String key, String val) {
        return pattern.replace(formatMetadataVarTemplate(key), val);
    }

    /**
     * 功能：执行 `formatDataVarTemplate` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：文本结果。
     */
    static String formatDataVarTemplate(String key) {
        return "$[" + key + ']';
    }

    /**
     * 功能：执行 `formatMetadataVarTemplate` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：文本结果。
     */
    static String formatMetadataVarTemplate(String key) {
        return "${" + key + '}';
    }
}

/*
 * 本类总结：
 * 1. 核心职责：提供节点配置转换和规则消息模板变量替换能力。
 * 2. 核心流程：节点初始化时 convert 配置；消息处理时 processPattern 先替换元数据，再解析 JSON data 路径并替换值节点。
 * 3. 关键依赖：TbNodeConfiguration、TbMsg、TbMsgMetaData、JacksonUtil、Pattern。
 * 4. 学习重点：模板替换刻意保持轻量，只支持元数据键和 data 对象点路径，不引入完整 JSONPath。
 */
