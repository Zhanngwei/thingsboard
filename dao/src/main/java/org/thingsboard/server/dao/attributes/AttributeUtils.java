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
package org.thingsboard.server.dao.attributes;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.util.KvUtils;

import java.util.List;

/**
 * 中文说明：
 * 1. `AttributeUtils` 是 ThingsBoard DAO 中处理属性通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class AttributeUtils {

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `scope`：`scope` 参数。
     * 返回：无。
     */
    public static void validate(EntityId id, String scope) {
        Validator.validateId(id.getId(), "Incorrect id " + id);
        Validator.validateString(scope, "Incorrect scope " + scope);
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `kvEntries`：数据列表。
     * - `valueNoXssValidation`：值。
     * 返回：无。
     */
    public static void validate(List<AttributeKvEntry> kvEntries,  boolean valueNoXssValidation) {
        kvEntries.forEach(tsKvEntry -> validate(tsKvEntry, valueNoXssValidation));
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `kvEntry`：`kvEntry` 参数。
     * - `valueNoXssValidation`：值。
     * 返回：无。
     */
    public static void validate(AttributeKvEntry kvEntry, boolean valueNoXssValidation) {
        KvUtils.validate(kvEntry, valueNoXssValidation);
        if (kvEntry.getDataType() == null) {
            throw new IncorrectParameterException("Incorrect kvEntry. Data type can't be null");
        } else {
            Validator.validateString(kvEntry.getKey(), "Incorrect kvEntry. Key can't be empty");
            Validator.validatePositiveNumber(kvEntry.getLastUpdateTs(), "Incorrect last update ts. Ts should be positive");
        }
    }
}
