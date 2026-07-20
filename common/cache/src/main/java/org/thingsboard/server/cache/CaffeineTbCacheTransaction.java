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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `CaffeineTbCacheTransaction` 是 ThingsBoard Common Cache 中管理缓存缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`、`TbCacheTransaction`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Slf4j
@RequiredArgsConstructor
public class CaffeineTbCacheTransaction<K extends Serializable, V extends Serializable> implements TbCacheTransaction<K, V> {
    @Getter
    private final UUID id = UUID.randomUUID();
    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private final CaffeineTbTransactionalCache<K, V> cache;
    /**
     * `keys`列表，用于保存一组待处理对象。
     */
    @Getter
    private final List<K> keys;
    /**
     * 当前操作是否失败。
     */
    @Getter
    @Setter
    private boolean failed;

    private final Map<Object, Object> pendingPuts = new LinkedHashMap<>();

    /**
     * 功能：执行 `putIfAbsent` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void putIfAbsent(K key, V value) {
        pendingPuts.put(key, value);
    }

    /**
     * 功能：执行 `commit` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean commit() {
        return cache.commit(id, pendingPuts);
    }

    /**
     * 功能：执行 `rollback` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void rollback() {
        cache.rollback(id);
    }


}
