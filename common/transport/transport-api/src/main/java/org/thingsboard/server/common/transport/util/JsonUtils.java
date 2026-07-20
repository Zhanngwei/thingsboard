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
package org.thingsboard.server.common.transport.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;

import java.util.List;

/**
 * 中文说明：
 * 1. `JsonUtils` 是 ThingsBoard Common Transport 中处理 `Json Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class JsonUtils {

    private static final JsonParser jsonParser = new JsonParser();

    /**
     * 功能：获取JSON。
     * 参数：
     * - `tsKv`：数据列表。
     * 返回：处理结果。
     */
    public static JsonObject getJsonObject(List<KeyValueProto> tsKv) {
        JsonObject json = new JsonObject();
        for (KeyValueProto kv : tsKv) {
            switch (kv.getType()) {
                case BOOLEAN_V:
                    json.addProperty(kv.getKey(), kv.getBoolV());
                    break;
                case LONG_V:
                    json.addProperty(kv.getKey(), kv.getLongV());
                    break;
                case DOUBLE_V:
                    json.addProperty(kv.getKey(), kv.getDoubleV());
                    break;
                case STRING_V:
                    json.addProperty(kv.getKey(), kv.getStringV());
                    break;
                case JSON_V:
                    json.add(kv.getKey(), jsonParser.parse(kv.getJsonV()));
                    break;
            }
        }
        return json;
    }

    /**
     * 功能：执行 `parse` 对应的处理。
     * 参数：
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    public static JsonElement parse(String params) {
        return jsonParser.parse(params);
    }

}
