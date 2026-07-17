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
package org.thingsboard.server.cache;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.cache.Cache;

/**
 * 中文说明：
 * 1. `SimpleTbCacheValueWrapper` 是 ThingsBoard Common Cache 中管理缓存缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `TbCacheValueWrapper`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleTbCacheValueWrapper<T> implements TbCacheValueWrapper<T> {

    /**
     * 值，保存当前处理得到的具体内容。
     */
    private final T value;

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public T get() {
        return value;
    }

    /**
     * 功能：执行 `empty` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static <T> SimpleTbCacheValueWrapper<T> empty() {
        return new SimpleTbCacheValueWrapper<>(null);
    }

    /**
     * 功能：执行 `wrap` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static <T> SimpleTbCacheValueWrapper<T> wrap(T value) {
        return new SimpleTbCacheValueWrapper<>(value);
    }

    /**
     * 功能：执行 `wrap` 对应的处理。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    public static <T> SimpleTbCacheValueWrapper<T> wrap(Cache.ValueWrapper source) {
        return source == null ? null : new SimpleTbCacheValueWrapper<>((T) source.get());
    }
}
