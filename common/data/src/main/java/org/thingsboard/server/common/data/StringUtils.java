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
package org.thingsboard.server.common.data;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.repeat;

/**
 * 中文说明：
 * 1. 类目的：`StringUtils` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public class StringUtils {

    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final int DEFAULT_TOKEN_LENGTH = 8;

    public static final SecureRandom RANDOM = new SecureRandom();

    /**
     * `EMPTY`常量，用于统一引用固定值。
     */
    public static final String EMPTY = "";

    /**
     * 索引常量，用于统一引用固定值。
     */
    public static final int INDEX_NOT_FOUND = -1;

    /**
     * 功能：判断`Empty`。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：判断结果。
     */
    public static boolean isEmpty(String source) {
        return source == null || source.isEmpty();
    }

    /**
     * 功能：判断`Blank`。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：判断结果。
     */
    public static boolean isBlank(String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
    }

    /**
     * 功能：判断`Not Empty`。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：判断结果。
     */
    public static boolean isNotEmpty(String source) {
        return source != null && !source.isEmpty();
    }

    /**
     * 功能：判断`Not Blank`。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：判断结果。
     */
    public static boolean isNotBlank(String source) {
        return source != null && !source.isEmpty() && !source.trim().isEmpty();
    }

    /**
     * 功能：执行 `notBlankOrDefault` 对应的处理。
     * 参数：
     * - `src`：`src` 参数。
     * - `def`：`def` 参数。
     * 返回：文本结果。
     */
    public static String notBlankOrDefault(String src, String def) {
        return isNotBlank(src) ? src : def;
    }

    /**
     * 功能：删除或清理`Start`。
     * 参数：
     * - `str`：`str` 参数。
     * - `remove`：`remove` 参数。
     * 返回：文本结果。
     */
    public static String removeStart(final String str, final String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.startsWith(remove)) {
            return str.substring(remove.length());
        }
        return str;
    }

    /**
     * 功能：执行 `substringBefore` 对应的处理。
     * 参数：
     * - `str`：`str` 参数。
     * - `separator`：`separator` 参数。
     * 返回：文本结果。
     */
    public static String substringBefore(final String str, final String separator) {
        if (isEmpty(str) || separator == null) {
            return str;
        }
        if (separator.isEmpty()) {
            return EMPTY;
        }
        final int pos = str.indexOf(separator);
        if (pos == INDEX_NOT_FOUND) {
            return str;
        }
        return str.substring(0, pos);
    }

    /**
     * 功能：执行 `substringBetween` 对应的处理。
     * 参数：
     * - `str`：`str` 参数。
     * - `open`：`open` 参数。
     * - `close`：`close` 参数。
     * 返回：文本结果。
     */
    public static String substringBetween(final String str, final String open, final String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        final int start = str.indexOf(open);
        if (start != INDEX_NOT_FOUND) {
            final int end = str.indexOf(close, start + open.length());
            if (end != INDEX_NOT_FOUND) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    /**
     * 功能：执行 `obfuscate` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * - `seenMargin`：`seenMargin` 参数。
     * - `obfuscationChar`：`obfuscationChar` 参数。
     * - `startIndexInclusive`：`startIndexInclusive` 参数。
     * - 其余参数：补充处理条件。
     * 返回：文本结果。
     */
    public static String obfuscate(String input, int seenMargin, char obfuscationChar,
                                   int startIndexInclusive, int endIndexExclusive) {

        String part = input.substring(startIndexInclusive, endIndexExclusive);
        String obfuscatedPart;
        if (part.length() <= seenMargin * 2) {
            obfuscatedPart = repeat(obfuscationChar, part.length());
        } else {
            obfuscatedPart = part.substring(0, seenMargin)
                    + repeat(obfuscationChar, part.length() - seenMargin * 2)
                    + part.substring(part.length() - seenMargin);
        }
        return input.substring(0, startIndexInclusive) + obfuscatedPart + input.substring(endIndexExclusive);
    }

    /**
     * 功能：执行 `split` 对应的处理。
     * 参数：
     * - `value`：值。
     * - `maxPartSize`：`maxPartSize` 参数。
     * 返回：匹配的数据集合。
     */
    public static Iterable<String> split(String value, int maxPartSize) {
        return Splitter.fixedLength(maxPartSize).split(value);
    }

    /**
     * 功能：执行 `equalsIgnoreCase` 对应的处理。
     * 参数：
     * - `str1`：`str1` 参数。
     * - `str2`：`str2` 参数。
     * 返回：判断结果。
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }

    /**
     * 功能：执行 `join` 对应的处理。
     * 参数：
     * - `keyArray`：键。
     * - `lwm2mSeparatorPath`：文件或资源路径。
     * 返回：文本结果。
     */
    public static String join(String[] keyArray, String lwm2mSeparatorPath) {
        return org.apache.commons.lang3.StringUtils.join(keyArray, lwm2mSeparatorPath);
    }

    /**
     * 功能：执行 `trimToNull` 对应的处理。
     * 参数：
     * - `toString`：`toString` 参数。
     * 返回：文本结果。
     */
    public static String trimToNull(String toString) {
        return org.apache.commons.lang3.StringUtils.trimToNull(toString);
    }

    /**
     * 功能：判断`None Empty`。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：判断结果。
     */
    public static boolean isNoneEmpty(String str) {
        return org.apache.commons.lang3.StringUtils.isNoneEmpty(str);
    }

    /**
     * 功能：执行 `endsWith` 对应的处理。
     * 参数：
     * - `str`：`str` 参数。
     * - `suffix`：`suffix` 参数。
     * 返回：判断结果。
     */
    public static boolean endsWith(String str, String suffix) {
        return org.apache.commons.lang3.StringUtils.endsWith(str, suffix);
    }

    /**
     * 功能：判断`Length`。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：判断结果。
     */
    public static boolean hasLength(String str) {
        return org.springframework.util.StringUtils.hasLength(str);
    }

    /**
     * 功能：判断`None Blank`。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：判断结果。
     */
    public static boolean isNoneBlank(String... str) {
        return org.apache.commons.lang3.StringUtils.isNoneBlank(str);
    }

    /**
     * 功能：判断`Text`。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：判断结果。
     */
    public static boolean hasText(String str) {
        return org.springframework.util.StringUtils.hasText(str);
    }

    /**
     * 功能：执行 `defaultString` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * - `defaultValue`：值。
     * 返回：文本结果。
     */
    public static String defaultString(String s, String defaultValue) {
        return org.apache.commons.lang3.StringUtils.defaultString(s, defaultValue);
    }

    /**
     * 功能：判断`Numeric`。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：判断结果。
     */
    public static boolean isNumeric(String str) {
        return org.apache.commons.lang3.StringUtils.isNumeric(str);
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `str1`：`str1` 参数。
     * - `str2`：`str2` 参数。
     * 返回：判断结果。
     */
    public static boolean equals(String str1, String str2) {
        return org.apache.commons.lang3.StringUtils.equals(str1, str2);
    }

    /**
     * 功能：执行 `equalsAny` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `otherStrings`：`otherStrings` 参数。
     * 返回：判断结果。
     */
    public static boolean equalsAny(String string, String... otherStrings) {
        return equalsAny(string, Arrays.asList(otherStrings));
    }

    /**
     * 功能：执行 `equalsAny` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `otherStrings`：数据列表。
     * 返回：判断结果。
     */
    public static boolean equalsAny(String string, List<String> otherStrings) {
        for (String otherString : otherStrings) {
            if (equals(string, otherString)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：执行 `equalsAnyIgnoreCase` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `otherStrings`：`otherStrings` 参数。
     * 返回：判断结果。
     */
    public static boolean equalsAnyIgnoreCase(String string, String... otherStrings) {
        for (String otherString : otherStrings) {
            if (equalsIgnoreCase(string, otherString)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：执行 `substringBeforeLast` 对应的处理。
     * 参数：
     * - `str`：`str` 参数。
     * - `separator`：`separator` 参数。
     * 返回：文本结果。
     */
    public static String substringBeforeLast(String str, String separator) {
        return org.apache.commons.lang3.StringUtils.substringBeforeLast(str, separator);
    }

    /**
     * 功能：执行 `substringAfterLast` 对应的处理。
     * 参数：
     * - `str`：`str` 参数。
     * - `sep`：`sep` 参数。
     * 返回：文本结果。
     */
    public static String substringAfterLast(String str, String sep) {
        return org.apache.commons.lang3.StringUtils.substringAfterLast(str, sep);
    }

    /**
     * 功能：执行 `containedByAny` 对应的处理。
     * 参数：
     * - `searchString`：`searchString` 参数。
     * - `strings`：`strings` 参数。
     * 返回：判断结果。
     */
    public static boolean containedByAny(String searchString, String... strings) {
        if (searchString == null) return false;
        for (String string : strings) {
            if (string != null && string.contains(searchString)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：执行 `contains` 对应的处理。
     * 参数：
     * - `seq`：`seq` 参数。
     * - `searchSeq`：`searchSeq` 参数。
     * 返回：判断结果。
     */
    public static boolean contains(final CharSequence seq, final CharSequence searchSeq) {
        return org.apache.commons.lang3.StringUtils.contains(seq, searchSeq);
    }

    /**
     * Use this to prevent org.postgresql.util.PSQLException: ERROR: invalid byte sequence for encoding "UTF8": 0x00
     **/
    /**
     * 功能：执行 `contains0x00` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：判断结果。
     */
    public static boolean contains0x00(final String s) {
        return s != null && s.contains("\u0000");
    }

    /**
     * 功能：执行 `randomNumeric` 对应的处理。
     * 参数：
     * - `length`：`length` 参数。
     * 返回：文本结果。
     */
    public static String randomNumeric(int length) {
        return RandomStringUtils.randomNumeric(length);
    }

    /**
     * 功能：执行 `random` 对应的处理。
     * 参数：
     * - `length`：`length` 参数。
     * 返回：文本结果。
     */
    public static String random(int length) {
        return RandomStringUtils.random(length);
    }

    /**
     * 功能：执行 `random` 对应的处理。
     * 参数：
     * - `length`：`length` 参数。
     * - `chars`：`chars` 参数。
     * 返回：文本结果。
     */
    public static String random(int length, String chars) {
        return RandomStringUtils.random(length, chars);
    }

    /**
     * 功能：执行 `randomAlphanumeric` 对应的处理。
     * 参数：
     * - `count`：`count` 参数。
     * 返回：文本结果。
     */
    public static String randomAlphanumeric(int count) {
        return RandomStringUtils.randomAlphanumeric(count);
    }

    /**
     * 功能：执行 `randomAlphabetic` 对应的处理。
     * 参数：
     * - `count`：`count` 参数。
     * 返回：文本结果。
     */
    public static String randomAlphabetic(int count) {
        return RandomStringUtils.randomAlphabetic(count);
    }

    /**
     * 功能：执行 `generateSafeToken` 对应的处理。
     * 参数：
     * - `length`：`length` 参数。
     * 返回：文本结果。
     */
    public static String generateSafeToken(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

    /**
     * 功能：执行 `generateSafeToken` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String generateSafeToken() {
        return generateSafeToken(DEFAULT_TOKEN_LENGTH);
    }

    /**
     * 功能：执行 `truncate` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `maxLength`：`maxLength` 参数。
     * 返回：文本结果。
     */
    public static String truncate(String string, int maxLength) {
        return truncate(string, maxLength, n -> "...[truncated " + n + " symbols]");
    }

    /**
     * 功能：执行 `truncate` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * - `maxLength`：`maxLength` 参数。
     * - `truncationMarkerFunc`：`truncationMarkerFunc` 参数。
     * 返回：文本结果。
     */
    public static String truncate(String string, int maxLength, Function<Integer, String> truncationMarkerFunc) {
        if (string == null || maxLength <= 0 || string.length() <= maxLength) {
            return string;
        }
        int truncatedSymbols = string.length() - maxLength;
        return string.substring(0, maxLength) + truncationMarkerFunc.apply(truncatedSymbols);
    }

    /**
     * 功能：执行 `splitByCommaWithoutQuotes` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：匹配的数据集合。
     */
    public static List<String> splitByCommaWithoutQuotes(String value) {
        List<String> splitValues = List.of(value.trim().split("\\s*,\\s*"));
        List<String> result = new ArrayList<>();
        char lastWayInputValue = '#';
        for (String str : splitValues) {
            char startWith = str.charAt(0);
            char endWith = str.charAt(str.length() - 1);

            // if first value is not quote, so we return values after split
            if (startWith != '\'' && startWith != '"') return splitValues;

            // if value is not in quote, so we return values after split
            if (startWith != endWith) return splitValues;

            // if different way values, so don't replace quote and return values after split
            if (lastWayInputValue != '#' && startWith != lastWayInputValue) return splitValues;

            result.add(str.substring(1, str.length() - 1));
            lastWayInputValue = startWith;
        }
        return result;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`StringUtils` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
