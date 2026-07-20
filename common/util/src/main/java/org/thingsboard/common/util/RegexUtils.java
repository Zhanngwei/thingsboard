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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `RegexUtils` 是 ThingsBoard Common 中处理 `Regex Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegexUtils {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");


    /**
     * 功能：执行 `replace` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * - `pattern`：`pattern` 参数。
     * - `replacer`：`replacer` 参数。
     * 返回：文本结果。
     */
    public static String replace(String s, Pattern pattern, UnaryOperator<String> replacer) {
        return pattern.matcher(s).replaceAll(matchResult -> {
            return replacer.apply(matchResult.group());
        });
    }

    /**
     * 功能：执行 `matches` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * - `pattern`：`pattern` 参数。
     * 返回：判断结果。
     */
    public static boolean matches(String input, Pattern pattern) {
        return pattern.matcher(input).matches();
    }

    /**
     * 功能：获取`Match`。
     * 参数：
     * - `input`：`input` 参数。
     * - `pattern`：`pattern` 参数。
     * - `group`：`group` 参数。
     * 返回：文本结果。
     */
    public static String getMatch(String input, Pattern pattern, int group) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            try {
                return matcher.group(group);
            } catch (Exception ignored) {}
        }
        return null;
    }

}
