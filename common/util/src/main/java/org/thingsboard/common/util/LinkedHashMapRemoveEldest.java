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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * LinkedHashMap that removed eldest entries (by insert order)
 * It guaranteed that size is not greater then maxEntries parameter. And remove time is constant O(1).
 * Example:
 *   LinkedHashMapRemoveEldest<Long, String> map =
 *                 new LinkedHashMapRemoveEldest<>(MAX_ENTRIES, this::removeConsumer);
 * */
/**
 * 中文说明：
 * 1. `LinkedHashMapRemoveEldest` 是 ThingsBoard Common 中围绕 `Linked Hash Map` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `LinkedHashMap`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LinkedHashMapRemoveEldest<K, V> extends LinkedHashMap<K, V> {
    /**
     * `maxEntries` 字段，保存当前对象的对应属性。
     */
    final long maxEntries;
    final BiConsumer<K, V> removalConsumer;

    /**
     * 功能：创建 `LinkedHashMapRemoveEldest` 实例，并初始化必要字段。
     * 参数：
     * - `maxEntries`：`maxEntries` 参数。
     * - `removalConsumer`：`removalConsumer` 参数。
     * 返回：新创建的对象实例。
     */
    public LinkedHashMapRemoveEldest(long maxEntries, BiConsumer<K, V> removalConsumer) {
        this.maxEntries = maxEntries;
        this.removalConsumer = removalConsumer;
    }

    /**
     * 功能：删除或清理`Eldest Entry`。
     * 参数：
     * - `eldest`：键值映射。
     * 返回：判断结果。
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() <= maxEntries) {
            return false;
        }
        removalConsumer.accept(eldest.getKey(), eldest.getValue());
        return true;
    }
}
