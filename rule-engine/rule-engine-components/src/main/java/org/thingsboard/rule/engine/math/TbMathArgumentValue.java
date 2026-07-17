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
package org.thingsboard.rule.engine.math;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `TbMathArgumentValue` 是 ThingsBoard Rule Engine Components 中围绕 `Tb Math Argument` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class TbMathArgumentValue {

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Getter
    private final double value;

    /**
     * 功能：创建 `TbMathArgumentValue` 实例，并初始化必要字段。
     * 参数：
     * - `value`：值。
     * 返回：新创建的对象实例。
     */
    private TbMathArgumentValue(double value) {
        this.value = value;
    }

    /**
     * 功能：执行 `constant` 对应的处理。
     * 参数：
     * - `arg`：`arg` 参数。
     * 返回：处理结果。
     */
    public static TbMathArgumentValue constant(TbMathArgument arg) {
        return fromString(arg.getKey());
    }

    /**
     * 功能：执行 `defaultOrThrow` 对应的处理。
     * 参数：
     * - `defaultValue`：值。
     * - `error`：错误信息。
     * 返回：处理结果。
     */
    private static TbMathArgumentValue defaultOrThrow(Double defaultValue, String error) {
        if (defaultValue != null) {
            return new TbMathArgumentValue(defaultValue);
        }
        throw new RuntimeException(error);
    }

    /**
     * 功能：执行 `fromMessageBody` 对应的处理。
     * 参数：
     * - `arg`：`arg` 参数。
     * - `argKey`：键。
     * - `jsonNodeOpt`：`jsonNodeOpt` 参数。
     * 返回：处理结果。
     */
    public static TbMathArgumentValue fromMessageBody(TbMathArgument arg, String argKey, Optional<ObjectNode> jsonNodeOpt) {
        Double defaultValue = arg.getDefaultValue();
        if (jsonNodeOpt.isEmpty()) {
            return defaultOrThrow(defaultValue, "Message body is empty!");
        }
        var json = jsonNodeOpt.get();
        if (!json.has(argKey)) {
            return defaultOrThrow(defaultValue, "Message body has no '" + argKey + "'!");
        }
        JsonNode valueNode = json.get(argKey);
        if (valueNode.isNull()) {
            return defaultOrThrow(defaultValue, "Message body has null '" + argKey + "'!");
        }
        double value;
        if (valueNode.isNumber()) {
            value = valueNode.doubleValue();
        } else if (valueNode.isTextual()) {
            var valueNodeText = valueNode.asText();
            if (StringUtils.isNotBlank(valueNodeText)) {
                try {
                    value = Double.parseDouble(valueNode.asText());
                } catch (NumberFormatException ne) {
                    throw new RuntimeException("Can't convert value '" + valueNode.asText() + "' to double!");
                }
            } else {
                return defaultOrThrow(defaultValue, "Message value is empty for '" + argKey + "'!");
            }
        } else {
            throw new RuntimeException("Can't convert value '" + valueNode.toString() + "' to double!");
        }
        return new TbMathArgumentValue(value);
    }

    /**
     * 功能：执行 `fromMessageMetadata` 对应的处理。
     * 参数：
     * - `arg`：`arg` 参数。
     * - `argKey`：键。
     * - `metaData`：待处理数据。
     * 返回：处理结果。
     */
    public static TbMathArgumentValue fromMessageMetadata(TbMathArgument arg, String argKey, TbMsgMetaData metaData) {
        Double defaultValue = arg.getDefaultValue();
        if (metaData == null) {
            return defaultOrThrow(defaultValue, "Message metadata is empty!");
        }
        var value = metaData.getValue(argKey);
        if (StringUtils.isEmpty(value)) {
            return defaultOrThrow(defaultValue, "Message metadata has no '" + argKey + "'!");
        }
        return fromString(value);
    }

    /**
     * 功能：执行 `fromLong` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static TbMathArgumentValue fromLong(long value) {
        return new TbMathArgumentValue(value);
    }

    /**
     * 功能：执行 `fromDouble` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static TbMathArgumentValue fromDouble(double value) {
        return new TbMathArgumentValue(value);
    }

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static TbMathArgumentValue fromString(String value) {
        try {
            return new TbMathArgumentValue(Double.parseDouble(value));
        } catch (NumberFormatException ne) {
            throw new RuntimeException("Can't convert value '" + value + "' to double!");
        }
    }
}
