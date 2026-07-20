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
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.databind.JsonNode;
import org.mvel2.ExecutionContext;
import org.mvel2.util.ArgsRepackUtil;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：
 * 1. `TbJson` 是 ThingsBoard Common 中围绕 `Tb Json` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class TbJson {

    /**
     * 功能：执行 `stringify` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：文本结果。
     */
    public static String stringify(Object value) {
        return value != null ? JacksonUtil.toString(value) : "null";
    }

    /**
     * 功能：执行 `parse` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `value`：值。
     * 返回：处理结果。
     */
    public static Object parse(ExecutionContext ctx, String value) throws IOException {
        if (value != null) {
            JsonNode node = JacksonUtil.toJsonNode(value);
            if (node.isObject()) {
                return ArgsRepackUtil.repack(ctx, JacksonUtil.convertValue(node, Map.class));
            } else if (node.isArray()) {
                return ArgsRepackUtil.repack(ctx, JacksonUtil.convertValue(node, List.class));
            } else if (node.isDouble()) {
                return node.doubleValue();
            } else if (node.isLong()) {
                return node.longValue();
            } else if (node.isInt()) {
                return node.intValue();
            } else if (node.isBoolean()) {
                return node.booleanValue();
            } else if (node.isTextual()) {
                return node.asText();
            } else if (node.isBinary()) {
                return node.binaryValue();
            } else if (node.isNull()) {
                return null;
            } else {
                return node.asText();
            }
        } else {
            return null;
        }
    }
}
