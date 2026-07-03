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
     * 中文说明：匹配消息 data 中 `$[path]` 形式变量的正则，数据来源于静态编译表达式；生命周期随类加载存在。
     * 设计为 static final 字段是为了复用 Pattern，避免每条消息处理时重复编译正则。
     */
    private static final Pattern DATA_PATTERN = Pattern.compile("(\\$\\[)(.*?)(])");

    /**
     * 中文说明：
     * 1. 方法职责：把节点 JSON 配置转换成指定的强类型配置对象。
     * 2. 输入参数：configuration 是 Rule Engine 传入的配置包装，clazz 是目标配置类型。
     * 3. 返回值：目标类型配置对象。
     * 4. 调用时机：节点 init 阶段解析配置时调用。
     * 5. 调用方：具体规则节点实现。
     * 6. 使用流程：属于 Rule Engine 节点配置初始化流程。
     * 7. 线程安全：方法只使用局部变量和线程安全的 Jackson 工具调用，本身线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；直接涉及 Rule Engine 配置转换。
     */
    public static <T> T convert(TbNodeConfiguration configuration, Class<T> clazz) throws TbNodeException {
        try {
            return JacksonUtil.treeToValue(configuration.getData(), clazz);
        } catch (IllegalArgumentException e) {
            throw new TbNodeException(e, true);
        }
    }

    /**
     * 中文说明：
     * 1. 方法职责：对模板列表执行消息元数据和 data 变量替换。
     * 2. 输入参数：patterns 是模板列表，tbMsg 是变量来源消息。
     * 3. 返回值：替换后的字符串列表；输入为空时返回空列表。
     * 4. 调用时机：节点需要批量解析主题、键名、字段名或外部请求参数时调用。
     * 5. 调用方：具体规则节点实现。
     * 6. 使用流程：属于 Rule Engine 消息处理中的模板渲染流程。
     * 7. 线程安全：无共享可变状态，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine 消息处理。
     */
    public static List<String> processPatterns(List<String> patterns, TbMsg tbMsg) {
        if (!CollectionUtils.isEmpty(patterns)) {
            return patterns.stream().map(p -> processPattern(p, tbMsg)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 中文说明：
     * 1. 方法职责：对单个模板执行 `${metadata}` 和 `$[data.path]` 变量替换。
     * 2. 输入参数：pattern 是模板字符串，tbMsg 是元数据和 JSON data 的来源。
     * 3. 返回值：变量替换后的字符串。
     * 4. 调用时机：节点处理消息并需要动态生成配置值时调用。
     * 5. 调用方：外部集成节点、转换节点、元数据节点等。
     * 6. 使用流程：属于 Rule Engine 消息模板处理流程。
     * 7. 线程安全：方法只使用局部变量和不可变 Pattern，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine。
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
                    // 只支持以点分隔的对象路径，不解析数组表达式；这样可以保持模板语法简单并避免引入 JSONPath 依赖。
                    for (String key : keys) {
                        if (!StringUtils.isEmpty(key) && jsonNode != null) {
                            jsonNode = jsonNode.get(key);
                        } else {
                            jsonNode = null;
                            break;
                        }
                    }

                    if (jsonNode != null && jsonNode.isValueNode()) {
                        // 只有值节点才替换，复杂对象和数组保留原模板，避免把结构化数据隐式转换成不可控字符串。
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
     * 中文说明：
     * 1. 方法职责：对模板列表执行仅基于消息元数据的变量替换。
     * 2. 输入参数：patterns 是模板列表，metaData 是变量来源。
     * 3. 返回值：替换后的字符串列表；输入为空时返回空列表。
     * 4. 调用时机：旧版本节点只需要元数据变量时调用。
     * 5. 调用方：兼容旧代码的规则节点。
     * 6. 使用流程：属于 Rule Engine 模板处理兼容流程。
     * 7. 线程安全：无共享可变状态，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine。
     */
    @Deprecated(since = "3.6.1", forRemoval = true)
    public static List<String> processPatterns(List<String> patterns, TbMsgMetaData metaData) {
        if (!CollectionUtils.isEmpty(patterns)) {
            return patterns.stream().map(p -> processPattern(p, metaData)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 中文说明：
     * 1. 方法职责：对单个模板执行 `${metadataKey}` 变量替换。
     * 2. 输入参数：pattern 是模板字符串，metaData 是变量来源。
     * 3. 返回值：替换后的字符串。
     * 4. 调用时机：节点只依赖元数据模板时调用。
     * 5. 调用方：规则节点和兼容旧 API 的调用方。
     * 6. 使用流程：属于 Rule Engine 元数据模板处理流程。
     * 7. 线程安全：无共享可变状态，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine。
     */
    public static String processPattern(String pattern, TbMsgMetaData metaData) {
        return processTemplate(pattern, metaData.values());
    }

    /**
     * 中文说明：
     * 1. 方法职责：使用键值 Map 替换模板中的元数据变量。
     * 2. 输入参数：template 是模板字符串，data 是变量名到变量值的映射。
     * 3. 返回值：替换后的字符串。
     * 4. 调用时机：调用方已有变量 Map、无需 TbMsg 包装时调用。
     * 5. 调用方：规则节点工具逻辑和测试。
     * 6. 使用流程：属于 Rule Engine 模板渲染的底层流程。
     * 7. 线程安全：方法只使用局部变量，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；服务 Rule Engine。
     */
    public static String processTemplate(String template, Map<String, String> data) {
        String result = template;
        for (Map.Entry<String, String> kv : data.entrySet()) {
            result = processVar(result, kv.getKey(), kv.getValue());
        }
        return result;
    }

    /**
     * 中文说明：
     * 1. 方法职责：替换模板中的单个元数据变量。
     * 2. 输入参数：pattern 是当前模板，key 是变量名，val 是变量值。
     * 3. 返回值：替换指定变量后的字符串。
     * 4. 调用时机：processTemplate 遍历变量 Map 时调用。
     * 5. 调用方：本工具类内部方法。
     * 6. 使用流程：属于模板渲染的最小替换步骤。
     * 7. 线程安全：纯字符串操作，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；服务 Rule Engine 模板处理。
     */
    private static String processVar(String pattern, String key, String val) {
        return pattern.replace(formatMetadataVarTemplate(key), val);
    }

    /**
     * 中文说明：
     * 1. 方法职责：生成 data 变量模板字符串。
     * 2. 输入参数：key 是 data 中的字段路径。
     * 3. 返回值：`$[key]` 形式的模板。
     * 4. 调用时机：查找和替换 data 变量时调用。
     * 5. 调用方：本工具类和单元测试。
     * 6. 使用流程：属于 Rule Engine 模板语法格式化流程。
     * 7. 线程安全：纯字符串拼接，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；服务 Rule Engine 模板处理。
     */
    static String formatDataVarTemplate(String key) {
        return "$[" + key + ']';
    }

    /**
     * 中文说明：
     * 1. 方法职责：生成元数据变量模板字符串。
     * 2. 输入参数：key 是元数据键名。
     * 3. 返回值：`${key}` 形式的模板。
     * 4. 调用时机：替换元数据变量时调用。
     * 5. 调用方：本工具类和单元测试。
     * 6. 使用流程：属于 Rule Engine 模板语法格式化流程。
     * 7. 线程安全：纯字符串拼接，线程安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；服务 Rule Engine 模板处理。
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
