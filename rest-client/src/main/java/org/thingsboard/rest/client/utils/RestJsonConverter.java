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
 * 1. 类目的：`RestJsonConverter` 是 ThingsBoard Rest Client 模块 中的REST JSON 到 KV 模型转换器，用于把 REST API 返回的属性和时序 JSON 转换为 ThingsBoard 内部 AttributeKvEntry、TsKvEntry 和 KvEntry 类型。
 * 2. 所属模块：位于 rest-client，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Jackson JsonNode、ThingsBoard KV 数据模型、RestClient 属性/时序查询方法。
 * 4. 生命周期：作为无状态工具类被静态调用，方法执行期间临时创建转换结果，调用结束即可释放局部对象。
 * 5. 存在原因：独立转换器让 RestClient 保持 HTTP 门面职责，不把 JSON 类型判断和 KV 构造细节混入大量 REST 方法。
 * 6. 事务：不涉及事务，只在内存中转换 REST 响应数据。
 * 7. 缓存：不涉及缓存，每次调用都重新转换输入 JSON。
 * 8. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
 * 9. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
 * 10. 数据库：不访问数据库，数据来源是服务端 REST 响应。
 * 11. Rule Engine：不执行 Rule Engine，只为可能来自遥测/属性查询的数据构造客户端模型。
 * 12. 设计模式：主要体现 Adapter / Converter。
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
    /**
     * 本类总结：
     * 1. 核心职责：`RestJsonConverter` 负责把 REST API 返回的属性和时序 JSON 转换为 ThingsBoard 内部 AttributeKvEntry、TsKvEntry 和 KvEntry 类型。
     * 2. 核心流程：读取 JSON 中的 key、value、ts 或 lastUpdateTs 字段，按布尔、数字、文本、JSON 容器类型构造对应 KV entry。
     * 3. 关键依赖：Jackson JsonNode、ThingsBoard KV 数据模型、RestClient 属性/时序查询方法。
     * 4. 设计重点：通过 Adapter / Converter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
