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
package org.thingsboard.server.dao.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.NoXssValidator;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `KvUtils` 是 ThingsBoard DAO 中处理 `Kv Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class KvUtils {

    /**
     * `validatedKeys`常量，用于统一引用固定值。
     */
    private static final Cache<String, Boolean> validatedKeys;

    static {
        validatedKeys = Caffeine.newBuilder()
                .weakKeys()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(100000).build();
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `tsKvEntries`：数据列表。
     * - `valueNoXssValidation`：值。
     * 返回：无。
     */
    public static void validate(List<? extends KvEntry> tsKvEntries, boolean valueNoXssValidation) {
        tsKvEntries.forEach(tsKvEntry -> validate(tsKvEntry, valueNoXssValidation));
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * - `valueNoXssValidation`：值。
     * 返回：无。
     */
    public static void validate(KvEntry tsKvEntry, boolean valueNoXssValidation) {
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }

        String key = tsKvEntry.getKey();

        if (StringUtils.isBlank(key)) {
            throw new DataValidationException("Key can't be null or empty");
        }

        if (key.length() > 255) {
            throw new DataValidationException("Validation error: key length must be equal or less than 255");
        }

        if (validatedKeys.getIfPresent(key) == null) {
            if (!NoXssValidator.isValid(key)) {
                throw new DataValidationException("Validation error: key is malformed");
            }
            validatedKeys.put(key, Boolean.TRUE);
        }

        if (valueNoXssValidation) {
            Object value = tsKvEntry.getValue();
            if (value instanceof CharSequence || value instanceof JsonNode) {
                if (!NoXssValidator.isValid(value.toString())) {
                    throw new DataValidationException("Validation error: value is malformed");
                }
            }
        }
    }
}
