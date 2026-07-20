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

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.lang3.StringUtils.removeStart;

/**
 * 中文说明：
 * 1. `TemplateUtils` 是 ThingsBoard Common Data 中处理 `Template Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class TemplateUtils {

    private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("\\$\\{(.+?)(:[a-zA-Z]+)?}");

    private static final Map<String, UnaryOperator<String>> FUNCTIONS = Map.of(
            "upperCase", String::toUpperCase,
            "lowerCase", String::toLowerCase,
            "capitalize", StringUtils::capitalize
    );

    /**
     * 功能：创建 `TemplateUtils` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private TemplateUtils() {}

    /**
     * 功能：处理`Template`。
     * 参数：
     * - `template`：`template` 参数。
     * - `context`：处理上下文。
     * 返回：文本结果。
     */
    public static String processTemplate(String template, Map<String, String> context) {
        return TEMPLATE_PARAM_PATTERN.matcher(template).replaceAll(matchResult -> {
            String key = matchResult.group(1);
            if (!context.containsKey(key)) {
                return "\\" + matchResult.group();
            }
            String value = nullToEmpty(context.get(key));
            String function = removeStart(matchResult.group(2), ":");
            if (function != null) {
                if (FUNCTIONS.containsKey(function)) {
                    value = FUNCTIONS.get(function).apply(value);
                }
            }
            return Matcher.quoteReplacement(value);
        });
    }

}
