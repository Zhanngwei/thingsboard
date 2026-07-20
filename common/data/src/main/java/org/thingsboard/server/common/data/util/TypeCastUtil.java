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
package org.thingsboard.server.common.data.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.kv.DataType;

import java.math.BigDecimal;

/**
 * 中文说明：
 * 1. `TypeCastUtil` 是 ThingsBoard Common Data 中处理 `Type Cast Util` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class TypeCastUtil {

    /**
     * 功能：创建 `TypeCastUtil` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private TypeCastUtil() {}

    /**
     * 功能：执行 `castValue` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static Pair<DataType, Object> castValue(String value) {
        if (isNumber(value)) {
            String formattedValue = value.replace(',', '.');
            try {
                BigDecimal bd = new BigDecimal(formattedValue);
                if (bd.stripTrailingZeros().scale() > 0 || isSimpleDouble(formattedValue)) {
                    if (bd.scale() <= 16) {
                        return Pair.of(DataType.DOUBLE, bd.doubleValue());
                    }
                } else {
                    return Pair.of(DataType.LONG, bd.longValueExact());
                }
            } catch (RuntimeException ignored) {}
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Pair.of(DataType.BOOLEAN, Boolean.parseBoolean(value));
        }
        return Pair.of(DataType.STRING, value);
    }

    /**
     * 功能：执行 `castToNumber` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static Pair<DataType, Number> castToNumber(String value) {
        if (isNumber(value)) {
            String formattedValue = value.replace(',', '.');
            BigDecimal bd = new BigDecimal(formattedValue);
            if (bd.stripTrailingZeros().scale() > 0 || isSimpleDouble(formattedValue)) {
                if (bd.scale() <= 16) {
                    return Pair.of(DataType.DOUBLE, bd.doubleValue());
                } else {
                    return Pair.of(DataType.DOUBLE, bd);
                }
            } else {
                return Pair.of(DataType.LONG, bd.longValueExact());
            }
        } else {
            throw new IllegalArgumentException("'" + value + "' can't be parsed as number");
        }
    }

    /**
     * 功能：判断`Number`。
     * 参数：
     * - `value`：值。
     * 返回：判断结果。
     */
    private static boolean isNumber(String value) {
        return NumberUtils.isNumber(value.replace(',', '.'));
    }

    /**
     * 功能：判断`Simple Double`。
     * 参数：
     * - `valueAsString`：值。
     * 返回：判断结果。
     */
    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }

}
