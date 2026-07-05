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
package org.thingsboard.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Created by Valerii Sosliuk on 5/12/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JacksonUtil` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public class JacksonUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final ObjectMapper PRETTY_SORTED_JSON_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .build();
    public static ObjectMapper ALLOW_UNQUOTED_FIELD_NAMES_MAPPER = JsonMapper.builder()
            .configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .build();
    public static final ObjectMapper IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：键值映射结果。
     */
    public static ObjectMapper getObjectMapperWithJavaTimeModule() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * 功能：转换值。
     * 参数：
     * - `fromValue`：值。
     * - `toValueType`：类型。
     * 返回：处理结果。
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueType) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The given object value cannot be converted to " + toValueType + ": " + fromValue, e);
        }
    }

    /**
     * 功能：转换值。
     * 参数：
     * - `fromValue`：值。
     * - `toValueTypeRef`：类型。
     * 返回：处理结果。
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The given object value cannot be converted to " + toValueTypeRef + ": " + fromValue, e);
        }
    }

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    public static <T> T fromString(String string, Class<T> clazz) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value cannot be transformed to Json object: " + string, e);
        }
    }

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `valueTypeRef`：类型。
     * 返回：处理结果。
     */
    public static <T> T fromString(String string, TypeReference<T> valueTypeRef) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, valueTypeRef) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value cannot be transformed to Json object: " + string, e);
        }
    }

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `javaType`：类型。
     * 返回：处理结果。
     */
    public static <T> T fromString(String string, JavaType javaType) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, javaType) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given String value cannot be transformed to Json object: " + string, e);
        }
    }

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `clazz`：`clazz` 参数。
     * - `ignoreUnknownFields`：`ignoreUnknownFields` 参数。
     * 返回：处理结果。
     */
    public static <T> T fromString(String string, Class<T> clazz, boolean ignoreUnknownFields) {
        try {
            return string != null ? IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.readValue(string, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value cannot be transformed to Json object: " + string, e);
        }
    }

    /**
     * 功能：执行 `fromBytes` 对应的处理。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        try {
            return bytes != null ? OBJECT_MAPPER.readValue(bytes, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value cannot be transformed to Json object: " + Arrays.toString(bytes), e);
        }
    }

    /**
     * 功能：执行 `fromBytes` 对应的处理。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * 返回：处理结果。
     */
    public static JsonNode fromBytes(byte[] bytes) {
        try {
            return OBJECT_MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given byte[] value cannot be transformed to Json object: " + Arrays.toString(bytes), e);
        }
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：
     * - `value`：值。
     * 返回：文本结果。
     */
    public static String toString(Object value) {
        try {
            return value != null ? OBJECT_MAPPER.writeValueAsString(value) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value cannot be transformed to a String: " + value, e);
        }
    }

    /**
     * 功能：执行 `toPrettyString` 对应的处理。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：文本结果。
     */
    public static String toPrettyString(Object o) {
        try {
            return PRETTY_SORTED_JSON_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `treeToValue` 对应的处理。
     * 参数：
     * - `node`：`node` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    public static <T> T treeToValue(JsonNode node, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't convert value: " + node.toString(), e);
        }
    }

    /**
     * 功能：执行 `toJsonNode` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static JsonNode toJsonNode(String value) {
        return toJsonNode(value, OBJECT_MAPPER);
    }

    /**
     * 功能：执行 `toJsonNode` 对应的处理。
     * 参数：
     * - `value`：值。
     * - `mapper`：键值映射。
     * 返回：处理结果。
     */
    public static JsonNode toJsonNode(String value, ObjectMapper mapper) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return mapper.readTree(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 功能：执行 `toJsonNode` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static JsonNode toJsonNode(File value) {
        try {
            return value != null ? OBJECT_MAPPER.readTree(value) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given File object value: "
                    + value + " cannot be transformed to a JsonNode", e);
        }
    }

    /**
     * 功能：执行 `newObjectNode` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static ObjectNode newObjectNode() {
        return newObjectNode(OBJECT_MAPPER);
    }

    /**
     * 功能：执行 `newObjectNode` 对应的处理。
     * 参数：
     * - `mapper`：键值映射。
     * 返回：处理结果。
     */
    public static ObjectNode newObjectNode(ObjectMapper mapper) {
        return mapper.createObjectNode();
    }

    /**
     * 功能：执行 `newArrayNode` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static ArrayNode newArrayNode() {
        return newArrayNode(OBJECT_MAPPER);
    }

    /**
     * 功能：执行 `newArrayNode` 对应的处理。
     * 参数：
     * - `mapper`：键值映射。
     * 返回：处理结果。
     */
    public static ArrayNode newArrayNode(ObjectMapper mapper) {
        return mapper.createArrayNode();
    }

    /**
     * 功能：执行 `clone` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static <T> T clone(T value) {
        @SuppressWarnings("unchecked")
        Class<T> valueClass = (Class<T>) value.getClass();
        return fromString(toString(value), valueClass);
    }

    /**
     * 功能：执行 `valueToTree` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static <T> JsonNode valueToTree(T value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 功能：执行 `writeValueAsBytes` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static <T> byte[] writeValueAsBytes(T value) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value cannot be transformed to a String: " + value, e);
        }
    }


    /**
     * 功能：获取`Safely`。
     * 参数：
     * - `node`：`node` 参数。
     * - `path`：文件或资源路径。
     * 返回：处理结果。
     */
    public static JsonNode getSafely(JsonNode node, String... path) {
        if (node == null) {
            return null;
        }
        for (String p : path) {
            if (!node.has(p)) {
                return null;
            } else {
                node = node.get(p);
            }
        }
        return node;
    }

    /**
     * 功能：执行 `asObject` 对应的处理。
     * 参数：
     * - `node`：`node` 参数。
     * 返回：处理结果。
     */
    public static ObjectNode asObject(JsonNode node) {
        return node != null && node.isObject() ? ((ObjectNode) node) : newObjectNode();
    }

    /**
     * 功能：执行 `replaceUuidsRecursively` 对应的处理。
     * 参数：
     * - `node`：`node` 参数。
     * - `skippedRootFields`：`skippedRootFields` 参数。
     * - `includedFieldsPattern`：`includedFieldsPattern` 参数。
     * - `replacer`：`replacer` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    public static void replaceUuidsRecursively(JsonNode node, Set<String> skippedRootFields, Pattern includedFieldsPattern, UnaryOperator<UUID> replacer, boolean root) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = Lists.newArrayList(objectNode.fieldNames());
            for (String fieldName : fieldNames) {
                if (root && skippedRootFields.contains(fieldName)) {
                    continue;
                }
                var child = objectNode.get(fieldName);
                if (child.isObject() || child.isArray()) {
                    replaceUuidsRecursively(child, skippedRootFields, includedFieldsPattern, replacer, false);
                } else if (child.isTextual()) {
                    if (includedFieldsPattern != null && !RegexUtils.matches(fieldName, includedFieldsPattern)) {
                        continue;
                    }
                    String text = child.asText();
                    String newText = RegexUtils.replace(text, RegexUtils.UUID_PATTERN, uuid -> replacer.apply(UUID.fromString(uuid)).toString());
                    if (!text.equals(newText)) {
                        objectNode.put(fieldName, newText);
                    }
                }
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                JsonNode arrayElement = array.get(i);
                if (arrayElement.isObject() || arrayElement.isArray()) {
                    replaceUuidsRecursively(arrayElement, skippedRootFields, includedFieldsPattern, replacer, false);
                } else if (arrayElement.isTextual()) {
                    String text = arrayElement.asText();
                    String newText = RegexUtils.replace(text, RegexUtils.UUID_PATTERN, uuid -> replacer.apply(UUID.fromString(uuid)).toString());
                    if (!text.equals(newText)) {
                        array.set(i, newText);
                    }
                }
            }
        }
    }

    /**
     * 功能：执行 `toFlatMap` 对应的处理。
     * 参数：
     * - `node`：`node` 参数。
     * 返回：处理结果。
     */
    public static Map<String, String> toFlatMap(JsonNode node) {
        HashMap<String, String> map = new HashMap<>();
        toFlatMap(node, "", map);
        return map;
    }

    /**
     * 功能：执行 `fromReader` 对应的处理。
     * 参数：
     * - `reader`：`reader` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    public static <T> T fromReader(Reader reader, Class<T> clazz) {
        try {
            return reader != null ? OBJECT_MAPPER.readValue(reader, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid request payload", e);
        }
    }

    /**
     * 功能：执行 `writeValue` 对应的处理。
     * 参数：
     * - `writer`：`writer` 参数。
     * - `value`：值。
     * 返回：无。
     */
    public static <T> void writeValue(Writer writer, T value) {
        try {
            OBJECT_MAPPER.writeValue(writer, value);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given writer value: "
                    + writer + "cannot be wrote", e);
        }
    }

    /**
     * 功能：执行 `constructCollectionType` 对应的处理。
     * 参数：
     * - `collectionClass`：`collectionClass` 参数。
     * - `elementClass`：`elementClass` 参数。
     * 返回：处理结果。
     */
    public static JavaType constructCollectionType(Class collectionClass, Class<?> elementClass) {
        return OBJECT_MAPPER.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }

    /**
     * 功能：执行 `toFlatMap` 对应的处理。
     * 参数：
     * - `node`：`node` 参数。
     * - `currentPath`：文件或资源路径。
     * - `map`：键值映射。
     * 返回：无。
     */
    private static void toFlatMap(JsonNode node, String currentPath, Map<String, String> map) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            currentPath = currentPath.isEmpty() ? "" : currentPath + ".";
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                toFlatMap(entry.getValue(), currentPath + entry.getKey(), map);
            }
        } else if (node.isValueNode()) {
            map.put(currentPath, node.asText());
        }
    }

    /**
     * 功能：保存或创建`Kv Entry`。
     * 参数：
     * - `entityNode`：实体对象。
     * - `kvEntry`：`kvEntry` 参数。
     * 返回：无。
     */
    public static void addKvEntry(ObjectNode entityNode, KvEntry kvEntry) {
        addKvEntry(entityNode, kvEntry, kvEntry.getKey());
    }

    /**
     * 功能：保存或创建`Kv Entry`。
     * 参数：
     * - `entityNode`：实体对象。
     * - `kvEntry`：`kvEntry` 参数。
     * - `key`：键。
     * 返回：无。
     */
    public static void addKvEntry(ObjectNode entityNode, KvEntry kvEntry, String key) {
        addKvEntry(entityNode, kvEntry, key, OBJECT_MAPPER);
    }

    /**
     * 功能：保存或创建`Kv Entry`。
     * 参数：
     * - `entityNode`：实体对象。
     * - `kvEntry`：`kvEntry` 参数。
     * - `key`：键。
     * - `mapper`：键值映射。
     * 返回：无。
     */
    public static void addKvEntry(ObjectNode entityNode, KvEntry kvEntry, String key, ObjectMapper mapper) {
        if (kvEntry.getDataType() == DataType.BOOLEAN) {
            kvEntry.getBooleanValue().ifPresent(value -> entityNode.put(key, value));
        } else if (kvEntry.getDataType() == DataType.DOUBLE) {
            kvEntry.getDoubleValue().ifPresent(value -> entityNode.put(key, value));
        } else if (kvEntry.getDataType() == DataType.LONG) {
            kvEntry.getLongValue().ifPresent(value -> entityNode.put(key, value));
        } else if (kvEntry.getDataType() == DataType.JSON) {
            if (kvEntry.getJsonValue().isPresent()) {
                entityNode.set(key, toJsonNode(kvEntry.getJsonValue().get(), mapper));
            }
        } else {
            entityNode.put(key, kvEntry.getValueAsString());
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JacksonUtil` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
