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
package org.thingsboard.server.cache.ota;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_CACHE;

/**
 * 中文说明：
 * 1. `RedisOtaPackageDataCache` 是 ThingsBoard Common Cache 中管理 OTA 缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `OtaPackageDataCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisOtaPackageDataCache implements OtaPackageDataCache {

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public byte[] get(String key) {
        return get(key, 0, 0);
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `chunkSize`：`chunkSize` 参数。
     * - `chunk`：`chunk` 参数。
     * 返回：处理结果。
     */
    @Override
    public byte[] get(String key, int chunkSize, int chunk) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            if (chunkSize == 0) {
                return connection.get(toOtaPackageCacheKey(key));
            }

            int startIndex = chunkSize * chunk;
            int endIndex = startIndex + chunkSize - 1;
            return connection.getRange(toOtaPackageCacheKey(key), startIndex, endIndex);
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void put(String key, byte[] value) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.set(toOtaPackageCacheKey(key), value);
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    @Override
    public void evict(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.del(toOtaPackageCacheKey(key));
        }
    }

    /**
     * 功能：执行 `toOtaPackageCacheKey` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    private byte[] toOtaPackageCacheKey(String key) {
        return String.format("%s::%s", OTA_PACKAGE_DATA_CACHE, key).getBytes();
    }
}
