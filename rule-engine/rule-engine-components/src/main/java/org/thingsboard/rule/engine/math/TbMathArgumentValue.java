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
 * 中文说明：`TbMathArgumentValue` 是数学参数值辅助类，用于解析数学参数、计算结果并可写回消息、属性或时间序列。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class TbMathArgumentValue {

    @Getter
    /**
     * 字段说明：保存 `value`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
     */
    private final double value;

    /**
     * 方法说明：构造 `TbMathArgumentValue` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    private TbMathArgumentValue(double value) {
        this.value = value;
    }

    /**
     * 方法说明：执行 `constant` 对应的辅助逻辑，供 `TbMathArgumentValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public static TbMathArgumentValue constant(TbMathArgument arg) {
        return fromString(arg.getKey());
    }

    /**
     * 方法说明：执行 `defaultOrThrow` 对应的辅助逻辑，供 `TbMathArgumentValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static TbMathArgumentValue defaultOrThrow(Double defaultValue, String error) {
        if (defaultValue != null) {
            return new TbMathArgumentValue(defaultValue);
        }
        throw new RuntimeException(error);
    }

    /**
     * 方法说明：执行 `fromMessageBody` 对应的辅助逻辑，供 `TbMathArgumentValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：执行 `fromMessageMetadata` 对应的辅助逻辑，供 `TbMathArgumentValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：执行 `fromLong` 对应的辅助逻辑，供 `TbMathArgumentValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public static TbMathArgumentValue fromLong(long value) {
        return new TbMathArgumentValue(value);
    }

    /**
     * 方法说明：执行 `fromDouble` 对应的辅助逻辑，供 `TbMathArgumentValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public static TbMathArgumentValue fromDouble(double value) {
        return new TbMathArgumentValue(value);
    }

    /**
     * 方法说明：执行 `fromString` 对应的辅助逻辑，供 `TbMathArgumentValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public static TbMathArgumentValue fromString(String value) {
        try {
            return new TbMathArgumentValue(Double.parseDouble(value));
        } catch (NumberFormatException ne) {
            throw new RuntimeException("Can't convert value '" + value + "' to double!");
        }
    }
    /*
     * 本类总结：`TbMathArgumentValue` 负责解析数学参数、计算结果并可写回消息、属性或时间序列；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
