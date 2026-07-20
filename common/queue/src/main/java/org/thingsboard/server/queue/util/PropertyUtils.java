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
package org.thingsboard.server.queue.util;

import org.thingsboard.server.common.data.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 中文说明：
 * 1. `PropertyUtils` 是 ThingsBoard Common Queue 中处理 `Property Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class PropertyUtils {

    /**
     * 功能：获取`Props`。
     * 参数：
     * - `properties`：`properties` 参数。
     * 返回：处理结果。
     */
    public static Map<String, String> getProps(String properties) {
        Map<String, String> configs = new HashMap<>();
        if (StringUtils.isNotEmpty(properties)) {
            for (String property : properties.split(";")) {
                if (StringUtils.isNotEmpty(property)) {
                    int delimiterPosition = property.indexOf(":");
                    String key = property.substring(0, delimiterPosition);
                    String value = property.substring(delimiterPosition + 1);
                    configs.put(key, value);
                }
            }
        }
        return configs;
    }

    /**
     * 功能：获取`Props`。
     * 参数：
     * - `defaultProperties`：键值映射。
     * - `propertiesStr`：`propertiesStr` 参数。
     * 返回：处理结果。
     */
    public static Map<String, String> getProps(Map<String, String> defaultProperties, String propertiesStr) {
        return getProps(defaultProperties, propertiesStr, PropertyUtils::getProps);
    }

    /**
     * 功能：获取`Props`。
     * 参数：
     * - `defaultProperties`：键值映射。
     * - `propertiesStr`：`propertiesStr` 参数。
     * - `parser`：键值映射。
     * 返回：处理结果。
     */
    public static Map<String, String> getProps(Map<String, String> defaultProperties, String propertiesStr, Function<String, Map<String, String>> parser) {
        Map<String, String> properties = defaultProperties;
        if (StringUtils.isNotBlank(propertiesStr)) {
            properties = new HashMap<>(properties);
            properties.putAll(parser.apply(propertiesStr));
        }
        return properties;
    }

}
