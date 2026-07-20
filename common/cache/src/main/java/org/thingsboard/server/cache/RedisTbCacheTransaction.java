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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;

import java.io.Serializable;
import java.util.Objects;

/**
 * 中文说明：
 * 1. `RedisTbCacheTransaction` 是 ThingsBoard Common Cache 中管理缓存缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`、`TbCacheTransaction`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Slf4j
@RequiredArgsConstructor
public class RedisTbCacheTransaction<K extends Serializable, V extends Serializable> implements TbCacheTransaction<K, V> {

    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private final RedisTbTransactionalCache<K, V> cache;
    private final RedisConnection connection;

    /**
     * 功能：执行 `putIfAbsent` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void putIfAbsent(K key, V value) {
        cache.put(connection, key, value, RedisStringCommands.SetOption.UPSERT);
    }

    /**
     * 功能：执行 `commit` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean commit() {
        try {
            var execResult = connection.exec();
            var result = execResult != null && execResult.stream().anyMatch(Objects::nonNull);
            return result;
        } finally {
            connection.close();
        }
    }

    /**
     * 功能：执行 `rollback` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void rollback() {
        try {
            connection.discard();
        } finally {
            connection.close();
        }
    }

}
