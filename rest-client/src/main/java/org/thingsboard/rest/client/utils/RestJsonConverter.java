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
package org.thingsboard.rest.client.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `RestJsonConverter` 是 ThingsBoard REST Client 中转换 `Rest Json` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 它直接协作于源模型、目标模型和相关编解码类型。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
 */
public class RestJsonConverter {
    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String LAST_UPDATE_TS = "lastUpdateTs";
    /**
     * 时间戳常量，用于统一引用固定值。
     */
    private static final String TS = "ts";

    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";

    /**
     * 功能：执行 `toAttributes` 对应的处理。
     * 参数：
     * - `attributes`：数据列表。
     * 返回：匹配的数据集合。
     */
    public static List<AttributeKvEntry> toAttributes(List<JsonNode> attributes) {
        if (!CollectionUtils.isEmpty(attributes)) {
            return attributes.stream().map(attr -> {
                        KvEntry entry = parseValue(attr.get(KEY).asText(), attr.get(VALUE));
                        return new BaseAttributeKvEntry(entry, attr.get(LAST_UPDATE_TS).asLong());
                    }
            ).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 功能：执行 `toTimeseries` 对应的处理。
     * 参数：
     * - `timeseries`：数据列表。
     * 返回：匹配的数据集合。
     */
    public static List<TsKvEntry> toTimeseries(Map<String, List<JsonNode>> timeseries) {
        if (!CollectionUtils.isEmpty(timeseries)) {
            List<TsKvEntry> result = new ArrayList<>();
            timeseries.forEach((key, values) ->
                    result.addAll(values.stream().map(ts -> {
                                KvEntry entry = parseValue(key, ts.get(VALUE));
                                return new BasicTsKvEntry(ts.get(TS).asLong(), entry);
                            }
                    ).collect(Collectors.toList()))
            );
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 功能：解析值。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：处理结果。
     */
    private static KvEntry parseValue(String key, JsonNode value) {
        if (!value.isContainerNode()) {
            if (value.isBoolean()) {
                return new BooleanDataEntry(key, value.asBoolean());
            } else if (value.isNumber()) {
                return parseNumericValue(key, value);
            } else if (value.isTextual()) {
                return new StringDataEntry(key, value.asText());
            } else {
                throw new RuntimeException(CAN_T_PARSE_VALUE + value);
            }
        } else {
            return new JsonDataEntry(key, value.toString());
        }
    }

    /**
     * 功能：解析值。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：处理结果。
     */
    private static KvEntry parseNumericValue(String key, JsonNode value) {
        if (value.isFloatingPointNumber()) {
            return new DoubleDataEntry(key, value.asDouble());
        } else {
            try {
                long longValue = Long.parseLong(value.toString());
                return new LongDataEntry(key, longValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Big integer values are not supported!");
            }
        }
    }
}
