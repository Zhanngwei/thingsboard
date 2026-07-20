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
 * 1. `TbNodeUtils` 是 ThingsBoard Rule Engine API 中处理规则节点通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
