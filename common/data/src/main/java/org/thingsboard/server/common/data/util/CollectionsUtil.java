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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `CollectionsUtil` 是 ThingsBoard Common Data 中处理 `Collections Util` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class CollectionsUtil {
    /**
     * 功能：判断`Empty`。
     * 参数：
     * - `collection`：数据列表。
     * 返回：判断结果。
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 功能：判断`Not Empty`。
     * 参数：
     * - `collection`：数据列表。
     * 返回：判断结果。
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Returns new set with elements that are present in set B(new) but absent in set A(old).
     */
    /**
     * 功能：执行 `diffSets` 对应的处理。
     * 参数：
     * - `a`：`a` 参数。
     * - `b`：`b` 参数。
     * 返回：匹配的数据集合。
     */
    public static <T> Set<T> diffSets(Set<T> a, Set<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toSet());
    }

    /**
     * 功能：执行 `contains` 对应的处理。
     * 参数：
     * - `collection`：数据列表。
     * - `element`：`element` 参数。
     * 返回：判断结果。
     */
    public static <T> boolean contains(Collection<T> collection, T element) {
        return isNotEmpty(collection) && collection.contains(element);
    }

    /**
     * 功能：统计`Non Null`数量。
     * 参数：
     * - `array`：`array` 参数。
     * 返回：数值结果。
     */
    public static <T> int countNonNull(T[] array) {
        int count = 0;
        for (T t : array) {
            if (t != null) count++;
        }
        return count;
    }

    /**
     * 功能：转换`Of`。
     * 参数：
     * - `kvs`：`kvs` 参数。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<T, T> mapOf(T... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of parameters");
        }
        Map<T, T> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            T key = kvs[i];
            T value = kvs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    /**
     * 功能：执行 `emptyOrContains` 对应的处理。
     * 参数：
     * - `collection`：数据列表。
     * - `element`：`element` 参数。
     * 返回：判断结果。
     */
    public static <V> boolean emptyOrContains(Collection<V> collection, V element) {
        return isEmpty(collection) || collection.contains(element);
    }

}
